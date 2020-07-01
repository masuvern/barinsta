package awais.instagrabber.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.DiscoverViewHolder;
import awais.instagrabber.models.DiscoverItemModel;
import awais.instagrabber.models.enums.MediaItemType;

public final class DiscoverAdapter extends RecyclerView.Adapter<DiscoverViewHolder> {
    private final ArrayList<DiscoverItemModel> discoverItemModels;
    private final View.OnClickListener clickListener;
    private final View.OnLongClickListener longClickListener;
    private LayoutInflater layoutInflater;
    public boolean isSelecting = false;

    public DiscoverAdapter(final ArrayList<DiscoverItemModel> discoverItemModels, final View.OnClickListener clickListener,
                           final View.OnLongClickListener longClickListener) {
        this.discoverItemModels = discoverItemModels;
        this.longClickListener = longClickListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public DiscoverViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(parent.getContext());
        return new DiscoverViewHolder(layoutInflater.inflate(R.layout.item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final DiscoverViewHolder holder, final int position) {
        final DiscoverItemModel itemModel = discoverItemModels.get(position);
        if (itemModel != null) {
            itemModel.setPosition(position);
            holder.itemView.setTag(itemModel);

            holder.itemView.setOnClickListener(clickListener);
            holder.itemView.setOnLongClickListener(longClickListener);

            final MediaItemType mediaType = itemModel.getItemType();

            holder.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO || mediaType == MediaItemType.MEDIA_TYPE_SLIDER
                    ? View.VISIBLE : View.GONE);

            holder.typeIcon.setImageResource(mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? R.drawable.slider : R.drawable.video);

            holder.selectedView.setVisibility(itemModel.isSelected() ? View.VISIBLE : View.GONE);
            holder.progressView.setVisibility(View.VISIBLE);

            Glide.with(layoutInflater.getContext()).load(itemModel.getDisplayUrl()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                    holder.progressView.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                    holder.progressView.setVisibility(View.GONE);
                    return false;
                }
            }).into(holder.postImage);

        }
    }

    @Override
    public int getItemCount() {
        return discoverItemModels == null ? 0 : discoverItemModels.size();
    }
}