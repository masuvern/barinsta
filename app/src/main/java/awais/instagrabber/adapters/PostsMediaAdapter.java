package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.PostMediaViewHolder;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.ViewerPostModel;

public final class PostsMediaAdapter extends RecyclerView.Adapter<PostMediaViewHolder> {
    private final View.OnClickListener clickListener;
    private LayoutInflater layoutInflater;
    private ViewerPostModel[] postModels;

    public PostsMediaAdapter(final ViewerPostModel[] postModels, final View.OnClickListener clickListener) {
        this.postModels = postModels;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PostMediaViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(parent.getContext());
        return new PostMediaViewHolder(layoutInflater.inflate(R.layout.item_child_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final PostMediaViewHolder holder, final int position) {
        final ViewerPostModel postModel = postModels[position];
        if (postModel != null) {
            postModel.setPosition(position);

            holder.itemView.setTag(postModel);
            holder.itemView.setOnClickListener(clickListener);

            holder.selectedView.setVisibility(postModel.isCurrentSlide() ? View.VISIBLE : View.GONE);

            holder.isDownloaded.setVisibility(postModel.isDownloaded() ? View.VISIBLE : View.GONE);

            Glide.with(layoutInflater.getContext()).load(postModel.getSliderDisplayUrl()).into(holder.icon);
        }
    }

    public void setData(final ViewerPostModel[] postModels) {
        this.postModels = postModels;
        notifyDataSetChanged();
    }

    public ViewerPostModel getItemAt(final int position) {
        return postModels == null ? null : postModels[position];
    }

    @Override
    public int getItemCount() {
        return postModels == null ? 0 : postModels.length;
    }

    public BasePostModel[] getPostModels() {
        return postModels;
    }
}