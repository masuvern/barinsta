package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;

public class DirectMessageDefaultViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectMessageDefaultViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                          @NonNull final LayoutDmTextBinding binding,
                                          final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final Context context = itemView.getContext();
        binding.tvMessage.setText(context.getText(R.string.dms_inbox_raven_message_unknown));
    }
}
