package org3.sport.timemarker.v1;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.View;

import java.util.TimeZone;

/**
 * @author ikh
 * @since 2/16/14
 */
public class AboutActivity extends PreferenceActivity {

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_container);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_about);
        PackageManager manager = this.getPackageManager();
        assert manager != null;
        PackageInfo info;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }

        buildPreference(info);
        versionPreference(info);
        releaseDatePreference();
    }

    private void releaseDatePreference() {
        Preference releasePreference = findPreference("releaseDatePreference");
        assert releasePreference != null;
        releasePreference.setSummary(BuildConfig.ORG3_SPORT_TIME_MARKER_BUILD_DATE);
    }

    private void versionPreference(PackageInfo info) {
        Preference versionPreference = findPreference("versionPreference");
        assert versionPreference != null;
        versionPreference.setSummary(info.versionName + (BuildConfig.DEBUG ? "-debug" : ""));
    }

    private void buildPreference(PackageInfo info) {
        Preference buildPreference = findPreference("buildPreference");
        assert buildPreference != null;
        buildPreference.setSummary("" + info.versionCode);
    }

    public void onClickBack(View view) {
        finish();
    }
}
