package awais.instagrabber.adapters.viewholder.feed;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.databinding.ItemFeedPhotoBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class FeedPhotoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedPhotoViewHolder";

    private final ItemFeedPhotoBinding binding;

    public FeedPhotoViewHolder(@NonNull final ItemFeedPhotoBinding binding,
                               final MentionClickListener mentionClickListener,
                               final View.OnClickListener clickListener,
                               final View.OnLongClickListener longClickListener) {
        super(binding.getRoot(), binding.itemFeedTop, binding.itemFeedBottom, mentionClickListener, clickListener, longClickListener);
        this.binding = binding;
        binding.itemFeedBottom.videoViewsContainer.setVisibility(View.GONE);
        binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
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
        final ViewGroup.LayoutParams layoutParams = binding.imageViewer.getLayoutParams();
        final int requiredWidth = Utils.displayMetrics.widthPixels;
        layoutParams.width = feedModel.getImageWidth() == 0 ? requiredWidth : feedModel.getImageWidth();
        layoutParams.height = feedModel.getImageHeight() == 0 ? requiredWidth + 1 : feedModel.getImageHeight();
        binding.imageViewer.requestLayout();
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
        // binding.imageViewer.setImageURI(url);
        // final RequestBuilder<Bitmap> thumbnailRequestBuilder = glide
        //         .asBitmap()
        //         .load(thumbnailUrl)
        //         .diskCacheStrategy(DiskCacheStrategy.ALL);
        // glide.asBitmap()
        //      .load(url)
        //      .thumbnail(thumbnailRequestBuilder)
        //      .diskCacheStrategy(DiskCacheStrategy.ALL)
        //      .into(customTarget);

    }
}
