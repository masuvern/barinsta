package awais.instagrabber.adapters.viewholder;

import android.text.Spannable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.CommentModel;

public final class CommentViewHolder extends RecyclerView.ViewHolder {
    private final MentionClickListener mentionClickListener;
    private final RecyclerView rvChildComments;
    private final ImageView ivProfilePic;
    private final TextView tvUsername, tvDate, tvComment, tvLikes;
    private final View container;

    public CommentViewHolder(@NonNull final View itemView, final View.OnClickListener onClickListener, final MentionClickListener mentionClickListener) {
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

    public final ImageView getProfilePicView() {
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

    public final void setCommment(final CharSequence commment) {
        if (tvComment != null) {
            tvComment.setText(commment, commment instanceof Spannable ? TextView.BufferType.SPANNABLE : TextView.BufferType.NORMAL);
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