package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.databinding.ItemChildPostBinding;
import awais.instagrabber.models.ViewerPostModel;

public final class PostMediaViewHolder extends RecyclerView.ViewHolder {

    private final ItemChildPostBinding binding;

    public PostMediaViewHolder(@NonNull final ItemChildPostBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final ViewerPostModel model, final int position, final View.OnClickListener clickListener) {
        if (model == null) return;
        // model.setPosition(position);
        itemView.setTag(model);
        itemView.setOnClickListener(clickListener);
        binding.selectedView.setVisibility(model.isCurrentSlide() ? View.VISIBLE : View.GONE);
        binding.isDownloaded.setVisibility(model.isDownloaded() ? View.VISIBLE : View.GONE);
        binding.icon.setImageURI(model.getDisplayUrl());
    }
}
