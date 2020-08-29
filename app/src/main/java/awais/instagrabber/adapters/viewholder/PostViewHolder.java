package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.MultiSelectListAdapter.OnItemClickListener;
import awais.instagrabber.adapters.MultiSelectListAdapter.OnItemLongClickListener;
import awais.instagrabber.databinding.ItemPostBinding;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.MediaItemType;

public final class PostViewHolder extends RecyclerView.ViewHolder {
    private final ItemPostBinding binding;

    public PostViewHolder(@NonNull final ItemPostBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final PostModel postModel,
                     final int position,
                     final OnItemClickListener<PostModel> clickListener,
                     final OnItemLongClickListener<PostModel> longClickListener) {
        if (postModel == null) return;
        postModel.setPosition(position);
        itemView.setOnClickListener(v -> clickListener.onItemClick(postModel, position));
        itemView.setOnLongClickListener(v -> longClickListener.onItemLongClick(postModel, position));

        final MediaItemType itemType = postModel.getItemType();
        final boolean isSlider = itemType == MediaItemType.MEDIA_TYPE_SLIDER;

        binding.isDownloaded.setVisibility(postModel.isDownloaded() ? View.VISIBLE : View.GONE);

        binding.typeIcon.setVisibility(itemType == MediaItemType.MEDIA_TYPE_VIDEO || isSlider ? View.VISIBLE : View.GONE);
        binding.typeIcon.setImageResource(isSlider ? R.drawable.slider : R.drawable.video);

        binding.selectedView.setVisibility(postModel.isSelected() ? View.VISIBLE : View.GONE);
        binding.postImage.setImageURI(postModel.getThumbnailUrl());
    }
}