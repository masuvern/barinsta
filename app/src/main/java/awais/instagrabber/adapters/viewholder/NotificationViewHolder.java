package awais.instagrabber.adapters.viewholder;

import android.text.Spannable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.NotificationModel;

public final class NotificationViewHolder extends RecyclerView.ViewHolder {
    private final MentionClickListener mentionClickListener;
    private final ImageView ivProfilePic, ivPreviewPic;
    private final TextView tvUsername, tvDate, tvComment, tvSubComment;
    private final View container, rightContainer;

    public NotificationViewHolder(@NonNull final View itemView, final View.OnClickListener onClickListener, final MentionClickListener mentionClickListener) {
        super(itemView);

        container = itemView.findViewById(R.id.container);
        rightContainer = itemView.findViewById(R.id.rightContainer);
        if (onClickListener != null) container.setOnClickListener(onClickListener);

        this.mentionClickListener = mentionClickListener;

        ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
        ivPreviewPic = itemView.findViewById(R.id.ivPreviewPic);
        tvUsername = itemView.findViewById(R.id.tvUsername);
        tvDate = itemView.findViewById(R.id.tvDate);
        tvComment = itemView.findViewById(R.id.tvComment);
        tvSubComment = itemView.findViewById(R.id.tvSubComment);

        tvUsername.setSelected(true);
        tvDate.setSelected(true);
    }

    public final ImageView getProfilePicView() {
        return ivProfilePic;
    }

    public final ImageView getPreviewPicView() {
        return ivPreviewPic;
    }

    public final void setNotificationModel(final NotificationModel notificationModel) {
        if (container != null) container.setTag(notificationModel);
        if (rightContainer != null) rightContainer.setTag(notificationModel);
    }

    public final void setUsername(final String username) {
        if (tvUsername != null) tvUsername.setText(username);
    }

    public final void setDate(final String date) {
        if (tvDate != null) tvDate.setText(date);
    }

    public final void setCommment(final int commment) {
        if (tvComment != null) {
            tvComment.setText(commment);
        }
    }

    public final void setSubCommment(final CharSequence commment) {
        if (tvSubComment != null) {
            tvSubComment.setText(commment, commment instanceof Spannable ? TextView.BufferType.SPANNABLE : TextView.BufferType.NORMAL);
            ((RamboTextView) tvSubComment).setMentionClickListener(mentionClickListener);
        }
    }
}