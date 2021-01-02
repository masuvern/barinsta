package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmLinkBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemLink;
import awais.instagrabber.repositories.responses.directmessages.DirectItemLinkContext;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemLinkViewHolder extends DirectItemViewHolder {

    private final LayoutDmLinkBinding binding;

    public DirectItemLinkViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                    final LayoutDmLinkBinding binding,
                                    final ProfileModel currentUser,
                                    final DirectThread thread,
                                    final MentionClickListener mentionClickListener,
                                    final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        final int margin = itemView.getResources().getDimensionPixelSize(R.dimen.dm_message_item_margin);
        final int width = Utils.displayMetrics.widthPixels - margin - Utils.convertDpToPx(8);
        final ViewGroup.LayoutParams layoutParams = binding.preview.getLayoutParams();
        layoutParams.width = width;
        binding.preview.requestLayout();
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        final DirectItemLink link = directItemModel.getLink();
        final DirectItemLinkContext linkContext = link.getLinkContext();
        final String linkImageUrl = linkContext.getLinkImageUrl();
        if (TextUtils.isEmpty(linkImageUrl)) {
            binding.preview.setVisibility(View.GONE);
        } else {
            binding.preview.setVisibility(View.VISIBLE);
            binding.preview.setImageURI(linkImageUrl);
        }
        if (TextUtils.isEmpty(linkContext.getLinkTitle())) {
            binding.title.setVisibility(View.GONE);
        } else {
            binding.title.setVisibility(View.VISIBLE);
            binding.title.setText(linkContext.getLinkTitle());
        }
        if (TextUtils.isEmpty(linkContext.getLinkSummary())) {
            binding.summary.setVisibility(View.GONE);
        } else {
            binding.summary.setVisibility(View.VISIBLE);
            binding.summary.setText(linkContext.getLinkSummary());
        }
        if (TextUtils.isEmpty(linkContext.getLinkUrl())) {
            binding.url.setVisibility(View.GONE);
        } else {
            binding.url.setVisibility(View.VISIBLE);
            binding.url.setText(linkContext.getLinkUrl());
        }
        binding.text.setText(link.getText());
    }
}
