package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.ImageVersions2;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemMediaViewHolder extends DirectItemViewHolder {

    private final LayoutDmMediaBinding binding;
    private final int maxHeight;
    private final int maxWidth;
    private final RoundingParams incomingRoundingParams;
    private final RoundingParams outgoingRoundingParams;

    public DirectItemMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                     @NonNull final LayoutDmMediaBinding binding,
                                     final ProfileModel currentUser,
                                     final DirectThread thread,
                                     final MentionClickListener mentionClickListener,
                                     final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        final Resources resources = itemView.getResources();
        maxHeight = resources.getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        final int margin = resources.getDimensionPixelSize(R.dimen.dm_message_item_margin);
        maxWidth = Utils.displayMetrics.widthPixels - margin - Utils.convertDpToPx(8);
        final int dmRadius = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius);
        final int dmRadiusSmall = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius_small);
        incomingRoundingParams = RoundingParams.fromCornersRadii(dmRadiusSmall, dmRadius, dmRadius, dmRadius);
        outgoingRoundingParams = RoundingParams.fromCornersRadii(dmRadius, dmRadiusSmall, dmRadius, dmRadius);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        final RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING ? incomingRoundingParams : outgoingRoundingParams;
        binding.mediaPreview.setHierarchy(new GenericDraweeHierarchyBuilder(itemView.getResources())
                                                  .setRoundingParams(roundingParams)
                                                  .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                                                  .build());
        final DirectItemMedia media = directItemModel.getMedia();
        final MediaItemType modelMediaType = media.getMediaType();
        binding.typeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO || modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER
                                       ? View.VISIBLE
                                       : View.GONE);
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                media.getOriginalHeight(),
                media.getOriginalWidth(),
                maxHeight,
                maxWidth
        );
        final ViewGroup.LayoutParams layoutParams = binding.mediaPreview.getLayoutParams();
        final int width = widthHeight.first != null ? widthHeight.first : 0;
        layoutParams.width = width;
        layoutParams.height = widthHeight.second != null ? widthHeight.second : 0;
        binding.mediaPreview.requestLayout();
        binding.bgTime.getLayoutParams().width = width;
        binding.bgTime.requestLayout();
        final ImageVersions2 imageVersions2 = media.getImageVersions2();
        if (imageVersions2 == null) return;
        final String thumbUrl = ResponseBodyUtils.getThumbUrl(imageVersions2);
        binding.mediaPreview.setImageURI(thumbUrl);
    }
}
