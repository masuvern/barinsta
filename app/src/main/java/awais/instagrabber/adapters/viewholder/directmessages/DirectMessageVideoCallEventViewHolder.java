package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;

public class DirectMessageVideoCallEventViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectMessageVideoCallEventViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                                 @NonNull final LayoutDmTextBinding binding,
                                                 final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        // todo add call event info
        binding.tvMessage.setVisibility(View.VISIBLE);
        binding.tvMessage.setBackgroundColor(0xFF_1F90E6);
    }
}
