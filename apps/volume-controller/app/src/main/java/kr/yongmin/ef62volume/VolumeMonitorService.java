package kr.yongmin.ef62volume;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.Set;

public final class VolumeMonitorService extends Service {
    public static final String PREFS = "monitor_state";
    public static final String KEY_FOREGROUND_PACKAGE = "foreground_package";
    public static final String KEY_TARGET_PERCENT = "target_percent";
    public static final String KEY_ACTUAL_PERCENT = "actual_percent";
    public static final String KEY_LAST_UPDATE = "last_update";
    public static final String KEY_USAGE_ACCESS = "usage_access";
    public static final String KEY_SERVICE_RUNNING = "service_running";
    public static final String KEY_WIFI_CONNECTED = "wifi_connected";
    public static final String KEY_NORMAL_PERCENT = "normal_percent";
    public static final String KEY_YOUTUBE_PERCENT = "youtube_percent";
    public static final int DEFAULT_NORMAL_PERCENT = 100;
    public static final int DEFAULT_YOUTUBE_PERCENT = 90;

    private static final String TAG = "EF62VolumeService";
    private static final String CHANNEL_ID = "ef62_volume_monitor";
    private static final int NOTIFICATION_ID = 6200;
    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final long INITIAL_LOOKBACK_MS = 12L * 60L * 60L * 1_000L;
    private static final Set<String> YOUTUBE_PACKAGES = Set.of(
            "com.google.android.youtube.tv",
            "com.google.android.youtube"
    );

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollAndApply();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private UsageStatsManager usageStatsManager;
    private AudioManager audioManager;
    private ConnectivityManager connectivityManager;
    private SharedPreferences preferences;
    private String foregroundPackage = "";
    private int lastTargetPercent = -1;
    private int lastActualPercent = -1;
    private boolean lastUsageAccess;
    private boolean lastYouTubeActive;
    private boolean lastWifiConnected;
    private long usageQueryStart;

    @Override
    public void onCreate() {
        super.onCreate();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        createNotificationChannel();
        int normalPercent = readPercent(KEY_NORMAL_PERCENT, DEFAULT_NORMAL_PERCENT);
        boolean wifiConnected = isValidatedWifiConnected();
        startForeground(NOTIFICATION_ID, buildNotification(normalPercent, false, wifiConnected));

        long now = System.currentTimeMillis();
        usageQueryStart = now - INITIAL_LOOKBACK_MS;
        preferences.edit()
                .putBoolean(KEY_SERVICE_RUNNING, true)
                .putBoolean(KEY_WIFI_CONNECTED, wifiConnected)
                .apply();
        handler.post(pollRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        preferences.edit().putBoolean(KEY_SERVICE_RUNNING, false).apply();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void pollAndApply() {
        boolean usageAccess = hasUsageStatsAccess();
        long now = System.currentTimeMillis();

        if (usageAccess) {
            updateForegroundPackage(usageQueryStart, now);
        }
        usageQueryStart = Math.max(0L, now - 2_000L);

        boolean youtubeActive = usageAccess && YOUTUBE_PACKAGES.contains(foregroundPackage);
        int normalPercent = readPercent(KEY_NORMAL_PERCENT, DEFAULT_NORMAL_PERCENT);
        int youtubePercent = readPercent(KEY_YOUTUBE_PERCENT, DEFAULT_YOUTUBE_PERCENT);
        int targetPercent = youtubeActive ? youtubePercent : normalPercent;
        boolean wifiConnected = isValidatedWifiConnected();
        int actualPercent = wifiConnected
                ? applyAndReadVolume(targetPercent)
                : readCurrentVolumePercent();

        if (targetPercent != lastTargetPercent
                || actualPercent != lastActualPercent
                || usageAccess != lastUsageAccess
                || youtubeActive != lastYouTubeActive
                || wifiConnected != lastWifiConnected) {
            lastTargetPercent = targetPercent;
            lastActualPercent = actualPercent;
            lastUsageAccess = usageAccess;
            lastYouTubeActive = youtubeActive;
            lastWifiConnected = wifiConnected;

            preferences.edit()
                    .putString(KEY_FOREGROUND_PACKAGE, foregroundPackage)
                    .putInt(KEY_TARGET_PERCENT, targetPercent)
                    .putInt(KEY_ACTUAL_PERCENT, actualPercent)
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .putBoolean(KEY_USAGE_ACCESS, usageAccess)
                    .putBoolean(KEY_SERVICE_RUNNING, true)
                    .putBoolean(KEY_WIFI_CONNECTED, wifiConnected)
                    .apply();

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(targetPercent, youtubeActive, wifiConnected)
                );
            }
        }
    }

    private int readPercent(String key, int defaultValue) {
        return Math.max(0, Math.min(100, preferences.getInt(key, defaultValue)));
    }

    private void updateForegroundPackage(long begin, long end) {
        if (usageStatsManager == null) {
            return;
        }
        UsageEvents events = usageStatsManager.queryEvents(begin, end);
        if (events == null) {
            return;
        }

        UsageEvents.Event event = new UsageEvents.Event();
        long newestTimestamp = Long.MIN_VALUE;
        String newestPackage = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            boolean movedToForeground = type == UsageEvents.Event.MOVE_TO_FOREGROUND;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                movedToForeground = movedToForeground || type == UsageEvents.Event.ACTIVITY_RESUMED;
            }
            if (movedToForeground && event.getTimeStamp() >= newestTimestamp) {
                newestTimestamp = event.getTimeStamp();
                newestPackage = event.getPackageName();
            }
        }
        if (newestPackage != null) {
            foregroundPackage = newestPackage;
        }
    }

    private int applyAndReadVolume(int percent) {
        if (audioManager == null || audioManager.isVolumeFixed()) {
            return -1;
        }
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max <= 0) {
            return -1;
        }

        int target = Math.round(max * (percent / 100f));
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (current != target) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            } catch (SecurityException exception) {
                Log.e(TAG, "No permission to set media volume", exception);
            }
        }
        return readCurrentVolumePercent();
    }

    private int readCurrentVolumePercent() {
        if (audioManager == null || audioManager.isVolumeFixed()) {
            return -1;
        }
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max <= 0) {
            return -1;
        }
        int actual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return Math.round(actual * 100f / max);
    }

    private boolean isValidatedWifiConnected() {
        if (connectivityManager == null) {
            return false;
        }
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to read Wi-Fi connectivity", exception);
            return false;
        }
    }

    private boolean hasUsageStatsAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(getPackageName(), 0);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, info.uid, getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception exception) {
            return false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(
            int targetPercent,
            boolean youtubeActive,
            boolean wifiConnected
    ) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String text;
        if (!wifiConnected) {
            text = "Wi-Fi 연결 확인 대기 중 · 볼륨 조절 일시 중지";
        } else if (youtubeActive) {
            text = "YouTube 실행 중 · 볼륨 " + targetPercent + "%";
        } else {
            text = "자동 조절 중 · 볼륨 " + targetPercent + "%";
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .build();
    }
}
