package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.NotificationViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.NotificationModel;

public final class NotificationsAdapter extends ListAdapter<NotificationModel, NotificationViewHolder> {
    private final OnNotificationClickListener notificationClickListener;
    private final MentionClickListener mentionClickListener;

    private static final DiffUtil.ItemCallback<NotificationModel> DIFF_CALLBACK = new DiffUtil.ItemCallback<NotificationModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final NotificationModel oldItem, @NonNull final NotificationModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final NotificationModel oldItem, @NonNull final NotificationModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public NotificationsAdapter(final OnNotificationClickListener notificationClickListener,
                                final MentionClickListener mentionClickListener) {
        super(DIFF_CALLBACK);
        this.notificationClickListener = notificationClickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemNotificationBinding binding = ItemNotificationBinding.inflate(layoutInflater, parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotificationViewHolder holder, final int position) {
        final NotificationModel notificationModel = getItem(position);
        holder.bind(notificationModel, notificationClickListener);
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(final NotificationModel model);
    }
}