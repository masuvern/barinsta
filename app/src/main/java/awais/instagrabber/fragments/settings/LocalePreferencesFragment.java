package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.UserAgentUtils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class LocalePreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        screen.addPreference(getLanguagePreference(context));
    }

    private Preference getLanguagePreference(@NonNull final Context context) {
        final ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final int length = getResources().getStringArray(R.array.languages).length;
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(Constants.APP_LANGUAGE);
        preference.setTitle(R.string.select_language);
        preference.setDialogTitle(R.string.select_language);
        preference.setEntries(R.array.languages);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            final int appUaCode = settingsHelper.getInteger(Constants.APP_UA_CODE);
            final String appUa = UserAgentUtils.generateAppUA(appUaCode, LocaleUtils.getCurrentLocale().getLanguage());
            settingsHelper.putString(Constants.APP_UA, appUa);
            return true;
        });
        return preference;
    }
}
