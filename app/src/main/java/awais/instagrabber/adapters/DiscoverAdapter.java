package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.DiscoverViewHolder;
import awais.instagrabber.models.DiscoverItemModel;
import awais.instagrabber.models.enums.MediaItemType;

public final class DiscoverAdapter extends MultiSelectListAdapter<DiscoverItemModel, DiscoverViewHolder> {

    private static final DiffUtil.ItemCallback<DiscoverItemModel> diffCallback = new DiffUtil.ItemCallback<DiscoverItemModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final DiscoverItemModel oldItem, @NonNull final DiscoverItemModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final DiscoverItemModel oldItem, @NonNull final DiscoverItemModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }
    };

    public DiscoverAdapter(final OnItemClickListener<DiscoverItemModel> clickListener,
                           final OnItemLongClickListener<DiscoverItemModel> longClickListener) {
        super(diffCallback, clickListener, longClickListener);
    }

    @NonNull
    @Override
    public DiscoverViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new DiscoverViewHolder(layoutInflater.inflate(R.layout.item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final DiscoverViewHolder holder, final int position) {
        final DiscoverItemModel itemModel = getItem(position);
        if (itemModel != null) {
            itemModel.setPosition(position);
            holder.itemView.setTag(itemModel);
            holder.itemView.setOnClickListener(v -> getInternalOnItemClickListener().onItemClick(itemModel, position));
            holder.itemView.setOnLongClickListener(v -> getInternalOnLongItemClickListener().onItemLongClick(itemModel, position));
            final MediaItemType mediaType = itemModel.getItemType();
            holder.typeIcon.setVisibility(
                    mediaType == MediaItemType.MEDIA_TYPE_VIDEO || mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
            holder.typeIcon.setImageResource(mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? R.drawable.ic_slider_24 : R.drawable.ic_video_24);
            holder.selectedView.setVisibility(itemModel.isSelected() ? View.VISIBLE : View.GONE);
            holder.postImage.setImageURI(itemModel.getDisplayUrl());
        }
    }
}