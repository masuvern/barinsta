package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;

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
        binding.ivAnimatedMessage.setController(Fresco.newDraweeControllerBuilder()
                                                      .setUri(directItemModel.getAnimatedMediaModel().getGifUrl())
                                                      .setAutoPlayAnimations(true)
                                                      .build());
        binding.ivAnimatedMessage.setVisibility(View.VISIBLE);
    }
}
