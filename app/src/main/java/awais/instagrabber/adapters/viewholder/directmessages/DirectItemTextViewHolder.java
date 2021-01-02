package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemTextViewHolder extends DirectItemViewHolder {

    private final LayoutDmTextBinding binding;

    public DirectItemTextViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                    @NonNull final LayoutDmTextBinding binding,
                                    final ProfileModel currentUser,
                                    final DirectThread thread,
                                    final View.OnClickListener onClickListener,
                                    final MentionClickListener mentionClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        // this.binding.tvMessage.setMentionClickListener(mentionClickListener);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        // final Context context = itemView.getContext();
        final String text = directItemModel.getText();
        if (text == null) return;
        binding.tvMessage.setText(text);
        // text = TextUtils.getSpannableUrl(text.toString()); // for urls
        // if (TextUtils.hasMentions(text)) text = TextUtils.getMentionText(text); // for mentions
        // if (text instanceof Spanned)
        //     binding.tvMessage.setText(text, TextView.BufferType.SPANNABLE);
        // else if (text == "") {
        //     binding.tvMessage.setText(context.getText(R.string.dms_inbox_raven_message_unknown));
        // } else binding.tvMessage.setText(text);
    }
}
