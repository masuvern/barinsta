package awais.instagrabber.adapters.viewholder;

import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedStoriesListAdapter.OnFeedStoryClickListener;
import awais.instagrabber.adapters.HighlightStoriesListAdapter.OnHighlightStoryClickListener;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.HighlightModel;

public final class StoryListViewHolder extends RecyclerView.ViewHolder {
    private final ItemNotificationBinding binding;

    public StoryListViewHolder(final ItemNotificationBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final FeedStoryModel model,
                     final OnFeedStoryClickListener notificationClickListener) {
        if (model == null) return;
        binding.tvComment.setVisibility(View.GONE);
        binding.tvSubComment.setVisibility(View.GONE);

        binding.tvDate.setText(model.getDateTime());

        binding.tvUsername.setText(model.getProfileModel().getUsername());
        binding.ivProfilePic.setImageURI(model.getProfileModel().getSdProfilePic());
        binding.ivProfilePic.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onProfileClick(model.getProfileModel().getUsername());
        });

        binding.ivPreviewPic.setVisibility(View.VISIBLE);
        binding.ivPreviewPic.setImageURI(model.getFirstStoryModel().getThumbnail());
        binding.ivPreviewPic.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onFeedStoryClick(model);
        });

        itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onFeedStoryClick(model);
        });
    }

    public void bind(final HighlightModel model,
                     final OnHighlightStoryClickListener notificationClickListener) {
        if (model == null) return;
        binding.tvComment.setVisibility(View.GONE);
        binding.tvSubComment.setVisibility(View.GONE);

        binding.tvUsername.setText(model.getDateTime());

        binding.ivProfilePic.setVisibility(View.GONE);

        binding.ivPreviewPic.setVisibility(View.VISIBLE);
        binding.ivPreviewPic.setImageURI(model.getThumbnailUrl());

        itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onHighlightClick(model);
        });
    }
}