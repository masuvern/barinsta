package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public class DirectMessagePlaceholderViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectMessagePlaceholderViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                              @NonNull final LayoutDmTextBinding binding,
                                              final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        binding.tvMessage.setText(HtmlCompat.fromHtml(directItemModel.getText().toString(), FROM_HTML_MODE_COMPACT));
    }
}
