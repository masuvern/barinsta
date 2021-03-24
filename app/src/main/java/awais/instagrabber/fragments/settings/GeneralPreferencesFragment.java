package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.TabOrderPreferenceDialogFragment;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class GeneralPreferencesFragment extends BasePreferencesFragment implements TabOrderPreferenceDialogFragment.Callback {

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (isLoggedIn) {
            screen.addPreference(getDefaultTabPreference(context));
            screen.addPreference(getTabOrderPreference(context));
        }
        screen.addPreference(getUpdateCheckPreference(context));
        screen.addPreference(getFlagSecurePreference(context));
    }

    private Preference getDefaultTabPreference(@NonNull final Context context) {
        final ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        final TypedArray mainNavGraphs = getResources().obtainTypedArray(R.array.main_nav_graphs);
        final int length = mainNavGraphs.length();
        final String[] navGraphFileNames = new String[length];
        for (int i = 0; i < length; i++) {
            final int resourceId = mainNavGraphs.getResourceId(i, -1);
            if (resourceId < 0) continue;
            navGraphFileNames[i] = getResources().getResourceEntryName(resourceId);
        }
        mainNavGraphs.recycle();
        preference.setKey(Constants.DEFAULT_TAB);
        preference.setTitle(R.string.pref_start_screen);
        preference.setDialogTitle(R.string.pref_start_screen);
        preference.setEntries(R.array.main_nav_titles);
        preference.setEntryValues(navGraphFileNames);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    @NonNull
    private Preference getTabOrderPreference(@NonNull final Context context) {
        final Preference preference = new Preference(context);
        preference.setTitle(R.string.tab_order);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            final TabOrderPreferenceDialogFragment dialogFragment = TabOrderPreferenceDialogFragment.newInstance();
            dialogFragment.show(getChildFragmentManager(), "tab_order_dialog");
            return true;
        });
        return preference;
    }

    private Preference getUpdateCheckPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.CHECK_UPDATES);
        preference.setTitle(R.string.update_check);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getFlagSecurePreference(@NonNull final Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                Constants.FLAG_SECURE,
                R.string.flag_secure,
                -1,
                false,
                (preference, newValue) -> {
                    shouldRecreate();
                    return true;
                });
    }

    @Override
    public void onSave(final boolean orderHasChanged) {
        Log.d("", "onSave: " + orderHasChanged);
    }

    @Override
    public void onCancel() {

    }
}
