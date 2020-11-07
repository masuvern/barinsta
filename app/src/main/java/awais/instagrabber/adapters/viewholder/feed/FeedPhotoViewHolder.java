package awais.instagrabber.adapters.viewholder.feed;

import android.net.Uri;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.databinding.ItemFeedPhotoBinding;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.TextUtils;

public class FeedPhotoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedPhotoViewHolder";

    private final ItemFeedPhotoBinding binding;
    private final FeedAdapterV2.FeedItemCallback feedItemCallback;

    public FeedPhotoViewHolder(@NonNull final ItemFeedPhotoBinding binding,
                               final FeedAdapterV2.FeedItemCallback feedItemCallback) {
        super(binding.getRoot(), binding.itemFeedTop, binding.itemFeedBottom, feedItemCallback);
        this.binding = binding;
        this.feedItemCallback = feedItemCallback;
        binding.itemFeedBottom.tvVideoViews.setVisibility(View.GONE);
        // binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
        binding.imageViewer.setAllowTouchInterceptionWhileZoomed(false);
        final GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(itemView.getContext().getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build();
        binding.imageViewer.setHierarchy(hierarchy);
    }

    @Override
    public void bindItem(final FeedModel feedModel) {
        if (feedModel == null) {
            return;
        }
        binding.getRoot().post(() -> {
            setDimensions(feedModel);
            final String thumbnailUrl = feedModel.getThumbnailUrl();
            String url = feedModel.getDisplayUrl();
            if (TextUtils.isEmpty(url)) url = thumbnailUrl;
            final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                                   // .setLocalThumbnailPreviewsEnabled(true)
                                                                   // .setProgressiveRenderingEnabled(true)
                                                                   .build();
            binding.imageViewer.setController(Fresco.newDraweeControllerBuilder()
                                                    .setImageRequest(requestBuilder)
                                                    .setOldController(binding.imageViewer.getController())
                                                    .setLowResImageRequest(ImageRequest.fromUri(thumbnailUrl))
                                                    .build());
            binding.imageViewer.setTapListener(new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(final MotionEvent e) {
                    if (feedItemCallback != null) {
                        feedItemCallback.onPostClick(feedModel, binding.itemFeedTop.ivProfilePic, binding.imageViewer);
                        return true;
                    }
                    return false;
                }
            });
        });
    }

    private void setDimensions(final FeedModel feedModel) {
        final float aspectRatio = (float) feedModel.getImageWidth() / feedModel.getImageHeight();
        binding.imageViewer.setAspectRatio(aspectRatio);
    }
}
