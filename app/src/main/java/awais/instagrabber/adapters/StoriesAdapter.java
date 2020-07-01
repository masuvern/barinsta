package awais.instagrabber.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import awais.instagrabber.R;
import awais.instagrabber.models.StoryModel;

public final class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.StoryViewHolder> {
    private final View.OnClickListener clickListener;
    private LayoutInflater layoutInflater;
    private StoryModel[] storyModels;
    private Resources resources;
    private int width, height;

    public StoriesAdapter(final StoryModel[] storyModels, final View.OnClickListener clickListener) {
        this.storyModels = storyModels;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final Context context = parent.getContext();
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(context);
        if (resources == null) resources = context.getResources();

        height = Math.round(resources.getDimension(R.dimen.story_item_height));
        width = Math.round(resources.getDimension(R.dimen.story_item_width));

        return new StoryViewHolder(layoutInflater.inflate(R.layout.item_story, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final StoryViewHolder holder, final int position) {
        final StoryModel storyModel = storyModels[position];
        if (storyModel != null) {
            storyModel.setPosition(position);

            holder.itemView.setTag(storyModel);
            holder.itemView.setOnClickListener(clickListener);

            holder.selectedView.setVisibility(storyModel.isCurrentSlide() ? View.VISIBLE : View.GONE);

            Glide.with(holder.itemView).load(storyModel.getStoryUrl())
                    .apply(new RequestOptions().override(width, height))
                    .into(holder.icon);
        }
    }

    public void setData(final StoryModel[] storyModels) {
        this.storyModels = storyModels;
        notifyDataSetChanged();
    }

    public StoryModel getItemAt(final int position) {
        return storyModels == null ? null : storyModels[position];
    }

    @Override
    public int getItemCount() {
        return storyModels == null ? 0 : storyModels.length;
    }

    public final static class StoryViewHolder extends RecyclerView.ViewHolder {
        public final ImageView icon, selectedView;

        public StoryViewHolder(@NonNull final View itemView) {
            super(itemView);
            selectedView = itemView.findViewById(R.id.selectedView);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}