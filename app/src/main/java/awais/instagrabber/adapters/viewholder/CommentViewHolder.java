package awais.instagrabber.adapters.viewholder;

import android.text.Spannable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.CommentModel;

public final class CommentViewHolder extends RecyclerView.ViewHolder {
    private final MentionClickListener mentionClickListener;
    private final RecyclerView rvChildComments;
    private final SimpleDraweeView ivProfilePic;
    private final TextView tvUsername;
    private final TextView tvDate;
    private final TextView tvComment;
    private final TextView tvLikes;
    private final View container;

    public CommentViewHolder(@NonNull final View itemView,
                             final View.OnClickListener onClickListener,
                             final MentionClickListener mentionClickListener) {
        super(itemView);

        container = itemView.findViewById(R.id.container);
        if (onClickListener != null) container.setOnClickListener(onClickListener);

        this.mentionClickListener = mentionClickListener;

        ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
        tvUsername = itemView.findViewById(R.id.tvUsername);
        tvDate = itemView.findViewById(R.id.tvDate);
        tvLikes = itemView.findViewById(R.id.tvLikes);
        tvComment = itemView.findViewById(R.id.tvComment);

        tvUsername.setSelected(true);
        tvDate.setSelected(true);

        rvChildComments = itemView.findViewById(R.id.rvChildComments);
    }

    public final SimpleDraweeView getProfilePicView() {
        return ivProfilePic;
    }

    public final boolean isParent() {
        return rvChildComments != null;
    }

    public final void setCommentModel(final CommentModel commentModel) {
        if (container != null) container.setTag(commentModel);
    }

    public final void setUsername(final String username) {
        if (tvUsername != null) tvUsername.setText(username);
    }

    public final void setDate(final String date) {
        if (tvDate != null) tvDate.setText(date);
    }

    public final void setLikes(final String likes) {
        if (tvLikes != null) tvLikes.setText(likes);
    }

    public final void setLiked(final boolean liked) {
        if (liked) container.setBackgroundColor(0x40FF69B4);
    }

    public final void setComment(final CharSequence comment) {
        if (tvComment != null) {
            tvComment.setText(comment, comment instanceof Spannable ? TextView.BufferType.SPANNABLE : TextView.BufferType.NORMAL);
            ((RamboTextView) tvComment).setMentionClickListener(mentionClickListener);
        }
    }

    public final void setChildAdapter(final CommentsAdapter adapter) {
        if (isParent()) {
            rvChildComments.setAdapter(adapter);
            rvChildComments.setVisibility(View.VISIBLE);
        }
    }

    public final void hideChildComments() {
        if (isParent()) rvChildComments.setVisibility(View.GONE);
    }
}