package awais.instagrabber.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.databinding.PrefAccountSwitcherBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AccountSwitcherListAdapter extends ArrayAdapter<DataBox.CookieModel> {
    private static final String TAG = "AccountSwitcherListAdap";

    private final OnAccountClickListener clickListener;
    private final OnAccountLongClickListener longClickListener;

    public AccountSwitcherListAdapter(@NonNull final Context context,
                                      final int resource,
                                      @NonNull final List<DataBox.CookieModel> allUsers,
                                      final OnAccountClickListener clickListener,
                                      final OnAccountLongClickListener longClickListener) {
        super(context, resource, allUsers);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
        final DataBox.CookieModel model = getItem(position);
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final PrefAccountSwitcherBinding binding = PrefAccountSwitcherBinding.inflate(layoutInflater, parent, false);
            final ViewHolder viewHolder = new ViewHolder(binding);
            viewHolder.itemView.setTag(viewHolder);
            if (model == null) return viewHolder.itemView;
            final boolean equals = model.getCookie().equals(cookie);
            viewHolder.bind(model, equals, clickListener, longClickListener);
            return viewHolder.itemView;
        }
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (model == null) return viewHolder.itemView;
        final boolean equals = model.getCookie().equals(cookie);
        viewHolder.bind(model, equals, clickListener, longClickListener);
        return viewHolder.itemView;
    }

    public interface OnAccountClickListener {
        void onAccountClick(final DataBox.CookieModel model, final boolean isCurrent);
    }

    public interface OnAccountLongClickListener {
        boolean onAccountLongClick(final DataBox.CookieModel model, final boolean isCurrent);
    }

    private static class ViewHolder {
        private final View itemView;
        private final PrefAccountSwitcherBinding binding;

        public ViewHolder(final PrefAccountSwitcherBinding binding) {
            this.itemView = binding.getRoot();
            this.binding = binding;
            binding.arrowDown.setImageResource(R.drawable.ic_check_24);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final DataBox.CookieModel model,
                         final boolean isCurrent,
                         final OnAccountClickListener clickListener,
                         final OnAccountLongClickListener longClickListener) {
            // Log.d(TAG, model.getFullName());
            itemView.setOnClickListener(v -> {
                if (clickListener == null) return;
                clickListener.onAccountClick(model, isCurrent);
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener == null) return false;
                return longClickListener.onAccountLongClick(model, isCurrent);
            });
            binding.profilePic.setImageURI(model.getProfilePic());
            binding.username.setText("@" + model.getUsername());
            binding.fullName.setTypeface(null);
            final String fullName = model.getFullName();
            if (TextUtils.isEmpty(fullName)) {
                binding.fullName.setVisibility(View.GONE);
            } else {
                binding.fullName.setVisibility(View.VISIBLE);
                binding.fullName.setText(fullName);
            }
            if (!isCurrent) {
                binding.arrowDown.setVisibility(View.GONE);
                return;
            }
            binding.fullName.setTypeface(binding.fullName.getTypeface(), Typeface.BOLD);
            binding.arrowDown.setVisibility(View.VISIBLE);
        }
    }
}
