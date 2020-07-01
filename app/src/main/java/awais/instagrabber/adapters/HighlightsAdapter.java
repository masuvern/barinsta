package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.HighlightViewHolder;
import awais.instagrabber.models.HighlightModel;

public final class HighlightsAdapter extends RecyclerView.Adapter<HighlightViewHolder> {
    private final View.OnClickListener clickListener;
    private LayoutInflater layoutInflater;
    private HighlightModel[] highlightModels;

    public HighlightsAdapter(final HighlightModel[] highlightModels, final View.OnClickListener clickListener) {
        this.highlightModels = highlightModels;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public HighlightViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(parent.getContext());
        return new HighlightViewHolder(layoutInflater.inflate(R.layout.item_highlight, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final HighlightViewHolder holder, final int position) {
        final HighlightModel highlightModel = highlightModels[position];
        if (highlightModel != null) {
            holder.itemView.setTag(highlightModel);
            holder.itemView.setOnClickListener(clickListener);
            holder.title.setText(highlightModel.getTitle());
            Glide.with(holder.itemView).load(highlightModel.getThumbnailUrl()).into(holder.icon);
        }
    }

    public void setData(final HighlightModel[] highlightModels) {
        this.highlightModels = highlightModels;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return highlightModels == null ? 0 : highlightModels.length;
    }
}
