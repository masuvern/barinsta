package awais.instagrabber.fragments.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import java.util.Collections;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.activities.Login;
import awais.instagrabber.adapters.AccountSwitcherListAdapter;
import awais.instagrabber.adapters.AccountSwitcherListAdapter.OnAccountClickListener;
import awais.instagrabber.databinding.PrefAccountSwitcherBinding;
import awais.instagrabber.repositories.responses.UserInfo;
import awais.instagrabber.webservices.ProfileService;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.FlavorTown;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.adapters.AccountSwitcherListAdapter.OnAccountLongClickListener;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class MorePreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = "MorePreferencesFragment";
    private AlertDialog accountSwitchDialog;
    private DataBox.CookieModel tappedModel;
    private ArrayAdapter<DataBox.CookieModel> adapter;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != null;
        // screen.addPreference(new MoreHeaderPreference(requireContext()));

        final PreferenceCategory accountCategory = new PreferenceCategory(requireContext());
        accountCategory.setTitle(R.string.account);
        accountCategory.setIconSpaceReserved(false);
        screen.addPreference(accountCategory);
        // To re-login, user can just add the same account back from account switcher dialog
        // accountCategory.addPreference(getPreference(
        //         isLoggedIn ? R.string.relogin : R.string.login,
        //         isLoggedIn ? R.string.relogin_summary : -1,
        //         -1,
        //         preference -> {
        //             startActivityForResult(new Intent(requireContext(), Login.class), Constants.LOGIN_RESULT_CODE);
        //             return true;
        //         }));
        if (isLoggedIn) {
            accountCategory.setSummary(R.string.account_hint);
            accountCategory.addPreference(getAccountSwitcherPreference(cookie));
            accountCategory.addPreference(getPreference(R.string.logout, R.string.logout_summary, R.drawable.ic_logout, preference -> {
                if (getContext() == null) return false;
                CookieUtils.setupCookies("LOGOUT");
                shouldRecreate();
                Toast.makeText(requireContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
                settingsHelper.putString(Constants.COOKIE, "");
                return true;
            }));
        } else {
            if (Utils.dataBox.getAllCookies().size() > 0) {
                accountCategory.addPreference(getAccountSwitcherPreference(null));
            }
            // Need to show something to trigger login activity
            accountCategory.addPreference(getPreference(R.string.add_account, R.drawable.ic_add, preference -> {
                startActivityForResult(new Intent(getContext(), Login.class), Constants.LOGIN_RESULT_CODE);
                return true;
            }));
        }

        if (Utils.dataBox.getAllCookies().size() > 0) {
            accountCategory.addPreference(getPreference(R.string.remove_all_acc, null, R.drawable.ic_delete, preference -> {
                if (getContext() == null) return false;
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.logout)
                        .setMessage(R.string.remove_all_acc_warning)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            CookieUtils.setupCookies("REMOVE");
                            shouldRecreate();
                            Toast.makeText(requireContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
                            settingsHelper.putString(Constants.COOKIE, "");
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            }));
        }

        final PreferenceCategory generalCategory = new PreferenceCategory(requireContext());
        generalCategory.setTitle(R.string.pref_category_general);
        generalCategory.setIconSpaceReserved(false);
        screen.addPreference(generalCategory);
        if (isLoggedIn) {
            generalCategory.addPreference(getPreference(R.string.action_notif, R.drawable.ic_not_liked, preference -> {
                final NavDirections navDirections = MorePreferencesFragmentDirections.actionMorePreferencesFragmentToNotificationsViewer();
                NavHostFragment.findNavController(this).navigate(navDirections);
                return true;
            }));
        }
        generalCategory.addPreference(getPreference(R.string.action_settings, R.drawable.ic_outline_settings_24, preference -> {
            final NavDirections navDirections = MorePreferencesFragmentDirections.actionMorePreferencesFragmentToSettingsPreferencesFragment();
            NavHostFragment.findNavController(this).navigate(navDirections);
            return true;
        }));
        final Preference aboutPreference = getPreference(R.string.action_about, R.drawable.ic_outline_info_24, preference -> {
            final NavDirections navDirections = MorePreferencesFragmentDirections.actionMorePreferencesFragmentToAboutFragment();
            NavHostFragment.findNavController(this).navigate(navDirections);
            return true;
        });
        generalCategory.addPreference(aboutPreference);

        final Preference divider = new Preference(requireContext());
        divider.setLayoutResource(R.layout.item_pref_divider);
        screen.addPreference(divider);

        final Preference versionPreference = getPreference(R.string.version,
                BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", -1, preference -> {
            FlavorTown.updateCheck((AppCompatActivity) requireActivity(), true);
            return true;
        });
        screen.addPreference(versionPreference);

        final Preference reminderPreference = getPreference(R.string.reminder, R.string.reminder_summary, R.drawable.ic_warning, null);
        reminderPreference.setSelectable(false);
        screen.addPreference(reminderPreference);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (resultCode == Constants.LOGIN_RESULT_CODE) {
            if (data == null) return;
            final String cookie = data.getStringExtra("cookie");
            CookieUtils.setupCookies(cookie);
            settingsHelper.putString(Constants.COOKIE, cookie);
            // No use as the timing of show is unreliable
            // Toast.makeText(requireContext(), R.string.login_success_loading_cookies, Toast.LENGTH_SHORT).show();

            // adds cookies to database for quick access
            final String uid = CookieUtils.getUserIdFromCookie(cookie);
            final ProfileService profileService = ProfileService.getInstance();
            profileService.getUserInfo(uid, new ServiceCallback<UserInfo>() {
                @Override
                public void onSuccess(final UserInfo result) {
                    // Log.d(TAG, "adding userInfo: " + result);
                    if (result != null) {
                        Utils.dataBox.addOrUpdateUser(uid, result.getUsername(), cookie, result.getFullName(), result.getProfilePicUrl());
                    }
                    final FragmentActivity activity = getActivity();
                    if (activity == null) return;
                    activity.recreate();
                }

                @Override
                public void onFailure(final Throwable t) {
                    Log.e(TAG, "Error fetching user info", t);
                }
            });
        }
    }

    @NonNull
    private AccountSwitcherPreference getAccountSwitcherPreference(final String cookie) {
        final List<DataBox.CookieModel> allUsers = Utils.dataBox.getAllCookies();
        if (getContext() != null && allUsers != null) {
            sortUserList(cookie, allUsers);
            final OnAccountClickListener clickListener = (model, isCurrent) -> {
                if (isCurrent) {
                    if (accountSwitchDialog == null) return;
                    accountSwitchDialog.dismiss();
                    return;
                }
                tappedModel = model;
                shouldRecreate();
                if (accountSwitchDialog == null) return;
                accountSwitchDialog.dismiss();
            };
            final OnAccountLongClickListener longClickListener = (model, isCurrent) -> {
                if (isCurrent) {
                    new AlertDialog.Builder(getContext())
                            .setMessage(R.string.quick_access_cannot_delete_curr)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    return true;
                }
                new AlertDialog.Builder(getContext())
                        .setMessage(getString(R.string.quick_access_confirm_delete, model.getUsername()))
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            Utils.dataBox.delUserCookie(model);
                            adapter.clear();
                            final List<DataBox.CookieModel> users = Utils.dataBox.getAllCookies();
                            if (users == null) return;
                            adapter.addAll(users);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                accountSwitchDialog.dismiss();
                return true;
            };
            adapter = new AccountSwitcherListAdapter(
                    getContext(),
                    R.layout.pref_account_switcher,
                    allUsers,
                    clickListener,
                    longClickListener
            );
            accountSwitchDialog = new AlertDialog.Builder(getContext())
                    .setTitle("Accounts")
                    .setNeutralButton("Add account", (dialog1, which) -> startActivityForResult(
                            new Intent(getContext(), Login.class),
                            Constants.LOGIN_RESULT_CODE))
                    .setAdapter(adapter, null)
                    .create();
            accountSwitchDialog.setOnDismissListener(dialog -> {
                if (tappedModel == null) return;
                CookieUtils.setupCookies(tappedModel.getCookie());
                settingsHelper.putString(Constants.COOKIE, tappedModel.getCookie());
            });
        }
        final AlertDialog finalDialog = accountSwitchDialog;
        return new AccountSwitcherPreference(requireContext(), cookie, v -> {
            if (finalDialog == null) return;
            finalDialog.show();
        });
    }

    /**
     * Sort the user list by following logic:
     * <ol>
     * <li>Keep currently active account at top.
     * <li>Check if any user does not have a full name.
     * <li>If all have full names, sort by full names.
     * <li>Otherwise, sort by the usernames
     * </ol>
     *
     * @param cookie   active cookie
     * @param allUsers list of users
     */
    private void sortUserList(final String cookie, final List<DataBox.CookieModel> allUsers) {
        boolean sortByName = true;
        for (final DataBox.CookieModel user : allUsers) {
            if (TextUtils.isEmpty(user.getFullName())) {
                sortByName = false;
                break;
            }
        }
        final boolean finalSortByName = sortByName;
        Collections.sort(allUsers, (o1, o2) -> {
            // keep current account at top
            if (o1.getCookie().equals(cookie)) return -1;
            if (finalSortByName) {
                // sort by full name
                return o1.getFullName().compareTo(o2.getFullName());
            }
            // otherwise sort by username
            return o1.getUsername().compareTo(o2.getUsername());
        });
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
        if (!TextUtils.isEmpty(summary)) {
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

    public static class AccountSwitcherPreference extends Preference {

        private final String cookie;
        private final View.OnClickListener onClickListener;

        public AccountSwitcherPreference(final Context context,
                                         final String cookie,
                                         final View.OnClickListener onClickListener) {
            super(context);
            this.cookie = cookie;
            this.onClickListener = onClickListener;
            setLayoutResource(R.layout.pref_account_switcher);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            final View root = holder.itemView;
            if (onClickListener != null) root.setOnClickListener(onClickListener);
            final PrefAccountSwitcherBinding binding = PrefAccountSwitcherBinding.bind(root);
            final String uid = CookieUtils.getUserIdFromCookie(cookie);
            if (uid == null) return;
            final DataBox.CookieModel user = Utils.dataBox.getCookie(uid);
            if (user == null) return;
            binding.fullName.setText(user.getFullName());
            binding.username.setText("@" + user.getUsername());
            binding.profilePic.setImageURI(user.getProfilePic());
        }
    }
}
