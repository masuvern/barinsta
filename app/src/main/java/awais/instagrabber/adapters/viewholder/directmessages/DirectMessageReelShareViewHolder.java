package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Utils;

public class DirectMessageReelShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmRavenMediaBinding binding;

    public DirectMessageReelShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                            @NonNull final LayoutDmRavenMediaBinding binding,
                                            final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        binding.tvMessage.setVisibility(View.GONE);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
        final String text = reelShare.getText();
        if (!Utils.isEmpty(text)) {
            binding.tvMessage.setText(text);
            binding.tvMessage.setVisibility(View.VISIBLE);
        }
        final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
        final MediaItemType mediaType = reelShareMedia.getMediaType();

        if (mediaType == null)
            binding.mediaExpiredIcon.setVisibility(View.VISIBLE);
        else {
            binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                    mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);

            getGlideRequestManager().load(reelShareMedia.getThumbUrl()).into(binding.ivMediaPreview);
        }
    }
}
