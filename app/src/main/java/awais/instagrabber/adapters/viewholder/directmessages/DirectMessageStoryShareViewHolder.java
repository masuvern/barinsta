package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Utils;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public class DirectMessageStoryShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmStoryShareBinding binding;

    public DirectMessageStoryShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                             @NonNull final LayoutDmStoryShareBinding binding,
                                             final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        binding.tvMessage.setVisibility(View.GONE);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemReelShareModel reelShare = directItemModel.getReelShare();
        if (reelShare == null) {
            binding.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), FROM_HTML_MODE_COMPACT));
            binding.tvMessage.setVisibility(View.VISIBLE);
        } else {
            final String text = reelShare.getText();
            if (!Utils.isEmpty(text)) {
                binding.tvMessage.setText(text);
                binding.tvMessage.setVisibility(View.VISIBLE);
            }
            else {
                final DirectItemModel.DirectItemMediaModel reelShareMedia = reelShare.getMedia();
                final MediaItemType mediaType = reelShareMedia.getMediaType();
                binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ? View.VISIBLE : View.GONE);
                getGlideRequestManager().load(reelShareMedia.getThumbUrl()).into(binding.ivMediaPreview);
            }
        }
    }
}
