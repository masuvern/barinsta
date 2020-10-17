package awais.instagrabber.adapters.viewholder;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.databinding.ItemFeedGridBinding;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.models.PostsLayoutPreferences.PostsLayoutType.STAGGERED_GRID;

public class FeedGridItemViewHolder extends RecyclerView.ViewHolder {
    private final ItemFeedGridBinding binding;

    public FeedGridItemViewHolder(@NonNull final ItemFeedGridBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        // for rounded borders (clip view to background shape)
        //
    }

    public void bind(@NonNull final FeedModel feedModel,
                     @NonNull final PostsLayoutPreferences layoutPreferences,
                     final FeedAdapterV2.OnPostClickListener postClickListener,
                     final boolean animate) {
        if (postClickListener != null) {
            itemView.setOnClickListener(v -> postClickListener.onPostClick(feedModel, binding.profilePic, binding.postImage));
        }
        itemView.setClipToOutline(layoutPreferences.getHasRoundedCorners());
        if (layoutPreferences.getType() == STAGGERED_GRID) {
            final float aspectRatio = (float) feedModel.getImageWidth() / feedModel.getImageHeight();
            binding.postImage.setAspectRatio(aspectRatio);
        } else {
            binding.postImage.setAspectRatio(1);
        }
        if (layoutPreferences.isAvatarVisible()) {
            binding.profilePic.setVisibility(View.VISIBLE);
            binding.profilePic.setImageURI(feedModel.getProfileModel().getSdProfilePic());
            final ViewGroup.LayoutParams layoutParams = binding.profilePic.getLayoutParams();
            @DimenRes final int dimenRes;
            switch (layoutPreferences.getProfilePicSize()) {
                case SMALL:
                    dimenRes = R.dimen.profile_pic_size_small;
                    break;
                case TINY:
                    dimenRes = R.dimen.profile_pic_size_tiny;
                    break;
                default:
                case REGULAR:
                    dimenRes = R.dimen.profile_pic_size_regular;
                    break;
            }
            final int dimensionPixelSize = itemView.getResources().getDimensionPixelSize(dimenRes);
            layoutParams.width = dimensionPixelSize;
            layoutParams.height = dimensionPixelSize;
            binding.profilePic.requestLayout();
        } else {
            binding.profilePic.setVisibility(View.GONE);
        }
        if (layoutPreferences.isNameVisible()) {
            binding.name.setVisibility(View.VISIBLE);
            binding.name.setText(feedModel.getProfileModel().getName());
        } else {
            binding.name.setVisibility(View.GONE);
        }
        String thumbnailUrl = null;
        final int typeIconRes;
        switch (feedModel.getItemType()) {
            case MEDIA_TYPE_IMAGE:
                typeIconRes = -1;
                thumbnailUrl = feedModel.getThumbnailUrl();
                break;
            case MEDIA_TYPE_VIDEO:
                thumbnailUrl = feedModel.getThumbnailUrl();
                typeIconRes = R.drawable.exo_icon_play;
                break;
            case MEDIA_TYPE_SLIDER:
                final List<PostChild> sliderItems = feedModel.getSliderItems();
                if (sliderItems != null) {
                    thumbnailUrl = sliderItems.get(0).getThumbnailUrl();
                }
                typeIconRes = R.drawable.ic_checkbox_multiple_blank_stroke;
                break;
            default:
                typeIconRes = -1;
                thumbnailUrl = null;
        }
        if (TextUtils.isEmpty(thumbnailUrl)) {
            binding.postImage.setController(null);
            return;
        }
        if (typeIconRes <= 0) {
            binding.typeIcon.setVisibility(View.GONE);
        } else {
            binding.typeIcon.setVisibility(View.VISIBLE);
            binding.typeIcon.setImageResource(typeIconRes);
        }
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(thumbnailUrl))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .setProgressiveRenderingEnabled(true)
                                                               .build();
        final PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
                                                              .setImageRequest(requestBuilder)
                                                              .setOldController(binding.postImage.getController());
        if (animate) {
            final BaseControllerListener<ImageInfo> imageListener = new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(final String id, final ImageInfo imageInfo, final Animatable animatable) {
                    setAnimation(binding.getRoot());
                }
            };
            builder.setControllerListener(imageListener);
        }
        binding.postImage.setController(builder.build());
    }

    private void setAnimation(View viewToAnimate) {
        final Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.fade_in);
        animation.setDuration(300);
        viewToAnimate.startAnimation(animation);
    }

    public void clearAnimation() {
        binding.getRoot().clearAnimation();
    }
}
