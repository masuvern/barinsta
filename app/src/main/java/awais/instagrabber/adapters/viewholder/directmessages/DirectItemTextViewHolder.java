package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemTextViewHolder extends DirectItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectItemTextViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                    @NonNull final LayoutDmTextBinding binding,
                                    final User currentUser,
                                    final DirectThread thread,
                                    final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        final String text = directItemModel.getText();
        if (text == null) return;
        binding.tvMessage.setText(text);
        // setupListeners();
    }

    @Override
    protected boolean showBackground() {
        return true;
    }

    // private void setupListeners() {
    //     binding.tvMessage.addOnHashtagListener(autoLinkItem -> {
    //         final String hashtag = autoLinkItem.getOriginalText().trim();
    //     });
    //     binding.tvMessage.addOnMentionClickListener(autoLinkItem -> {
    //         final String mention = autoLinkItem.getOriginalText().trim();
    //     });
    //     binding.tvMessage.addOnEmailClickListener(autoLinkItem -> {
    //         final String email = autoLinkItem.getOriginalText().trim();
    //     });
    //     binding.tvMessage.addOnURLClickListener(autoLinkItem -> {
    //         final String url = autoLinkItem.getOriginalText().trim();
    //     });
    //     binding.tvMessage.setOnLongClickListener(v -> {
    //         return true;
    //     });
    // }
}
