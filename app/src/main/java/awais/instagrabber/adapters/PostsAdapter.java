package awais.instagrabber.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.PostViewHolder;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.MediaItemType;

public final class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
    private final ArrayList<PostModel> postModels;
    private final View.OnClickListener clickListener;
    private final View.OnLongClickListener longClickListener;
    private LayoutInflater layoutInflater;
    public boolean isSelecting = false;

    public PostsAdapter(final ArrayList<PostModel> postModels, final View.OnClickListener clickListener,
                        final View.OnLongClickListener longClickListener) {
        this.postModels = postModels;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(parent.getContext());
        return new PostViewHolder(layoutInflater.inflate(R.layout.item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final PostViewHolder holder, final int position) {
        final PostModel postModel = postModels.get(position);
        if (postModel != null) {
            postModel.setPosition(position);

            holder.itemView.setTag(postModel);

            holder.itemView.setOnClickListener(clickListener);
            holder.itemView.setOnLongClickListener(longClickListener);

            final MediaItemType itemType = postModel.getItemType();
            final boolean isSlider = itemType == MediaItemType.MEDIA_TYPE_SLIDER;

            holder.isDownloaded.setVisibility(postModel.isDownloaded() ? View.VISIBLE : View.GONE);

            holder.typeIcon.setVisibility(itemType == MediaItemType.MEDIA_TYPE_VIDEO || isSlider ? View.VISIBLE : View.GONE);
            holder.typeIcon.setImageResource(isSlider ? R.drawable.slider : R.drawable.video);

            holder.selectedView.setVisibility(postModel.isSelected() ? View.VISIBLE : View.GONE);
            holder.progressView.setVisibility(View.VISIBLE);

            final RequestManager glideRequestManager = Glide.with(holder.postImage);

            glideRequestManager.load(postModel.getThumbnailUrl()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                    holder.progressView.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                    holder.progressView.setVisibility(View.GONE);
                    glideRequestManager.load(postModel.getDisplayUrl()).into(holder.postImage);
                    return false;
                }
            }).into(holder.postImage);
        }
    }

    @Override
    public int getItemCount() {
        return postModels == null ? 0 : postModels.size();
    }
}
