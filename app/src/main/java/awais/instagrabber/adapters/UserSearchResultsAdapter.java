package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import awais.instagrabber.adapters.DirectUsersAdapter.OnDirectUserClickListener;
import awais.instagrabber.adapters.viewholder.directmessages.DirectUserViewHolder;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;

public final class UserSearchResultsAdapter extends ListAdapter<User, DirectUserViewHolder> {

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull final User oldItem, @NonNull final User newItem) {
            return oldItem.getPk() == newItem.getPk();
        }

        @Override
        public boolean areContentsTheSame(@NonNull final User oldItem, @NonNull final User newItem) {
            return oldItem.getUsername().equals(newItem.getUsername()) &&
                    oldItem.getFullName().equals(newItem.getFullName());
        }
    };
    private final boolean showSelection;
    private final Set<Long> selectedUserIds;
    private final OnDirectUserClickListener onUserClickListener;

    public UserSearchResultsAdapter(final boolean showSelection,
                                    final OnDirectUserClickListener onUserClickListener) {
        super(DIFF_CALLBACK);
        this.showSelection = showSelection;
        selectedUserIds = showSelection ? new HashSet<>() : null;
        this.onUserClickListener = onUserClickListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public DirectUserViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final LayoutDmUserItemBinding binding = LayoutDmUserItemBinding.inflate(layoutInflater, parent, false);
        return new DirectUserViewHolder(binding, onUserClickListener, null);

    }

    @Override
    public void onBindViewHolder(@NonNull final DirectUserViewHolder holder, final int position) {
        final User user = getItem(position);
        boolean isSelected = selectedUserIds != null && selectedUserIds.contains(user.getPk());
        holder.bind(position, user, false, false, showSelection, isSelected);
    }

    @Override
    public long getItemId(final int position) {
        return getItem(position).getPk();
    }

    public void setSelectedUser(final long userId, final boolean selected) {
        if (selectedUserIds == null) return;
        int position = -1;
        final List<User> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getPk() == userId) {
                position = i;
                break;
            }
        }
        if (position < 0) return;
        if (selected) {
            selectedUserIds.add(userId);
        } else {
            selectedUserIds.remove(userId);
        }
        notifyItemChanged(position);
    }
}