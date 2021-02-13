package awais.instagrabber.adapters.viewholder;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.customviews.VerticalDragHelper;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.databinding.ItemSliderPhotoBinding;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.ResponseBodyUtils;

public class SliderPhotoViewHolder extends SliderItemViewHolder {
    private static final String TAG = "FeedSliderPhotoViewHolder";

    private final ItemSliderPhotoBinding binding;
    private final VerticalDragHelper.OnVerticalDragListener onVerticalDragListener;

    public SliderPhotoViewHolder(@NonNull final ItemSliderPhotoBinding binding,
                                 final VerticalDragHelper.OnVerticalDragListener onVerticalDragListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onVerticalDragListener = onVerticalDragListener;
    }

    public void bind(@NonNull final Media model,
                     final int position,
                     final SliderItemsAdapter.SliderCallback sliderCallback) {
        final ImageRequest requestBuilder = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(ResponseBodyUtils.getImageUrl(model)))
                .setLocalThumbnailPreviewsEnabled(true)
                .build();
        binding.getRoot()
               .setController(Fresco.newDraweeControllerBuilder()
                                    .setImageRequest(requestBuilder)
                                    .setControllerListener(new BaseControllerListener<ImageInfo>() {
                                        @Override
                                        public void onFailure(final String id, final Throwable throwable) {
                                            if (sliderCallback != null) {
                                                sliderCallback.onThumbnailLoaded(position);
                                            }
                                        }

                                        @Override
                                        public void onFinalImageSet(final String id,
                                                                    final ImageInfo imageInfo,
                                                                    final Animatable animatable) {
                                            if (sliderCallback != null) {
                                                sliderCallback.onThumbnailLoaded(position);
                                            }
                                        }
                                    })
                                    .setLowResImageRequest(ImageRequest.fromUri(ResponseBodyUtils.getThumbUrl(model)))
                                    .build());
        // binding.getRoot().setOnClickListener(v -> {
        //     if (sliderCallback != null) {
        //         sliderCallback.onItemClicked(position);
        //     }
        // });
        binding.getRoot().setTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(final MotionEvent e) {
                if (sliderCallback != null) {
                    sliderCallback.onItemClicked(position);
                    return true;
                }
                return false;
            }
        });
        final AnimatedZoomableController zoomableController = AnimatedZoomableController.newInstance();
        zoomableController.setMaxScaleFactor(3f);
        binding.getRoot().setZoomableController(zoomableController);
        if (onVerticalDragListener != null) {
            binding.getRoot().setOnVerticalDragListener(onVerticalDragListener);
        }
    }

    // private void setDimensions(final FeedModel feedModel, final int spanCount, final boolean animate) {
    //     final ViewGroup.LayoutParams layoutParams = binding.imageViewer.getLayoutParams();
    //     final int deviceWidth = Utils.displayMetrics.widthPixels;
    //     final int spanWidth = deviceWidth / spanCount;
    //     final int spanHeight = NumberUtils.getResultingHeight(spanWidth, feedModel.getImageHeight(), feedModel.getImageWidth());
    //     final int width = spanWidth == 0 ? deviceWidth : spanWidth;
    //     final int height = spanHeight == 0 ? deviceWidth + 1 : spanHeight;
    //     if (animate) {
    //         Animation animation = AnimationUtils.expand(
    //                 binding.imageViewer,
    //                 layoutParams.width,
    //                 layoutParams.height,
    //                 width,
    //                 height,
    //                 new Animation.AnimationListener() {
    //                     @Override
    //                     public void onAnimationStart(final Animation animation) {
    //                         showOrHideDetails(spanCount);
    //                     }
    //
    //                     @Override
    //                     public void onAnimationEnd(final Animation animation) {
    //                         // showOrHideDetails(spanCount);
    //                     }
    //
    //                     @Override
    //                     public void onAnimationRepeat(final Animation animation) {
    //
    //                     }
    //                 });
    //         binding.imageViewer.startAnimation(animation);
    //     } else {
    //         layoutParams.width = width;
    //         layoutParams.height = height;
    //         binding.imageViewer.requestLayout();
    //     }
    // }
    //
    // private void showOrHideDetails(final int spanCount) {
    //     if (spanCount == 1) {
    //         binding.itemFeedTop.getRoot().setVisibility(View.VISIBLE);
    //         binding.itemFeedBottom.getRoot().setVisibility(View.VISIBLE);
    //     } else {
    //         binding.itemFeedTop.getRoot().setVisibility(View.GONE);
    //         binding.itemFeedBottom.getRoot().setVisibility(View.GONE);
    //     }
    // }
}
