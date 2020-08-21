package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;

public class DirectMessageMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmMediaBinding binding;

    public DirectMessageMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                        @NonNull final LayoutDmMediaBinding binding,
                                        final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
        getGlideRequestManager().load(mediaModel.getThumbUrl()).into(binding.ivMediaPreview);
        final MediaItemType modelMediaType = mediaModel.getMediaType();
        binding.typeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO
                || modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
    }
}
