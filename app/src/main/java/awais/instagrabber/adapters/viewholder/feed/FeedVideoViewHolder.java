package awais.instagrabber.adapters.viewholder.feed;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ItemFeedVideoBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedVideoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedVideoViewHolder";

    private final ItemFeedVideoBinding binding;
    private final Handler handler;
    private final DefaultDataSourceFactory dataSourceFactory;

    private CacheDataSourceFactory cacheDataSourceFactory;
    private FeedModel feedModel;
    private SimpleExoPlayer player;

    final Runnable loadRunnable = new Runnable() {
        @Override
        public void run() {
            loadPlayer(feedModel);
        }
    };

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
    public void bindItem(final FeedModel feedModel) {
        // Log.d(TAG, "Binding post: " + feedModel.getPostId());
        this.feedModel = feedModel;
        setThumbnail(feedModel);
        binding.itemFeedBottom.tvVideoViews.setText(String.valueOf(feedModel.getViewCount()));
    }

    private void setThumbnail(final FeedModel feedModel) {
        final ViewGroup.LayoutParams layoutParams = binding.thumbnailParent.getLayoutParams();
        final int requiredWidth = Utils.displayMetrics.widthPixels;
        layoutParams.width = feedModel.getImageWidth() == 0 ? requiredWidth : feedModel.getImageWidth();
        layoutParams.height = feedModel.getImageHeight() == 0 ? requiredWidth + 1 : feedModel.getImageHeight();
        binding.thumbnailParent.requestLayout();
        final ImageRequest thumbnailRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(feedModel.getThumbnailUrl()))
                                                                 .setProgressiveRenderingEnabled(true)
                                                                 .build();
        final DraweeController controller = Fresco.newDraweeControllerBuilder()
                                                  .setImageRequest(thumbnailRequest)
                                                  .build();
        binding.thumbnail.setController(controller);
        binding.thumbnailParent.setOnClickListener(v -> loadPlayer(feedModel));
    }

    private void loadPlayer(final FeedModel feedModel) {
        if (feedModel == null) {
            return;
        }
        // Log.d(TAG, "playing post:" + feedModel.getPostId());
        if (binding.viewSwitcher.getDisplayedChild() == 0) {
            binding.viewSwitcher.showNext();
        }
        binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
        final ViewGroup.LayoutParams layoutParams = binding.playerView.getLayoutParams();
        final int requiredWidth = Utils.displayMetrics.widthPixels;
        final int resultingHeight = Utils.getResultingHeight(requiredWidth, feedModel.getImageHeight(), feedModel.getImageWidth());
        layoutParams.width = requiredWidth;
        layoutParams.height = resultingHeight;
        binding.playerView.requestLayout();
        float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;
        setMuteIcon(vol);
        player = (SimpleExoPlayer) binding.playerView.getPlayer();
        if (player != null) {
            player.release();
        }
        player = new SimpleExoPlayer.Builder(itemView.getContext())
                .setLooper(Looper.getMainLooper())
                .build();
        player.setVolume(vol);
        player.setPlayWhenReady(true);
        final DataSource.Factory factory = cacheDataSourceFactory != null ? cacheDataSourceFactory : dataSourceFactory;
        final ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(factory);
        final ProgressiveMediaSource mediaSource = sourceFactory.createMediaSource(Uri.parse(feedModel.getDisplayUrl()));
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.prepare(mediaSource);
        binding.playerView.setPlayer(player);
        final SimpleExoPlayer finalPlayer = player;
        binding.itemFeedBottom.btnMute.setOnClickListener(v -> {
            final float intVol = finalPlayer.getVolume() == 0f ? 1f : 0f;
            finalPlayer.setVolume(intVol);
            setMuteIcon(intVol);
            Utils.sessionVolumeFull = intVol == 1f;
        });
        binding.playerView.setOnClickListener(v -> finalPlayer.setPlayWhenReady(!finalPlayer.getPlayWhenReady()));
    }

    private void setMuteIcon(final float vol) {
        binding.itemFeedBottom.btnMute.setImageResource(vol == 0f ? R.drawable.ic_volume_up_24 : R.drawable.ic_volume_off_24);
    }

    public FeedModel getCurrentFeedModel() {
        return feedModel;
    }

    public void stopPlaying() {
        // Log.d(TAG, "Stopping post: " + feedModel.getPostId() + ", player: " + player + ", player.isPlaying: " + (player != null && player.isPlaying()));
        handler.removeCallbacks(loadRunnable);
        if (player != null) {
            player.release();
        }
        if (binding.viewSwitcher.getDisplayedChild() == 1) {
            binding.viewSwitcher.showPrevious();
        }
    }

    public void startPlaying() {
        handler.removeCallbacks(loadRunnable);
        handler.postDelayed(loadRunnable, 800);
    }
}
