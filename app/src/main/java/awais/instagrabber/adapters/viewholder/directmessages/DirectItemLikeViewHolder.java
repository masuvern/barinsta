package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmLikeBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemLikeViewHolder extends DirectItemViewHolder {

    public DirectItemLikeViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                    @NonNull final LayoutDmLikeBinding binding,
                                    final User currentUser,
                                    final DirectThread thread,
                                    final View.OnClickListener onClickListener,
                                    final MentionClickListener mentionClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        removeBg();
    }
}
