package awais.instagrabber.adapters.viewholder.directmessages;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemClip;
import awais.instagrabber.repositories.responses.directmessages.DirectItemFelixShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;

public class DirectItemMediaShareViewHolder extends DirectItemViewHolder {

    private final LayoutDmMediaShareBinding binding;
    private final RoundingParams incomingRoundingParams;
    private final RoundingParams outgoingRoundingParams;

    public DirectItemMediaShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                          @NonNull final LayoutDmMediaShareBinding binding,
                                          final User currentUser,
                                          final DirectThread thread,
                                          final MentionClickListener mentionClickListener,
                                          final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        incomingRoundingParams = RoundingParams.fromCornersRadii(dmRadiusSmall, dmRadius, dmRadius, dmRadius);
        outgoingRoundingParams = RoundingParams.fromCornersRadii(dmRadius, dmRadiusSmall, dmRadius, dmRadius);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem item, final MessageDirection messageDirection) {
        final RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING ? incomingRoundingParams : outgoingRoundingParams;
        binding.mediaPreview.setHierarchy(new GenericDraweeHierarchyBuilder(itemView.getResources())
                                                  .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                                                  .setRoundingParams(roundingParams)
                                                  .build());
        binding.topBg.setBackgroundResource(messageDirection == MessageDirection.INCOMING
                                            ? R.drawable.bg_media_share_top_incoming
                                            : R.drawable.bg_media_share_top_outgoing);
        Media media = getMedia(item);
        if (media == null) return;
        final User user = media.getUser();
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
                mediaImageMaxHeight,
                mediaImageMaxWidth
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

    @Nullable
    private Media getMedia(@NonNull final DirectItem item) {
        Media media = null;
        if (item.getItemType() == DirectItemType.MEDIA_SHARE) {
            media = item.getMediaShare();
        } else if (item.getItemType() == DirectItemType.CLIP) {
            final DirectItemClip clip = item.getClip();
            if (clip == null) return null;
            media = clip.getClip();
        } else if (item.getItemType() == DirectItemType.FELIX_SHARE) {
            final DirectItemFelixShare felixShare = item.getFelixShare();
            if (felixShare == null) return null;
            media = felixShare.getVideo();
        }
        return media;
    }
}
