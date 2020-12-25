package awais.instagrabber.adapters.viewholder;

import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.NotificationsAdapter.OnNotificationClickListener;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.models.enums.NotificationType;

public final class NotificationViewHolder extends RecyclerView.ViewHolder {
    private final ItemNotificationBinding binding;

    public NotificationViewHolder(final ItemNotificationBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final NotificationModel model,
                     final OnNotificationClickListener notificationClickListener) {
        if (model == null) return;
        int text = -1;
        CharSequence subtext = null;
        switch (model.getType()) {
            case LIKE:
                text = R.string.liked_notif;
                break;
            case COMMENT:
                text = R.string.comment_notif;
                subtext = model.getText();
                break;
            case MENTION:
                text = R.string.mention_notif;
                subtext = model.getText();
                break;
            case TAGGED:
                text = R.string.tagged_notif;
                break;
            case FOLLOW:
                text = R.string.follow_notif;
                break;
            case REQUEST:
                text = R.string.request_notif;
                subtext = model.getText();
                break;
            case COMMENT_LIKE:
            case TAGGED_COMMENT:
            case RESPONDED_STORY:
                subtext = model.getText();
                break;
        }
        if (text == -1 && subtext != null) {
            binding.tvComment.setText(subtext);
            binding.tvSubComment.setVisibility(View.GONE);
        }
        else if (text != -1) {
            binding.tvComment.setText(text);
            binding.tvSubComment.setText(subtext);
            binding.tvSubComment.setVisibility(subtext == null ? View.GONE : View.VISIBLE);
        }

        if (model.getType() != NotificationType.REQUEST) {
            binding.tvDate.setText(model.getDateTime());
        }

        binding.tvUsername.setText(model.getUsername());
        binding.ivProfilePic.setImageURI(model.getProfilePic());
        binding.ivProfilePic.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onProfileClick(model.getUsername());
        });

        if (TextUtils.isEmpty(model.getPreviewPic())) {
            binding.ivPreviewPic.setVisibility(View.INVISIBLE);
        } else {
            binding.ivPreviewPic.setVisibility(View.VISIBLE);
            binding.ivPreviewPic.setImageURI(model.getPreviewPic());
            binding.ivPreviewPic.setOnClickListener(v -> {
                if (notificationClickListener == null) return;
                notificationClickListener.onPreviewClick(model);
            });
        }

        itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onNotificationClick(model);
        });
    }
}