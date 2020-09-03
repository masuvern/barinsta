package awais.instagrabber.adapters.viewholder;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.PostViewAdapter.OnPostCaptionLongClickListener;
import awais.instagrabber.adapters.PostViewAdapter.OnPostViewChildViewClickListener;
import awais.instagrabber.adapters.PostViewerChildAdapter;
import awais.instagrabber.databinding.ItemFullPostViewBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.ViewerPostModelWrapper;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Utils;

public class PostViewerViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "PostViewerViewHolder";

    private final ItemFullPostViewBinding binding;
    private int currentChildPosition;

    public PostViewerViewHolder(@NonNull final ItemFullPostViewBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        binding.topPanel.viewStoryPost.setVisibility(View.GONE);
    }

    public void bind(final ViewerPostModelWrapper wrapper,
                     final int position,
                     final OnPostViewChildViewClickListener clickListener,
                     final OnPostCaptionLongClickListener longClickListener,
                     final MentionClickListener mentionClickListener) {
        if (wrapper == null) return;
        final ViewerPostModel[] items = wrapper.getViewerPostModels();
        if (items == null || items.length <= 0) return;
        if (items[0] == null) return;
        final PostViewerChildAdapter adapter = new PostViewerChildAdapter();
        binding.mediaViewPager.setAdapter(adapter);
        final ViewerPostModel firstPost = items[0];
        setPostInfo(firstPost, mentionClickListener);
        setMediaItems(items, adapter);
        setupListeners(wrapper,
                       position,
                       clickListener,
                       longClickListener,
                       mentionClickListener,
                       firstPost.getLocation());
    }

    private void setPostInfo(final ViewerPostModel firstPost,
                             final MentionClickListener mentionClickListener) {
        final ProfileModel profileModel = firstPost.getProfileModel();
        if (profileModel == null) return;
        binding.topPanel.title.setText(profileModel.getUsername());
        final String locationName = firstPost.getLocationName();
        if (!Utils.isEmpty(locationName)) {
            binding.topPanel.location.setVisibility(View.VISIBLE);
            binding.topPanel.location.setText(locationName);
        } else binding.topPanel.location.setVisibility(View.GONE);
        binding.topPanel.ivProfilePic.setImageURI(profileModel.getSdProfilePic());
        binding.bottomPanel.commentsCount.setText(String.valueOf(firstPost.getCommentsCount()));
        final CharSequence postCaption = firstPost.getPostCaption();
        if (Utils.hasMentions(postCaption)) {
            binding.bottomPanel.viewerCaption
                    .setText(Utils.getMentionText(postCaption), TextView.BufferType.SPANNABLE);
            binding.bottomPanel.viewerCaption.setMentionClickListener(mentionClickListener);
        } else {
            binding.bottomPanel.viewerCaption.setMentionClickListener(null);
            binding.bottomPanel.viewerCaption.setText(postCaption);
        }
        binding.bottomPanel.tvPostDate.setText(firstPost.getPostDate());
        setupLikes(firstPost);
        setupSave(firstPost);
    }

    private void setupLikes(final ViewerPostModel firstPost) {
        final boolean liked = firstPost.getLike();
        final long likeCount = firstPost.getLikes();
        final Resources resources = itemView.getContext().getResources();
        if (liked) {
            final String unlikeString = resources.getString(R.string.unlike, String.valueOf(likeCount));
            binding.btnLike.setText(unlikeString);
            ViewCompat.setBackgroundTintList(binding.btnLike,
                                             ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.btn_pink_background)));
        } else {
            final String likeString = resources.getString(R.string.like, String.valueOf(likeCount));
            binding.btnLike.setText(likeString);
            ViewCompat.setBackgroundTintList(binding.btnLike,
                                             ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.btn_lightpink_background)));
        }
    }

    private void setupSave(final ViewerPostModel firstPost) {
        final boolean saved = firstPost.getBookmark();
        if (saved) {
            binding.btnBookmark.setText(R.string.unbookmark);
            ViewCompat.setBackgroundTintList(binding.btnBookmark,
                                             ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.btn_orange_background)));
        } else {
            binding.btnBookmark.setText(R.string.bookmark);
            ViewCompat.setBackgroundTintList(
                    binding.btnBookmark,
                    ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.btn_lightorange_background)));
        }
    }

    private void setupListeners(final ViewerPostModelWrapper wrapper,
                                final int position,
                                final OnPostViewChildViewClickListener clickListener,
                                final OnPostCaptionLongClickListener longClickListener,
                                final MentionClickListener mentionClickListener,
                                final String location) {
        final View.OnClickListener onClickListener = v -> clickListener
                .onClick(v, wrapper, position, currentChildPosition);
        binding.bottomPanel.btnComments.setOnClickListener(onClickListener);
        binding.topPanel.title.setOnClickListener(onClickListener);
        binding.topPanel.ivProfilePic.setOnClickListener(onClickListener);
        binding.bottomPanel.btnDownload.setOnClickListener(onClickListener);
        binding.bottomPanel.viewerCaption.setOnClickListener(onClickListener);
        binding.btnLike.setOnClickListener(onClickListener);
        binding.btnBookmark.setOnClickListener(onClickListener);
        binding.bottomPanel.viewerCaption.setOnLongClickListener(v -> {
            longClickListener.onLongClick(binding.bottomPanel.viewerCaption.getText().toString());
            return true;
        });
        if (!Utils.isEmpty(location)) {
            binding.topPanel.location.setOnClickListener(v -> mentionClickListener
                    .onClick(binding.topPanel.location, location, false, true));
        }
    }

    private void setMediaItems(final ViewerPostModel[] items,
                               final PostViewerChildAdapter adapter) {
        final List<ViewerPostModel> filteredList = new ArrayList<>();
        for (final ViewerPostModel model : items) {
            final MediaItemType itemType = model.getItemType();
            if (itemType == MediaItemType.MEDIA_TYPE_VIDEO || itemType == MediaItemType.MEDIA_TYPE_IMAGE) {
                filteredList.add(model);
            }
        }
        binding.mediaCounter.setVisibility(filteredList.size() > 1 ? View.VISIBLE : View.GONE);
        final String counter = "1/" + filteredList.size();
        binding.mediaCounter.setText(counter);
        adapter.submitList(filteredList);
        binding.mediaViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                if (filteredList.size() <= 0 || position >= filteredList.size()) return;
                currentChildPosition = position;
                final String counter = (position + 1) + "/" + filteredList.size();
                binding.mediaCounter.setText(counter);
                final ViewerPostModel viewerPostModel = filteredList.get(position);
                if (viewerPostModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                    setVideoDetails(viewerPostModel);
                    setVolumeListener(position);
                    return;
                }
                setImageDetails();
            }
        });
    }

    private void setVolumeListener(final int position) {
        binding.bottomPanel.btnMute.setOnClickListener(v -> {
            try {
                final RecyclerView.ViewHolder viewHolder = ((RecyclerView) binding.mediaViewPager
                        .getChildAt(0)).findViewHolderForAdapterPosition(position);
                if (viewHolder != null) {
                    final View itemView = viewHolder.itemView;
                    if (itemView instanceof PlayerView) {
                        final SimpleExoPlayer player = (SimpleExoPlayer) ((PlayerView) itemView)
                                .getPlayer();
                        if (player == null) return;
                        final float vol = player.getVolume() == 0f ? 1f : 0f;
                        player.setVolume(vol);
                        binding.bottomPanel.btnMute.setImageResource(vol == 0f ? R.drawable.ic_volume_up_24
                                                                               : R.drawable.ic_volume_off_24);
                        Utils.sessionVolumeFull = vol == 1f;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        });
    }

    private void setImageDetails() {
        binding.bottomPanel.btnMute.setVisibility(View.GONE);
        binding.bottomPanel.videoViewsContainer.setVisibility(View.GONE);
    }

    private void setVideoDetails(final ViewerPostModel viewerPostModel) {
        binding.bottomPanel.btnMute.setVisibility(View.VISIBLE);
        final long videoViews = viewerPostModel.getVideoViews();
        if (videoViews < 0) {
            binding.bottomPanel.videoViewsContainer.setVisibility(View.GONE);
            return;
        }
        binding.bottomPanel.tvVideoViews.setText(String.valueOf(videoViews));
        binding.bottomPanel.videoViewsContainer.setVisibility(View.VISIBLE);
    }

    public void stopPlayingVideo() {
        try {
            final RecyclerView.ViewHolder viewHolder = ((RecyclerView) binding.mediaViewPager
                    .getChildAt(0)).findViewHolderForAdapterPosition(currentChildPosition);
            if (viewHolder != null) {
                final View itemView = viewHolder.itemView;
                if (itemView instanceof PlayerView) {
                    final Player player = ((PlayerView) itemView).getPlayer();
                    if (player != null) {
                        player.setPlayWhenReady(false);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }
}
