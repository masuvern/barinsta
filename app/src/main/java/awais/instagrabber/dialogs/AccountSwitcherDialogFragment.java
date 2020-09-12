package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.AccountSwitcherAdapter;
import awais.instagrabber.databinding.DialogAccountSwitcherBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AccountSwitcherDialogFragment extends DialogFragment {

    private final OnAddAccountClickListener onAddAccountClickListener;
    private DialogAccountSwitcherBinding binding;

    private final AccountSwitcherAdapter.OnAccountClickListener accountClickListener = (model, isCurrent) -> {
        if (isCurrent) {
            dismiss();
            return;
        }
        CookieUtils.setupCookies(model.getCookie());
        settingsHelper.putString(Constants.COOKIE, model.getCookie());
        final FragmentActivity activity = getActivity();
        if (activity != null) activity.recreate();
        dismiss();
    };

    private final AccountSwitcherAdapter.OnAccountLongClickListener accountLongClickListener = (model, isCurrent) -> {
        final Context context = getContext();
        if (context == null) return false;
        if (isCurrent) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.quick_access_cannot_delete_curr)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return true;
        }
        new AlertDialog.Builder(context)
                .setMessage(getString(R.string.quick_access_confirm_delete, model.getUsername()))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Utils.dataBox.delUserCookie(model);
                    dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        dismiss();
        return true;
    };

    public AccountSwitcherDialogFragment(final OnAddAccountClickListener onAddAccountClickListener) {
        this.onAddAccountClickListener = onAddAccountClickListener;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = DialogAccountSwitcherBinding.inflate(inflater, container, false);
        binding.accounts.setLayoutManager(new LinearLayoutManager(getContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        final int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int width = (int) (Utils.displayMetrics.widthPixels * 0.8);
        window.setLayout(width, height);
    }

    private void init() {
        final AccountSwitcherAdapter adapter = new AccountSwitcherAdapter(accountClickListener, accountLongClickListener);
        binding.accounts.setAdapter(adapter);
        final List<DataBox.CookieModel> allUsers = Utils.dataBox.getAllCookies();
        if (allUsers == null) return;
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        sortUserList(cookie, allUsers);
        adapter.submitList(allUsers);
        binding.addAccountBtn.setOnClickListener(v -> {
            if (onAddAccountClickListener == null) return;
            onAddAccountClickListener.onAddAccountClick(this);
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

    public interface OnAddAccountClickListener {
        void onAddAccountClick(final AccountSwitcherDialogFragment dialogFragment);
    }
}
