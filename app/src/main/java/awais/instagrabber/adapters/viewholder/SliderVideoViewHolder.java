package awais.instagrabber.adapters.viewholder;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.customviews.VerticalDragHelper;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.databinding.LayoutExoCustomControlsBinding;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class SliderVideoViewHolder extends SliderItemViewHolder {
    private static final String TAG = "SliderVideoViewHolder";

    private final LayoutVideoPlayerWithThumbnailBinding binding;
    private final LayoutExoCustomControlsBinding controlsBinding;
    private final boolean loadVideoOnItemClick;
    private VideoPlayerViewHelper videoPlayerViewHelper;

    @SuppressLint("ClickableViewAccessibility")
    public SliderVideoViewHolder(@NonNull final LayoutVideoPlayerWithThumbnailBinding binding,
                                 final VerticalDragHelper.OnVerticalDragListener onVerticalDragListener,
                                 final LayoutExoCustomControlsBinding controlsBinding,
                                 final boolean loadVideoOnItemClick) {
        super(binding.getRoot());
        this.binding = binding;
        this.controlsBinding = controlsBinding;
        this.loadVideoOnItemClick = loadVideoOnItemClick;
        if (onVerticalDragListener != null) {
            final VerticalDragHelper thumbnailVerticalDragHelper = new VerticalDragHelper(binding.thumbnailParent);
            final VerticalDragHelper playerVerticalDragHelper = new VerticalDragHelper(binding.playerView);
            thumbnailVerticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
            playerVerticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
            binding.thumbnailParent.setOnTouchListener((v, event) -> {
                final boolean onDragTouch = thumbnailVerticalDragHelper.onDragTouch(event);
                if (onDragTouch) {
                    return true;
                }
                return thumbnailVerticalDragHelper.onGestureTouchEvent(event);
            });
            binding.playerView.setOnTouchListener((v, event) -> {
                final boolean onDragTouch = playerVerticalDragHelper.onDragTouch(event);
                if (onDragTouch) {
                    return true;
                }
                return playerVerticalDragHelper.onGestureTouchEvent(event);
            });
        }
    }

    public void bind(@NonNull final PostChild model,
                     final int position,
                     final SliderItemsAdapter.SliderCallback sliderCallback) {
        final float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {

            @Override
            public void onThumbnailClick() {
                if (sliderCallback != null) {
                    sliderCallback.onItemClicked(position);
                }
            }

            @Override
            public void onThumbnailLoaded() {
                if (sliderCallback != null) {
                    sliderCallback.onThumbnailLoaded(position);
                }
            }

            @Override
            public void onPlayerViewLoaded() {
                // binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
                final ViewGroup.LayoutParams layoutParams = binding.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils.getResultingHeight(requiredWidth, model.getHeight(), model.getWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                binding.playerView.requestLayout();
                // setMuteIcon(vol == 0f && Utils.sessionVolumeFull ? 1f : vol);
            }

            @Override
            public void onPlay() {
                if (sliderCallback != null) {
                    sliderCallback.onPlayerPlay(position);
                }
            }

            @Override
            public void onPause() {
                if (sliderCallback != null) {
                    sliderCallback.onPlayerPause(position);
                }
            }
        };
        final float aspectRatio = (float) model.getWidth() / model.getHeight();
        videoPlayerViewHelper = new VideoPlayerViewHelper(binding.getRoot().getContext(),
                                                          binding,
                                                          model.getDisplayUrl(),
                                                          vol,
                                                          aspectRatio,
                                                          model.getThumbnailUrl(),
                                                          loadVideoOnItemClick,
                                                          controlsBinding,
                                                          videoPlayerCallback);
        // binding.itemFeedBottom.btnMute.setOnClickListener(v -> {
        //     final float newVol = videoPlayerViewHelper.toggleMute();
        //     setMuteIcon(newVol);
        //     Utils.sessionVolumeFull = newVol == 1f;
        // });
        binding.playerView.setOnClickListener(v -> {
            if (sliderCallback != null) {
                sliderCallback.onItemClicked(position);
            }
        });
    }

    public void pause() {
        if (videoPlayerViewHelper == null) return;
        videoPlayerViewHelper.pause();
    }

    public void releasePlayer() {
        if (videoPlayerViewHelper == null) return;
        videoPlayerViewHelper.releasePlayer();
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
