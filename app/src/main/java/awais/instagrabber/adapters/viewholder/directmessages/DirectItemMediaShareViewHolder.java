package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.directmessages.Caption;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemClip;
import awais.instagrabber.repositories.responses.directmessages.DirectItemFelixShare;
import awais.instagrabber.repositories.responses.directmessages.DirectItemMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectUser;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemMediaShareViewHolder extends DirectItemViewHolder {

    private final LayoutDmMediaShareBinding binding;
    private final int maxHeight;
    private final int maxWidth;
    private final int dmRadius;
    private final int dmRadiusSmall;
    // private final RoundingParams roundingParams;

    public DirectItemMediaShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                          @NonNull final LayoutDmMediaShareBinding binding,
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
        dmRadius = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius);
        dmRadiusSmall = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius_small);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem item, final MessageDirection messageDirection) {
        removeBg();
        final RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING
                                              ? RoundingParams.fromCornersRadii(dmRadiusSmall, dmRadius, dmRadius, dmRadius)
                                              : RoundingParams.fromCornersRadii(dmRadius, dmRadiusSmall, dmRadius, dmRadius);
        final GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(itemView.getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                .setRoundingParams(roundingParams)
                .build();
        binding.mediaPreview.setHierarchy(hierarchy);
        binding.topBg.setBackgroundResource(messageDirection == MessageDirection.INCOMING
                                            ? R.drawable.bg_media_share_top_incoming
                                            : R.drawable.bg_media_share_top_outgoing);
        DirectItemMedia media = null;
        if (item.getItemType() == DirectItemType.MEDIA_SHARE) {
            media = item.getMediaShare();
        } else if (item.getItemType() == DirectItemType.CLIP) {
            final DirectItemClip clip = item.getClip();
            if (clip == null) return;
            media = clip.getClip();
        } else if (item.getItemType() == DirectItemType.FELIX_SHARE) {
            final DirectItemFelixShare felixShare = item.getFelixShare();
            if (felixShare == null) return;
            media = felixShare.getVideo();
        }
        if (media == null) return;
        final DirectUser user = media.getUser();
        if (user != null) {
            binding.username.setVisibility(View.VISIBLE);
            binding.profilePic.setVisibility(View.VISIBLE);
            binding.username.setText(user.getUsername());
            binding.profilePic.setImageURI(user.getProfilePicUrl());
        } else {
            binding.username.setVisibility(View.GONE);
            binding.profilePic.setVisibility(View.GONE);
        }
        final String title = media.getTitle();
        if (!TextUtils.isEmpty(title)) {
            binding.title.setVisibility(View.VISIBLE);
            binding.title.setText(title);
        } else {
            binding.title.setVisibility(View.GONE);
        }
        final Caption caption = media.getCaption();
        if (caption != null) {
            binding.caption.setVisibility(View.VISIBLE);
            binding.caption.setText(caption.getText());
            binding.caption.setEllipsize(TextUtils.TruncateAt.END);
            binding.caption.setMaxLines(2);
        } else {
            binding.caption.setVisibility(View.GONE);
        }
        final MediaItemType mediaType = media.getMediaType();
        if (mediaType == MediaItemType.MEDIA_TYPE_SLIDER) {
            media = media.getCarouselMedia().get(0);
        }
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                media.getOriginalHeight(),
                media.getOriginalWidth(),
                maxHeight,
                maxWidth
        );
        final ViewGroup.LayoutParams layoutParams = binding.mediaPreview.getLayoutParams();
        layoutParams.width = widthHeight.first != null ? widthHeight.first : 0;
        layoutParams.height = widthHeight.second != null ? widthHeight.second : 0;
        binding.mediaPreview.requestLayout();
        final String url = ResponseBodyUtils.getThumbUrl(media.getImageVersions2());
        binding.mediaPreview.setImageURI(url);
        final boolean showTypeIcon = mediaType == MediaItemType.MEDIA_TYPE_VIDEO || mediaType == MediaItemType.MEDIA_TYPE_SLIDER;
        if (!showTypeIcon) {
            binding.typeIcon.setVisibility(View.GONE);
            return;
        }
        binding.typeIcon.setVisibility(View.VISIBLE);
        binding.typeIcon.setImageResource(mediaType == MediaItemType.MEDIA_TYPE_VIDEO
                                          ? R.drawable.ic_video_24
                                          : R.drawable.ic_checkbox_multiple_blank_stroke);
    }
}
