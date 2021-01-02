package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemPlaceholderViewHolder extends DirectItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectItemPlaceholderViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                           final LayoutDmTextBinding binding,
                                           final ProfileModel currentUser,
                                           final DirectThread thread,
                                           final MentionClickListener mentionClickListener,
                                           final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        final String text = String.format("%s: %s", directItemModel.getPlaceholder().getTitle(), directItemModel.getPlaceholder().getMessage());
        binding.tvMessage.setText(text);
    }
}
