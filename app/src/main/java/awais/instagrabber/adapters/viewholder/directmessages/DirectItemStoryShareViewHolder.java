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
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectItemStoryShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.ImageVersions2;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemStoryShareViewHolder extends DirectItemViewHolder {

    private final LayoutDmStoryShareBinding binding;
    private final int maxHeight;
    private final int maxWidth;
    private final int dmRadius;
    private final int dmRadiusSmall;

    public DirectItemStoryShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                          @NonNull final LayoutDmStoryShareBinding binding,
                                          final ProfileModel currentUser,
                                          final DirectThread thread,
                                          final MentionClickListener mentionClickListener,
                                          final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        maxHeight = itemView.getResources().getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        final Resources resources = itemView.getResources();
        final int margin = resources.getDimensionPixelSize(R.dimen.dm_message_item_margin);
        maxWidth = Utils.displayMetrics.widthPixels - margin - Utils.convertDpToPx(8);
        dmRadius = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius);
        dmRadiusSmall = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius_small);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem item, final MessageDirection messageDirection) {
        removeBg();
        String format = "@%s's story";
        final String reelType = item.getStoryShare().getReelType();
        if (reelType == null || item.getStoryShare().getMedia() == null) {
            setExpiredStoryInfo(item);
            return;
        }
        if (reelType.equals("highlight_reel")) {
            format = "@%s's story highlight";
        }
        final String info = String.format(format, item.getStoryShare().getMedia().getUser().getUsername());
        binding.shareInfo.setText(info);
        binding.text.setVisibility(View.GONE);
        binding.ivMediaPreview.setController(null);
        final DirectItemStoryShare storyShare = item.getStoryShare();
        if (storyShare == null) return;
        final String text = storyShare.getText();
        if (!TextUtils.isEmpty(text)) {
            binding.text.setText(text);
            binding.text.setVisibility(View.VISIBLE);
            return;
        }
        final DirectItemMedia storyShareMedia = storyShare.getMedia();
        final MediaItemType mediaType = storyShareMedia.getMediaType();
        binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ? View.VISIBLE : View.GONE);
        final RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING
                                              ? RoundingParams.fromCornersRadii(dmRadiusSmall, dmRadius, dmRadius, dmRadius)
                                              : RoundingParams.fromCornersRadii(dmRadius, dmRadiusSmall, dmRadius, dmRadius);
        binding.ivMediaPreview.setHierarchy(new GenericDraweeHierarchyBuilder(itemView.getResources())
                                                    .setRoundingParams(roundingParams)
                                                    .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                                                    .build());
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                storyShareMedia.getOriginalHeight(),
                storyShareMedia.getOriginalWidth(),
                maxHeight,
                maxWidth
        );
        final ViewGroup.LayoutParams layoutParams = binding.ivMediaPreview.getLayoutParams();
        layoutParams.width = widthHeight.first != null ? widthHeight.first : 0;
        layoutParams.height = widthHeight.second != null ? widthHeight.second : 0;
        binding.ivMediaPreview.requestLayout();
        final ImageVersions2 imageVersions2 = storyShareMedia.getImageVersions2();
        if (imageVersions2 == null) return;
        final String thumbUrl = ResponseBodyUtils.getThumbUrl(imageVersions2);
        binding.ivMediaPreview.setImageURI(thumbUrl);
    }

    private void setExpiredStoryInfo(final DirectItem item) {
        binding.shareInfo.setText(item.getStoryShare().getTitle());
        binding.text.setVisibility(View.VISIBLE);
        binding.text.setText(item.getStoryShare().getMessage());
        binding.ivMediaPreview.setVisibility(View.GONE);
        binding.typeIcon.setVisibility(View.GONE);
    }
}
