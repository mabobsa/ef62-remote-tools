package kr.yongmin.ef62volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "EF62BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // The service waits for a validated Wi-Fi connection before changing volume.
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
}
