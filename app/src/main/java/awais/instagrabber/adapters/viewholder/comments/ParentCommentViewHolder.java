package awais.instagrabber.adapters.viewholder.comments;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter.CommentCallback;
import awais.instagrabber.databinding.ItemCommentBinding;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Utils;

public final class ParentCommentViewHolder extends RecyclerView.ViewHolder {

    private final ItemCommentBinding binding;

    public ParentCommentViewHolder(@NonNull final ItemCommentBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final CommentModel comment,
                     final boolean selected,
                     final CommentCallback commentCallback) {
        if (comment == null) return;
        if (commentCallback != null) {
            itemView.setOnClickListener(v -> commentCallback.onClick(comment));
        }
        if (selected) {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.comment_selected));
        } else {
            itemView.setBackgroundColor(itemView.getResources().getColor(android.R.color.transparent));
        }
        setupCommentText(comment, commentCallback);
        binding.tvDate.setText(comment.getDateTime());
        setLiked(comment.getLiked());
        setLikes((int) comment.getLikes());
        setUser(comment);
    }

    private void setupCommentText(final CommentModel comment, final CommentCallback commentCallback) {
        binding.tvComment.clearOnURLClickListeners();
        binding.tvComment.clearOnHashtagClickListeners();
        binding.tvComment.clearOnMentionClickListeners();
        binding.tvComment.clearOnEmailClickListeners();
        binding.tvComment.setText(comment.getText());
        binding.tvComment.addOnHashtagListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText();
            if (commentCallback == null) return;
            commentCallback.onHashtagClick(originalText);
        });
        binding.tvComment.addOnMentionClickListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText();
            if (commentCallback == null) return;
            commentCallback.onMentionClick(originalText);

        });
        binding.tvComment.addOnEmailClickListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText();
            if (commentCallback == null) return;
            commentCallback.onEmailClick(originalText);
        });
        binding.tvComment.addOnURLClickListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText();
            if (commentCallback == null) return;
            commentCallback.onURLClick(originalText);
        });
        binding.tvComment.setOnLongClickListener(v -> {
            Utils.copyText(itemView.getContext(), comment.getText());
            return true;
        });
        binding.tvComment.setOnClickListener(v -> commentCallback.onClick(comment));
    }

    private void setUser(final CommentModel comment) {
        final User profileModel = comment.getProfileModel();
        if (profileModel == null) return;
        binding.tvUsername.setText(profileModel.getUsername());
        binding.ivProfilePic.setImageURI(profileModel.getProfilePicUrl());
        binding.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
    }

    private void setLikes(final int likes) {
        final String likesString = itemView.getResources().getQuantityString(R.plurals.likes_count, likes, likes);
        binding.tvLikes.setText(likesString);
    }

    public final void setLiked(final boolean liked) {
        if (liked) {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.comment_liked));
        }
    }
}