package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemDefaultViewHolder extends DirectItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectItemDefaultViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                       @NonNull final LayoutDmTextBinding binding,
                                       final User currentUser,
                                       final DirectThread thread,
                                       final MentionClickListener mentionClickListener,
                                       final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        // setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        final Context context = itemView.getContext();
        binding.tvMessage.setText(context.getText(R.string.dms_inbox_raven_message_unknown));
    }
}
