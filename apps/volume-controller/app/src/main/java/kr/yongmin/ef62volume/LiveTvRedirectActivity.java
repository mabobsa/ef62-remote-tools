package kr.yongmin.ef62volume;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Redirects the EF-62 remote's Live TV deep link to the configured streaming app. */
public final class LiveTvRedirectActivity extends Activity {
    public static final String KEY_LIVE_TV_TARGET = "live_tv_target";
    public static final String TARGET_COUPANG_PLAY = "coupang_play";
    public static final String TARGET_DISNEY_PLUS = "disney_plus";
    public static final String DEFAULT_TARGET = TARGET_DISNEY_PLUS;

    private static final String COUPANG_PLAY_PACKAGE = "com.coupang.mobile.play";
    private static final String DISNEY_PLUS_PACKAGE = "com.disney.disneyplus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String target = getSharedPreferences(VolumeMonitorService.PREFS, MODE_PRIVATE)
                .getString(KEY_LIVE_TV_TARGET, DEFAULT_TARGET);
        boolean coupangSelected = TARGET_COUPANG_PLAY.equals(target);
        String targetPackage = coupangSelected ? COUPANG_PLAY_PACKAGE : DISNEY_PLUS_PACKAGE;
        String targetLabel = coupangSelected ? "쿠팡플레이" : "디즈니+";

        Intent launchIntent = getPackageManager()
                .getLeanbackLaunchIntentForPackage(targetPackage);
        if (launchIntent == null) {
            launchIntent = getPackageManager().getLaunchIntentForPackage(targetPackage);
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, targetLabel + " 앱을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
