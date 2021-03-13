package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.TimeSettingsDialog;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class PostPreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        // generalCategory.addPreference(getAutoPlayVideosPreference(context));
        screen.addPreference(getAlwaysMuteVideosPreference(context));
        screen.addPreference(getShowCaptionPreference(context));
        screen.addPreference(getPostTimeFormatPreference(context));
    }

    private Preference getAutoPlayVideosPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.AUTOPLAY_VIDEOS);
        preference.setTitle(R.string.post_viewer_autoplay_video);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getAlwaysMuteVideosPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.MUTED_VIDEOS);
        preference.setTitle(R.string.post_viewer_muted_autoplay);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getShowCaptionPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.SHOW_CAPTIONS);
        preference.setDefaultValue(true);
        preference.setTitle(R.string.post_viewer_show_captions);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getPostTimeFormatPreference(@NonNull final Context context) {
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.time_settings);
        preference.setSummary(Utils.datetimeParser.format(new Date()));
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            new TimeSettingsDialog(
                    settingsHelper.getBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED),
                    settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT),
                    settingsHelper.getString(Constants.DATE_TIME_SELECTION),
                    settingsHelper.getBoolean(Constants.SWAP_DATE_TIME_FORMAT_ENABLED),
                    (isCustomFormat,
                     formatSelection,
                     spTimeFormatSelectedItemPosition,
                     spSeparatorSelectedItemPosition,
                     spDateFormatSelectedItemPosition,
                     selectedFormat,
                     currentFormat,
                     swapDateTime) -> {
                        if (isCustomFormat) {
                            settingsHelper.putString(Constants.CUSTOM_DATE_TIME_FORMAT, formatSelection);
                        } else {
                            final String formatSelectionUpdated = spTimeFormatSelectedItemPosition + ";"
                                    + spSeparatorSelectedItemPosition + ';'
                                    + spDateFormatSelectedItemPosition; // time;separator;date
                            settingsHelper.putString(Constants.DATE_TIME_FORMAT, selectedFormat);
                            settingsHelper.putString(Constants.DATE_TIME_SELECTION, formatSelectionUpdated);
                        }
                        settingsHelper.putBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED, isCustomFormat);
                        settingsHelper.putBoolean(Constants.SWAP_DATE_TIME_FORMAT_ENABLED, swapDateTime);
                        Utils.datetimeParser = (SimpleDateFormat) currentFormat.clone();
                        preference.setSummary(Utils.datetimeParser.format(new Date()));
                    }
            ).show(getParentFragmentManager(), null);
            return true;
        });
        return preference;
    }
}
