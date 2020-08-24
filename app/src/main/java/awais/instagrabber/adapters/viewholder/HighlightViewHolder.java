package awais.instagrabber.adapters.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.customviews.CircularImageView;

public final class HighlightViewHolder extends RecyclerView.ViewHolder {
    public final CircularImageView icon;
    public final TextView title;

    public HighlightViewHolder(@NonNull final View itemView) {
        super(itemView);
        icon = itemView.findViewById(R.id.icon);
        title = itemView.findViewById(R.id.title);
    }
}