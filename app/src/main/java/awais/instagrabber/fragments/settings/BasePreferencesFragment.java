package awais.instagrabber.fragments.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;

public abstract class BasePreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean shouldRecreate = false;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        final PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName("settings");
        preferenceManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(requireContext());
        setupPreferenceScreen(screen);
        setPreferenceScreen(screen);
    }

    abstract void setupPreferenceScreen(PreferenceScreen screen);

    protected void shouldRecreate() {
        this.shouldRecreate = true;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (!shouldRecreate) return;
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        if (key.equals(Constants.APP_LANGUAGE)) {
            LocaleUtils.setLocale(activity.getBaseContext());
        }
        shouldRecreate = false;
        activity.recreate();
    }
}
