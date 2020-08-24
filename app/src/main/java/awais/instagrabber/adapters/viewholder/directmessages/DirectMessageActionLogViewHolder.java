package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

public class DirectMessageActionLogViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectMessageActionLogViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                            @NonNull final LayoutDmTextBinding binding,
                                            final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final String text = directItemModel.getActionLogModel().getDescription();
        binding.tvMessage.setText(HtmlCompat.fromHtml("<small>" + text + "</small>", FROM_HTML_MODE_COMPACT));
        binding.tvMessage.setVisibility(View.VISIBLE);
    }
}
