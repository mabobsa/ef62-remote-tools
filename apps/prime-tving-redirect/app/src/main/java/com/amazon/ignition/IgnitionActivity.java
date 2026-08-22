package com.amazon.ignition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/** Receives the EF-62 Prime Video button intent and opens TVING. */
public final class IgnitionActivity extends Activity {
    private static final String TAG = "PrimeTvingRedirect";
    private static final String TVING_PACKAGE = "net.cj.em.tving";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Prime Video button intent received: " + getIntent());

        Intent launchIntent = getPackageManager()
                .getLeanbackLaunchIntentForPackage(TVING_PACKAGE);
        if (launchIntent == null) {
            launchIntent = getPackageManager().getLaunchIntentForPackage(TVING_PACKAGE);
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
            Log.i(TAG, "TVING launch requested");
        } else {
            Log.e(TAG, "TVING package is not installed");
            Toast.makeText(this, "TVING 앱을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
