package kr.yongmin.ef62volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "EF62BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Apply the normal target immediately, even before usage events become available.
        int normalPercent = context.getSharedPreferences(VolumeMonitorService.PREFS, Context.MODE_PRIVATE)
                .getInt(VolumeMonitorService.KEY_NORMAL_PERCENT, VolumeMonitorService.DEFAULT_NORMAL_PERCENT);
        setMediaVolumePercent(context, normalPercent);

        Intent serviceIntent = new Intent(context, VolumeMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to start the volume monitor after boot", exception);
        }
    }

    private static void setMediaVolumePercent(Context context, int percent) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null || audioManager.isVolumeFixed()) {
            return;
        }
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int target = Math.round(max * (percent / 100f));
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
        } catch (SecurityException exception) {
            Log.e(TAG, "No permission to set media volume", exception);
        }
    }
}
