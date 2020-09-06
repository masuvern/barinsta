package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.TimeSettingsDialog;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DirectoryChooser;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class SettingsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = "SettingsPrefsFrag";
    private static AppCompatTextView customPathTextView;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        screen.addPreference(getLanguagePreference());
        screen.addPreference(getDefaultTabPreference());
        screen.addPreference(getThemePreference());
        screen.addPreference(getAmoledThemePreference());
        screen.addPreference(getDownloadUserFolderPreference());
        screen.addPreference(getSaveToCustomFolderPreference());
        screen.addPreference(getAutoPlayVideosPreference());
        screen.addPreference(getAlwaysMuteVideosPreference());
        screen.addPreference(getPostTimePreference());

        final PreferenceCategory loggedInUsersPreferenceCategory = new PreferenceCategory(requireContext());
        loggedInUsersPreferenceCategory.setIconSpaceReserved(false);
        screen.addPreference(loggedInUsersPreferenceCategory);
        loggedInUsersPreferenceCategory.setTitle(R.string.login_settings);
        loggedInUsersPreferenceCategory.addPreference(getMarkStoriesSeenPreference());
        loggedInUsersPreferenceCategory.addPreference(getEnableActivityNotificationsPreference());

        final PreferenceCategory anonUsersPreferenceCategory = new PreferenceCategory(requireContext());
        anonUsersPreferenceCategory.setIconSpaceReserved(false);
        screen.addPreference(anonUsersPreferenceCategory);
        anonUsersPreferenceCategory.setTitle(R.string.anonymous_settings);
        anonUsersPreferenceCategory.addPreference(getUseInstaDpPreference());
        anonUsersPreferenceCategory.addPreference(getUseStoriesIgPreference());

    }

    @NonNull
    private DropDownPreference getLanguagePreference() {
        final DropDownPreference preference = new DropDownPreference(requireContext());
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final int length = getResources().getStringArray(R.array.languages).length;
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(Constants.APP_LANGUAGE);
        preference.setTitle(R.string.select_language);
        preference.setEntries(R.array.languages);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            return true;
        });
        return preference;
    }

    private Preference getDefaultTabPreference() {
        final DropDownPreference preference = new DropDownPreference(requireContext());
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return preference;
        }
        final TypedArray mainNavIds = getResources().obtainTypedArray(R.array.main_nav_ids);
        final int length = mainNavIds.length();
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            final int resourceId = mainNavIds.getResourceId(i, -1);
            if (resourceId < 0) continue;
            values[i] = String.valueOf(resourceId);
        }
        mainNavIds.recycle();
        preference.setKey(Constants.DEFAULT_TAB);
        preference.setTitle(R.string.select_default_tab);
        preference.setEntries(R.array.main_nav_ids_values);
        preference.setEntryValues(values);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            return true;
        });
        return preference;
    }

    @NonNull
    private DropDownPreference getThemePreference() {
        final DropDownPreference preference = new DropDownPreference(requireContext());
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final int length = getResources().getStringArray(R.array.theme_presets).length;
        final String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(Constants.APP_THEME);
        preference.setTitle(R.string.theme_settings);
        preference.setEntries(R.array.theme_presets);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            shouldRecreate();
            return true;
        });
        return preference;
    }

    private SwitchPreferenceCompat getAmoledThemePreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.AMOLED_THEME);
        preference.setTitle(R.string.use_amoled_dark_theme);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            final boolean isNight = Utils.isNight(requireContext(), settingsHelper.getThemeCode(true));
            if (isNight) shouldRecreate();
            return true;
        });
        return preference;
    }

    private Preference getDownloadUserFolderPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.DOWNLOAD_USER_FOLDER);
        preference.setTitle("Download to username folder");
        preference.setSummary(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getSaveToCustomFolderPreference() {
        return new SaveToCustomFolderPreference(requireContext(), (resultCallback) -> {
            new DirectoryChooser()
                    .setInitialDirectory(settingsHelper.getString(FOLDER_PATH))
                    .setInteractionListener(path -> {
                        settingsHelper.putString(FOLDER_PATH, path);
                        resultCallback.onResult(path);
                    })
                    .show(getParentFragmentManager(), null);
        });
    }

    private Preference getAutoPlayVideosPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.AUTOPLAY_VIDEOS);
        preference.setTitle(R.string.post_viewer_autoplay_video);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getAlwaysMuteVideosPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.MUTED_VIDEOS);
        preference.setTitle(R.string.post_viewer_muted_autoplay);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getMarkStoriesSeenPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.MARK_AS_SEEN);
        preference.setTitle(R.string.mark_as_seen_setting);
        preference.setSummary(R.string.mark_as_seen_setting_summary);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getEnableActivityNotificationsPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.CHECK_ACTIVITY);
        preference.setTitle(R.string.activity_setting);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getUseInstaDpPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.INSTADP);
        preference.setTitle(R.string.instadp_settings);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getUseStoriesIgPreference() {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setKey(Constants.STORIESIG);
        preference.setTitle(R.string.storiesig_settings);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getPostTimePreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle(R.string.time_settings);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            new TimeSettingsDialog(
                    settingsHelper.getBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED),
                    settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT),
                    settingsHelper.getString(Constants.DATE_TIME_SELECTION),
                    (isCustomFormat,
                     formatSelection,
                     spTimeFormatSelectedItemPosition,
                     spSeparatorSelectedItemPosition,
                     spDateFormatSelectedItemPosition,
                     selectedFormat, currentFormat) -> {
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
                        Utils.datetimeParser = (SimpleDateFormat) currentFormat.clone();
                    }
            ).show(getParentFragmentManager(), null);
            return true;
        });
        return preference;
    }

    public static class SaveToCustomFolderPreference extends Preference {

        private final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener;
        private String key;

        public SaveToCustomFolderPreference(final Context context, final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener) {
            super(context);
            this.onSelectFolderButtonClickListener = onSelectFolderButtonClickListener;
            key = Constants.FOLDER_SAVE_TO;
            setLayoutResource(R.layout.pref_custom_folder);
            setKey(key);
            setTitle(R.string.save_to_folder);
            setIconSpaceReserved(false);
        }

        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            final SwitchMaterial cbSaveTo = (SwitchMaterial) holder.findViewById(R.id.cbSaveTo);
            final View buttonContainer = holder.findViewById(R.id.button_container);
            customPathTextView = (AppCompatTextView) holder.findViewById(R.id.custom_path);
            cbSaveTo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsHelper.putBoolean(FOLDER_SAVE_TO, isChecked);
                buttonContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                final String customPath = settingsHelper.getString(FOLDER_PATH);
                customPathTextView.setText(customPath);
            });
            final boolean savedToEnabled = settingsHelper.getBoolean(key);
            holder.itemView.setOnClickListener(v -> cbSaveTo.toggle());
            cbSaveTo.setChecked(savedToEnabled);
            buttonContainer.setVisibility(savedToEnabled ? View.VISIBLE : View.GONE);
            final AppCompatButton btnSaveTo = (AppCompatButton) holder.findViewById(R.id.btnSaveTo);
            btnSaveTo.setOnClickListener(v -> {
                if (onSelectFolderButtonClickListener == null) return;
                onSelectFolderButtonClickListener.onClick(result -> {
                    if (Utils.isEmpty(result)) return;
                    customPathTextView.setText(result);
                });
            });
        }

        public interface ResultCallback {
            void onResult(String result);
        }

        public interface OnSelectFolderButtonClickListener {
            void onClick(ResultCallback resultCallback);
        }
    }
}
