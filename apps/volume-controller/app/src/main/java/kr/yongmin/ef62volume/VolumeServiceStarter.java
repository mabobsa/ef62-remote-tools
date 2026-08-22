package kr.yongmin.ef62volume;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public final class VolumeServiceStarter {
    private static final String TAG = "EF62ServiceStarter";

    private VolumeServiceStarter() {
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, VolumeMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to start volume monitor", exception);
        }
    }
}
