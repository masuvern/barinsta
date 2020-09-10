package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.utils.TextUtils;

public class DirectMessageTextViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectMessageTextViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                       @NonNull final LayoutDmTextBinding binding,
                                       final View.OnClickListener onClickListener,
                                       final MentionClickListener mentionClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        this.binding.tvMessage.setMentionClickListener(mentionClickListener);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final Context context = itemView.getContext();
        CharSequence text = directItemModel.getText();
        text = TextUtils.getSpannableUrl(text.toString()); // for urls
        if (TextUtils.hasMentions(text)) text = TextUtils.getMentionText(text); // for mentions
        if (text instanceof Spanned)
            binding.tvMessage.setText(text, TextView.BufferType.SPANNABLE);
        else if (text == "") {
            binding.tvMessage.setText(context.getText(R.string.dms_inbox_raven_message_unknown));
        } else binding.tvMessage.setText(text);
    }
}
