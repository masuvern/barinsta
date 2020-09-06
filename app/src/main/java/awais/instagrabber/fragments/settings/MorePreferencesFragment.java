package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.activities.Login;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.FlavorTown;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class MorePreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = "MorePreferencesFragment";

    private final String cookie = settingsHelper.getString(Constants.COOKIE);

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        screen.addPreference(new MoreHeaderPreference(requireContext()));

        final PreferenceCategory accountCategory = new PreferenceCategory(requireContext());
        accountCategory.setTitle("Account");
        accountCategory.setIconSpaceReserved(false);
        screen.addPreference(accountCategory);
        final boolean isLoggedIn = !Utils.isEmpty(cookie) && Utils.getUserIdFromCookie(cookie) != null;
        screen.addPreference(getPreference(
                isLoggedIn ? R.string.relogin : R.string.login,
                isLoggedIn ? R.string.relogin_summary : -1,
                -1,
                preference -> {
                    startActivityForResult(new Intent(requireContext(), Login.class), Constants.LOGIN_RESULT_CODE);
                    return true;
                }));
        if (isLoggedIn) {
            screen.addPreference(getPreference(R.string.logout, -1, preference -> {
                Utils.setupCookies("LOGOUT");
                shouldRecreate();
                Toast.makeText(requireContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
                settingsHelper.putString(Constants.COOKIE, "");
                return true;
            }));
        }

        final PreferenceCategory generalCategory = new PreferenceCategory(requireContext());
        generalCategory.setTitle("General");
        generalCategory.setIconSpaceReserved(false);
        screen.addPreference(generalCategory);
        generalCategory.addPreference(getPreference(R.string.action_notif, R.drawable.ic_not_liked, preference -> false));
        generalCategory.addPreference(getPreference(R.string.action_settings, R.drawable.ic_outline_settings_24, preference -> {
            final NavDirections navDirections = MorePreferencesFragmentDirections.actionMorePreferencesFragmentToSettingsPreferencesFragment();
            NavHostFragment.findNavController(this).navigate(navDirections);
            return true;
        }));
        final Preference aboutPreference = getPreference(R.string.action_about, R.drawable.ic_outline_info_24, preference -> false);
        generalCategory.addPreference(aboutPreference);

        final Preference divider = new Preference(requireContext());
        divider.setLayoutResource(R.layout.item_pref_divider);
        screen.addPreference(divider);

        final Preference versionPreference = getPreference(R.string.version, BuildConfig.VERSION_NAME, -1, preference -> {
            FlavorTown.updateCheck((AppCompatActivity) requireActivity(), true);
            return true;
        });
        screen.addPreference(versionPreference);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (resultCode == Constants.LOGIN_RESULT_CODE) {
            if (data == null) return;
            final String cookie = data.getStringExtra("cookie");
            Utils.setupCookies(cookie);
            shouldRecreate();
            Toast.makeText(requireContext(), R.string.login_success_loading_cookies, Toast.LENGTH_SHORT).show();
            settingsHelper.putString(Constants.COOKIE, cookie);
        }
    }

    @NonNull
    private Preference getPreference(final int title,
                                     final int icon,
                                     final Preference.OnPreferenceClickListener clickListener) {
        return getPreference(title, -1, icon, clickListener);
    }

    @NonNull
    private Preference getPreference(final int title,
                                     final int summary,
                                     final int icon,
                                     final Preference.OnPreferenceClickListener clickListener) {
        String string = null;
        if (summary > 0) {
            try {
                string = getString(summary);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Error", e);
            }
        }
        return getPreference(title, string, icon, clickListener);
    }

    @NonNull
    private Preference getPreference(final int title,
                                     final String summary,
                                     final int icon,
                                     final Preference.OnPreferenceClickListener clickListener) {
        final Preference preference = new Preference(requireContext());
        if (icon <= 0) preference.setIconSpaceReserved(false);
        if (icon > 0) preference.setIcon(icon);
        preference.setTitle(title);
        if (!Utils.isEmpty(summary)) {
            preference.setSummary(summary);
        }
        preference.setOnPreferenceClickListener(clickListener);
        return preference;
    }

    public static class MoreHeaderPreference extends Preference {

        public MoreHeaderPreference(final Context context) {
            super(context);
            setLayoutResource(R.layout.pref_more_header);
            setSelectable(false);
        }
    }
}
