package awais.instagrabber.adapters.viewholder.feed;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.databinding.ItemFeedVideoBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedVideoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedVideoViewHolder";

    private final ItemFeedVideoBinding binding;
    private final Handler handler;
    private final DefaultDataSourceFactory dataSourceFactory;

    private CacheDataSourceFactory cacheDataSourceFactory;
    private FeedModel feedModel;

    // private final Runnable loadRunnable = new Runnable() {
    //     @Override
    //     public void run() {
    //         // loadPlayer(feedModel);
    //     }
    // };

    public FeedVideoViewHolder(@NonNull final ItemFeedVideoBinding binding,
                               final MentionClickListener mentionClickListener,
                               final View.OnClickListener clickListener,
                               final View.OnLongClickListener longClickListener) {
        super(binding.getRoot(), binding.itemFeedTop, binding.itemFeedBottom, mentionClickListener, clickListener, longClickListener);
        this.binding = binding;
        binding.itemFeedBottom.videoViewsContainer.setVisibility(View.VISIBLE);
        handler = new Handler(Looper.getMainLooper());
        final Context context = binding.getRoot().getContext();
        dataSourceFactory = new DefaultDataSourceFactory(context, "instagram");
        final SimpleCache simpleCache = Utils.getSimpleCacheInstance(context);
        if (simpleCache != null) {
            cacheDataSourceFactory = new CacheDataSourceFactory(simpleCache, dataSourceFactory);
        }
    }

    @Override
    public void bindItem(final FeedModel feedModel,
                         final FeedAdapterV2.OnPostClickListener postClickListener) {
        // Log.d(TAG, "Binding post: " + feedModel.getPostId());
        this.feedModel = feedModel;
        binding.itemFeedBottom.tvVideoViews.setText(String.valueOf(feedModel.getViewCount()));
        // showOrHideDetails(false);
        final float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {

            @Override
            public void onThumbnailClick() {
                postClickListener.onPostClick(feedModel, binding.itemFeedTop.ivProfilePic, binding.videoPost.thumbnail);
            }

            @Override
            public void onPlayerViewLoaded() {
                binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
                final ViewGroup.LayoutParams layoutParams = binding.videoPost.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils.getResultingHeight(requiredWidth, feedModel.getImageHeight(), feedModel.getImageWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                binding.videoPost.playerView.requestLayout();
                setMuteIcon(vol == 0f && Utils.sessionVolumeFull ? 1f : vol);
            }
        };
        // final DataSource.Factory factory = cacheDataSourceFactory != null ? cacheDataSourceFactory : dataSourceFactory;
        // final ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(factory);
        // final Uri uri = Uri.parse(feedModel.getDisplayUrl());
        // final MediaItem mediaItem = MediaItem.fromUri(uri);
        // final ProgressiveMediaSource mediaSource = sourceFactory.createMediaSource(mediaItem);
        final float aspectRatio = (float) feedModel.getImageWidth() / feedModel.getImageHeight();
        final VideoPlayerViewHelper videoPlayerViewHelper = new VideoPlayerViewHelper(binding.getRoot().getContext(),
                                                                                      binding.videoPost,
                                                                                      feedModel.getDisplayUrl(),
                                                                                      vol,
                                                                                      aspectRatio,
                                                                                      feedModel.getThumbnailUrl(),
                                                                                      null,
                                                                                      videoPlayerCallback);
        binding.itemFeedBottom.btnMute.setOnClickListener(v -> {
            final float newVol = videoPlayerViewHelper.toggleMute();
            setMuteIcon(newVol);
            Utils.sessionVolumeFull = newVol == 1f;
        });
        binding.videoPost.playerView.setOnClickListener(v -> videoPlayerViewHelper.togglePlayback());
    }


    private void setMuteIcon(final float vol) {
        binding.itemFeedBottom.btnMute.setImageResource(vol == 0f ? R.drawable.ic_volume_up_24 : R.drawable.ic_volume_off_24);
    }

    public FeedModel getCurrentFeedModel() {
        return feedModel;
    }

    // public void stopPlaying() {
    //     // Log.d(TAG, "Stopping post: " + feedModel.getPostId() + ", player: " + player + ", player.isPlaying: " + (player != null && player.isPlaying()));
    //     handler.removeCallbacks(loadRunnable);
    //     if (player != null) {
    //         player.release();
    //     }
    //     if (binding.videoPost.root.getDisplayedChild() == 1) {
    //         binding.videoPost.root.showPrevious();
    //     }
    // }
    //
    // public void startPlaying() {
    //     handler.removeCallbacks(loadRunnable);
    //     handler.postDelayed(loadRunnable, 800);
    // }

    private void showOrHideDetails(final boolean show) {
        if (show) {
            binding.itemFeedTop.getRoot().setVisibility(View.VISIBLE);
            binding.itemFeedBottom.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.itemFeedTop.getRoot().setVisibility(View.GONE);
            binding.itemFeedBottom.getRoot().setVisibility(View.GONE);
        }
    }
}
