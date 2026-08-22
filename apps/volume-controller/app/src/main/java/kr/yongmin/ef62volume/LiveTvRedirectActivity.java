package kr.yongmin.ef62volume;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Redirects the EF-62 remote's Live TV deep link to Coupang Play. */
public final class LiveTvRedirectActivity extends Activity {
    private static final String COUPANG_PLAY_PACKAGE = "com.coupang.mobile.play";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent launchIntent = getPackageManager()
                .getLeanbackLaunchIntentForPackage(COUPANG_PLAY_PACKAGE);
        if (launchIntent == null) {
            launchIntent = getPackageManager().getLaunchIntentForPackage(COUPANG_PLAY_PACKAGE);
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "쿠팡플레이 앱을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
