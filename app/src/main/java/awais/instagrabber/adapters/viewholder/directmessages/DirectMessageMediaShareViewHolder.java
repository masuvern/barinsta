package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.DirectItemModel.DirectItemMediaModel;
import awais.instagrabber.models.enums.MediaItemType;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public class DirectMessageMediaShareViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmMediaShareBinding binding;

    public DirectMessageMediaShareViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                             @NonNull final LayoutDmMediaShareBinding binding,
                                             final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final Context context = itemView.getContext();
        final DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
        final ProfileModel modelUser = mediaModel.getUser();
        if (modelUser != null) {
            binding.tvMessage.setText(HtmlCompat.fromHtml(
                    "<small>" + context.getString(R.string.dms_inbox_media_shared_from, modelUser.getUsername()) + "</small>",
                    FROM_HTML_MODE_COMPACT));
        }
        binding.ivMediaPreview.setImageURI(mediaModel.getThumbUrl());
        final MediaItemType modelMediaType = mediaModel.getMediaType();
        binding.typeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO
                                               || modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
    }
}
