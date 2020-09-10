package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.TextUtils;

public class DirectMessageReelShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmRavenMediaBinding binding;

    public DirectMessageReelShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                            @NonNull final LayoutDmRavenMediaBinding binding,
                                            final View.OnClickListener onClickListener,
                                            final MentionClickListener mentionClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        binding.tvMessage.setMentionClickListener(mentionClickListener);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
        CharSequence text = reelShare.getText();
        if (TextUtils.isEmpty(text)) {
            binding.tvMessage.setVisibility(View.GONE);
        } else {
            if (TextUtils.hasMentions(text)) text = TextUtils.getMentionText(text); // for mentions
            binding.tvMessage.setText(text);
        }
        final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
        final MediaItemType mediaType = reelShareMedia.getMediaType();
        if (mediaType == null) {
            binding.mediaExpiredIcon.setVisibility(View.VISIBLE);
        } else {
            binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                    mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
            getGlideRequestManager().load(reelShareMedia.getThumbUrl()).into(binding.ivMediaPreview);
        }
    }
}
