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
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.TextUtils;

public class FeedPhotoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedPhotoViewHolder";

    private final ItemFeedPhotoBinding binding;
    // private final long animationDuration;

    public FeedPhotoViewHolder(@NonNull final ItemFeedPhotoBinding binding,
                               final MentionClickListener mentionClickListener,
                               final View.OnClickListener clickListener,
                               final View.OnLongClickListener longClickListener) {
        super(binding.getRoot(), binding.itemFeedTop, binding.itemFeedBottom, mentionClickListener, clickListener, longClickListener);
        this.binding = binding;
        // this.animationDuration = animationDuration;
        binding.itemFeedBottom.videoViewsContainer.setVisibility(View.GONE);
        binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
        binding.imageViewer.setAllowTouchInterceptionWhileZoomed(false);
        final GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(itemView.getContext().getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build();
        binding.imageViewer.setHierarchy(hierarchy);
    }

    @Override
    public void bindItem(final FeedModel feedModel,
                         final FeedAdapterV2.OnPostClickListener postClickListener) {
        if (feedModel == null) {
            return;
        }
        setDimensions(feedModel);
        showOrHideDetails(false);
        final String thumbnailUrl = feedModel.getThumbnailUrl();
        String url = feedModel.getDisplayUrl();
        if (TextUtils.isEmpty(url)) url = thumbnailUrl;
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .setProgressiveRenderingEnabled(true)
                                                               .build();
        binding.imageViewer.setController(Fresco.newDraweeControllerBuilder()
                                                .setImageRequest(requestBuilder)
                                                .setOldController(binding.imageViewer.getController())
                                                .setLowResImageRequest(ImageRequest.fromUri(thumbnailUrl))
                                                .build());
        binding.imageViewer.setTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                if (postClickListener != null) {
                    postClickListener.onPostClick(feedModel, binding.itemFeedTop.ivProfilePic, binding.imageViewer);
                    return true;
                }
                return false;
            }
        });
    }

    private void setDimensions(final FeedModel feedModel) {
        // final ViewGroup.LayoutParams layoutParams = binding.imageViewer.getLayoutParams();
        // final int deviceWidth = Utils.displayMetrics.widthPixels;
        // final int spanWidth = deviceWidth / spanCount;
        // final int spanHeight = NumberUtils.getResultingHeight(spanWidth, feedModel.getImageHeight(), feedModel.getImageWidth());
        // final int width = spanWidth == 0 ? deviceWidth : spanWidth;
        // final int height = spanHeight == 0 ? deviceWidth + 1 : spanHeight;
        final float aspectRatio = (float) feedModel.getImageWidth() / feedModel.getImageHeight();
        binding.imageViewer.setAspectRatio(aspectRatio);
        // Log.d(TAG, "setDimensions: aspectRatio:" + aspectRatio);
        // if (animate) {
        //     Animation animation = AnimationUtils.expand(
        //             binding.imageViewer,
        //             layoutParams.width,
        //             layoutParams.height,
        //             width,
        //             height,
        //             new Animation.AnimationListener() {
        //                 @Override
        //                 public void onAnimationStart(final Animation animation) {
        //                     showOrHideDetails(spanCount);
        //                 }
        //
        //                 @Override
        //                 public void onAnimationEnd(final Animation animation) {
        //                     // showOrHideDetails(spanCount);
        //                 }
        //
        //                 @Override
        //                 public void onAnimationRepeat(final Animation animation) {
        //
        //                 }
        //             });
        //     binding.imageViewer.startAnimation(animation);
        // } else {
        //     layoutParams.width = width;
        //     layoutParams.height = height;
        //     binding.imageViewer.requestLayout();
        // }
    }

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
