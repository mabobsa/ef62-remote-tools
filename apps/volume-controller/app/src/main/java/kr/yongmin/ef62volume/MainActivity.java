package kr.yongmin.ef62volume;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusView;
    private Button permissionButton;
    private Button liveTvTargetButton;
    private SeekBar normalSeekBar;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, 1_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        setContentView(buildContentView());
        normalSeekBar.post(normalSeekBar::requestFocus);
        requestNotificationPermissionIfNeeded();
        VolumeServiceStarter.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private ScrollView buildContentView() {
        int horizontal = dp(54);
        int vertical = dp(30);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(horizontal, vertical, horizontal, vertical);
        root.setBackgroundColor(Color.rgb(16, 24, 32));

        TextView title = new TextView(this);
        title.setText("EF-62 볼륨 자동 설정");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, fullWidth(dp(52)));

        TextView description = new TextView(this);
        description.setText("설정 항목을 선택하고 리모컨 좌·우키로 1%씩 조절하세요.\nWi-Fi 연결 확인 후 자동 볼륨 조절을 시작합니다.");
        description.setTextColor(Color.rgb(185, 211, 226));
        description.setTextSize(18);
        description.setLineSpacing(0, 1.25f);
        root.addView(description, fullWidth(dp(76)));

        SharedPreferences prefs = getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE);
        normalSeekBar = addVolumeControl(
                root,
                "상시 볼륨",
                VolumeMonitorService.KEY_NORMAL_PERCENT,
                prefs.getInt(VolumeMonitorService.KEY_NORMAL_PERCENT, VolumeMonitorService.DEFAULT_NORMAL_PERCENT)
        );
        addVolumeControl(
                root,
                "YouTube 볼륨",
                VolumeMonitorService.KEY_YOUTUBE_PERCENT,
                prefs.getInt(VolumeMonitorService.KEY_YOUTUBE_PERCENT, VolumeMonitorService.DEFAULT_YOUTUBE_PERCENT)
        );

        liveTvTargetButton = makeButton("");
        refreshLiveTvTargetLabel();
        liveTvTargetButton.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                setLiveTvTarget(LiveTvRedirectActivity.TARGET_COUPANG_PLAY);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                setLiveTvTarget(LiveTvRedirectActivity.TARGET_DISNEY_PLUS);
                return true;
            }
            return false;
        });
        liveTvTargetButton.setOnClickListener(view -> {
            String current = getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE)
                    .getString(LiveTvRedirectActivity.KEY_LIVE_TV_TARGET, LiveTvRedirectActivity.DEFAULT_TARGET);
            setLiveTvTarget(LiveTvRedirectActivity.TARGET_COUPANG_PLAY.equals(current)
                    ? LiveTvRedirectActivity.TARGET_DISNEY_PLUS
                    : LiveTvRedirectActivity.TARGET_COUPANG_PLAY);
        });
        root.addView(liveTvTargetButton, fullWidth(dp(60)));

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(17);
        statusView.setLineSpacing(0, 1.3f);
        statusView.setPadding(dp(22), dp(16), dp(22), dp(16));
        statusView.setBackgroundColor(Color.rgb(24, 52, 74));
        root.addView(statusView, fullWidth(dp(180)));

        permissionButton = makeButton("사용 정보 접근 권한 열기");
        permissionButton.setOnClickListener(view -> openUsageAccessSettings());
        root.addView(permissionButton, fullWidth(dp(60)));

        Button startButton = makeButton("자동 조절 시작 / 다시 시작");
        startButton.setOnClickListener(view -> {
            VolumeServiceStarter.start(this);
            Toast.makeText(this, "자동 볼륨 조절을 시작했습니다.", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        root.addView(startButton, fullWidth(dp(60)));

        Button youtubeButton = makeButton("YouTube 열어서 테스트");
        youtubeButton.setOnClickListener(view -> openYouTube());
        root.addView(youtubeButton, fullWidth(dp(60)));

        TextView guide = new TextView(this);
        guide.setText("사용 정보 접근 권한이 없으면 YouTube 여부를 구분할 수 없어 상시 볼륨을 적용합니다.");
        guide.setTextColor(Color.rgb(185, 211, 226));
        guide.setTextSize(15);
        guide.setPadding(0, dp(12), 0, 0);
        root.addView(guide, fullWidth(dp(60)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.addView(root);
        return scroll;
    }

    private SeekBar addVolumeControl(LinearLayout root, String label, String preferenceKey, int initialValue) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(8), dp(22), dp(8));
        card.setBackgroundColor(Color.rgb(24, 52, 74));

        TextView valueView = new TextView(this);
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(18);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setText(label + ": " + initialValue + "%");
        card.addView(valueView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
        ));

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMin(0);
        seekBar.setMax(100);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setProgress(Math.max(0, Math.min(100, initialValue)));
        seekBar.setFocusable(true);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                valueView.setText(label + ": " + progress + "%");
                if (fromUser) {
                    getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE)
                            .edit()
                            .putInt(preferenceKey, progress)
                            .apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        card.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(38)
        ));
        root.addView(card, fullWidth(dp(82)));
        return seekBar;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setFocusable(true);
        return button;
    }

    private void setLiveTvTarget(String target) {
        getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE)
                .edit()
                .putString(LiveTvRedirectActivity.KEY_LIVE_TV_TARGET, target)
                .apply();
        refreshLiveTvTargetLabel();
    }

    private void refreshLiveTvTargetLabel() {
        String target = getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE)
                .getString(LiveTvRedirectActivity.KEY_LIVE_TV_TARGET, LiveTvRedirectActivity.DEFAULT_TARGET);
        String label = LiveTvRedirectActivity.TARGET_COUPANG_PLAY.equals(target)
                ? "쿠팡플레이"
                : "디즈니+";
        liveTvTargetButton.setText("Live TV 실행 앱: " + label + "  (← 쿠팡 / 디즈니 →)");
    }

    private LinearLayout.LayoutParams fullWidth(int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
        params.topMargin = dp(10);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshStatus() {
        SharedPreferences prefs = getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE);
        boolean hasAccess = hasUsageStatsAccess();
        boolean running = prefs.getBoolean(VolumeMonitorService.KEY_SERVICE_RUNNING, false);
        boolean wifiConnected = prefs.getBoolean(VolumeMonitorService.KEY_WIFI_CONNECTED, false);
        String foreground = prefs.getString(VolumeMonitorService.KEY_FOREGROUND_PACKAGE, "");
        int target = prefs.getInt(VolumeMonitorService.KEY_TARGET_PERCENT, 100);
        int actual = prefs.getInt(VolumeMonitorService.KEY_ACTUAL_PERCENT, -1);
        long updatedAt = prefs.getLong(VolumeMonitorService.KEY_LAST_UPDATE, 0L);

        String appLabel;
        if ("com.google.android.youtube.tv".equals(foreground)
                || "com.google.android.youtube".equals(foreground)) {
            appLabel = "YouTube";
        } else if (foreground == null || foreground.isEmpty()) {
            appLabel = "확인 중";
        } else {
            appLabel = foreground;
        }

        String updated = updatedAt == 0L
                ? "확인 중"
                : DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(updatedAt));
        String actualText = actual < 0 ? "기기에서 확인 불가" : actual + "%";

        statusView.setText(
                "서비스: " + (running ? "실행 중" : "시작 중") +
                "\nWi-Fi: " + (wifiConnected ? "연결 확인됨" : "연결 대기 · 볼륨 조절 중지") +
                "\n사용 정보 권한: " + (hasAccess ? "허용됨" : "권한 필요") +
                "\n현재 앱: " + appLabel +
                "\n목표 볼륨: " + (wifiConnected ? target + "%" : "Wi-Fi 연결 대기") +
                "\n현재 볼륨: " + actualText +
                "\n마지막 확인: " + updated
        );
        permissionButton.setText(hasAccess ? "사용 정보 접근 권한: 허용됨" : "사용 정보 접근 권한 열기 (필수)");
    }

    private boolean hasUsageStatsAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(getPackageName(), 0);
            return appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    info.uid,
                    getPackageName()
            ) == AppOpsManager.MODE_ALLOWED;
        } catch (Exception exception) {
            return false;
        }
    }

    private void openUsageAccessSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception firstException) {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception secondException) {
                Toast.makeText(this, "설정 > 앱 > 특별한 앱 액세스 > 사용 정보 접근을 열어 주세요.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openYouTube() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube.tv");
        if (intent == null) {
            intent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
        }
        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "YouTube 앱을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 62);
        }
    }
}
