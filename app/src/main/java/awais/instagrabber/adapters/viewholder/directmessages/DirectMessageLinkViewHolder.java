package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmLinkBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.utils.Utils;

public class DirectMessageLinkViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmLinkBinding binding;

    public DirectMessageLinkViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                       @NonNull final LayoutDmLinkBinding binding,
                                       final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemLinkModel link = directItemModel.getLinkModel();
        final DirectItemModel.DirectItemLinkContext linkContext = link.getLinkContext();
        final String linkImageUrl = linkContext.getLinkImageUrl();
        if (Utils.isEmpty(linkImageUrl)) {
            binding.ivLinkPreview.setVisibility(View.GONE);
        } else {
            getGlideRequestManager().load(linkImageUrl).into(binding.ivLinkPreview);
        }
        if (Utils.isEmpty(linkContext.getLinkTitle())) {
            binding.tvLinkTitle.setVisibility(View.GONE);
        } else {
            binding.tvLinkTitle.setText(linkContext.getLinkTitle());
        }
        if (Utils.isEmpty(linkContext.getLinkSummary())) {
            binding.tvLinkSummary.setVisibility(View.GONE);
        } else {
            binding.tvLinkSummary.setText(linkContext.getLinkSummary());
        }
        binding.tvMessage.setText(Utils.getSpannableUrl(link.getText()));
    }
}
