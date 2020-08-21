package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;

public class DirectMessageAnimatedMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmAnimatedMediaBinding binding;

    public DirectMessageAnimatedMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                                @NonNull final LayoutDmAnimatedMediaBinding binding,
                                                final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        getGlideRequestManager().asGif().load(directItemModel.getAnimatedMediaModel().getGifUrl())
                .into(binding.ivAnimatedMessage);
        binding.ivAnimatedMessage.setVisibility(View.VISIBLE);
    }
}
