package awais.instagrabber.adapters;

import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import awais.instagrabber.customviews.drawee.ZoomableDraweeView;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public class PostViewerChildAdapter extends ListAdapter<ViewerPostModel, PostViewerChildAdapter.ChildViewHolder> {

    private static final DiffUtil.ItemCallback<ViewerPostModel> diffCallback = new DiffUtil.ItemCallback<ViewerPostModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final ViewerPostModel oldItem, @NonNull final ViewerPostModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId()) && oldItem.getShortCode().equals(newItem.getShortCode());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final ViewerPostModel oldItem, @NonNull final ViewerPostModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId()) && oldItem.getShortCode().equals(newItem.getShortCode());
        }
    };

    public PostViewerChildAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        // final AppCompatTextView textView = new AppCompatTextView(parent.getContext());
        // textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // return new ChildViewHolder(textView);
        final MediaItemType mediaItemType = MediaItemType.valueOf(viewType);
        if (mediaItemType == null) return getPlaceholder(parent);
        switch (mediaItemType) {
            case MEDIA_TYPE_IMAGE:
                return getImageViewHolder(parent);
            case MEDIA_TYPE_VIDEO:
                return getVideoViewHolder(parent);
            default:
                return getPlaceholder(parent);
        }
    }

    private ChildViewHolder getImageViewHolder(final ViewGroup parent) {
        final ZoomableDraweeView view = new ZoomableDraweeView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ChildViewHolder(view);
    }

    private ChildViewHolder getVideoViewHolder(final ViewGroup parent) {
        final PlayerView view = new PlayerView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ChildViewHolder(view);
    }

    private ChildViewHolder getPlaceholder(final ViewGroup parent) {
        final AppCompatTextView textView = new AppCompatTextView(parent.getContext());
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textView.setGravity(Gravity.CENTER);
        textView.setText("Placeholder");
        return new ChildViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChildViewHolder holder, final int position) {
        holder.bind(getItem(position));
    }

    @Override
    public int getItemViewType(final int position) {
        final ViewerPostModel item = getItem(position);
        return item.getItemType().getId();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull final ChildViewHolder holder) {
        if (holder.itemView instanceof PlayerView) {
            final Player player = ((PlayerView) holder.itemView).getPlayer();
            if (player != null) {
                player.setPlayWhenReady(false);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull final ChildViewHolder holder) {
        if (holder.itemView instanceof PlayerView) {
            final Player player = ((PlayerView) holder.itemView).getPlayer();
            if (player != null) {
                player.release();
            }
            return;
        }
        if (holder.itemView instanceof ZoomableDraweeView) {
            ((ZoomableDraweeView) holder.itemView).setController(null);
        }
    }

    public static class ChildViewHolder extends RecyclerView.ViewHolder {

        public ChildViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        public void bind(final ViewerPostModel item) {
            final MediaItemType mediaItemType = item.getItemType();
            switch (mediaItemType) {
                case MEDIA_TYPE_IMAGE:
                    bindImage(item);
                    break;
                case MEDIA_TYPE_VIDEO:
                    bindVideo(item);
                    break;
                default:
            }
        }

        private void bindImage(final ViewerPostModel item) {
            final ZoomableDraweeView imageView = (ZoomableDraweeView) itemView;
            imageView.setController(null);
            final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(item.getDisplayUrl()))
                                                                   .setLocalThumbnailPreviewsEnabled(true)
                                                                   .setProgressiveRenderingEnabled(true)
                                                                   .build();
            final DraweeController controller = Fresco.newDraweeControllerBuilder()
                                                      .setImageRequest(requestBuilder)
                                                      .setOldController(imageView.getController())
                                                      // .setControllerListener(new BaseControllerListener<ImageInfo>() {
                                                      //
                                                      //     @Override
                                                      //     public void onFailure(final String id, final Throwable throwable) {
                                                      //         // viewerBinding.progressView.setVisibility(View.GONE);
                                                      //     }
                                                      //
                                                      //     @Override
                                                      //     public void onFinalImageSet(final String id, final ImageInfo imageInfo, final Animatable animatable) {
                                                      //         // viewerBinding.progressView.setVisibility(View.GONE);
                                                      //     }
                                                      // })
                                                      .build();
            imageView.setController(controller);
        }

        private void bindVideo(final ViewerPostModel item) {
            final SimpleExoPlayer player = new SimpleExoPlayer.Builder(itemView.getContext()).build();
            final PlayerView playerView = (PlayerView) itemView;
            playerView.setPlayer(player);
            float vol = Utils.settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
            if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;
            player.setVolume(vol);
            player.setPlayWhenReady(Utils.settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));
            final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(itemView.getContext(), "instagram"))
                    .createMediaSource(Uri.parse(item.getDisplayUrl()));
            // mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            //     @Override
            //     public void onLoadCompleted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
            //         viewerBinding.progressView.setVisibility(View.GONE);
            //     }
            //
            //     @Override
            //     public void onLoadStarted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
            //         viewerBinding.progressView.setVisibility(View.VISIBLE);
            //     }
            //
            //     @Override
            //     public void onLoadCanceled(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
            //         viewerBinding.progressView.setVisibility(View.GONE);
            //     }
            //
            //     @Override
            //     public void onLoadError(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
            //         viewerBinding.progressView.setVisibility(View.GONE);
            //     }
            // });
            player.prepare(mediaSource);
            player.setVolume(vol);
            // viewerBinding.bottomPanel.btnMute.setImageResource(vol == 0f ? R.drawable.ic_volume_up_24 : R.drawable.ic_volume_off_24);
            // viewerBinding.bottomPanel.btnMute.setOnClickListener(onClickListener);
        }
    }
}
