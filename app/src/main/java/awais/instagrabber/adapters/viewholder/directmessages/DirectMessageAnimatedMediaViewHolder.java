package awais.instagrabber.adapters.viewholder.directmessages;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

public class DirectMessageAnimatedMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmAnimatedMediaBinding binding;
    private final int maxHeight;
    private final int maxWidth;

    public DirectMessageAnimatedMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                                @NonNull final LayoutDmAnimatedMediaBinding binding,
                                                final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        maxWidth = (int) (Utils.displayMetrics.widthPixels - Utils.convertDpToPx(64) - getItemMargin());
        setItemView(binding.getRoot());
        removeElevation();
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemAnimatedMediaModel animatedMediaModel = directItemModel.getAnimatedMediaModel();
        final String url = animatedMediaModel.getGifUrl();
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                animatedMediaModel.getHeight(),
                animatedMediaModel.getWidth(),
                maxHeight,
                maxWidth
        );
        binding.ivAnimatedMessage.setVisibility(View.VISIBLE);
        final ViewGroup.LayoutParams layoutParams = binding.ivAnimatedMessage.getLayoutParams();
        final int width = widthHeight.first != null ? widthHeight.first : 0;
        final int height = widthHeight.second != null ? widthHeight.second : 0;
        layoutParams.width = width;
        layoutParams.height = height;
        binding.ivAnimatedMessage.requestLayout();
        final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                        .setResizeOptions(ResizeOptions.forDimensions(width, height))
                                                        .build();
        binding.ivAnimatedMessage.setController(Fresco.newDraweeControllerBuilder()
                                                      .setImageRequest(request)
                                                      .setAutoPlayAnimations(true)
                                                      .build());
    }
}
