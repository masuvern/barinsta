package awais.instagrabber.adapters.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;

public final class PostMediaViewHolder extends RecyclerView.ViewHolder {
    public final ImageView icon, isDownloaded, selectedView;

    public PostMediaViewHolder(@NonNull final View itemView) {
        super(itemView);
        selectedView = itemView.findViewById(R.id.selectedView);
        isDownloaded = itemView.findViewById(R.id.isDownloaded);
        icon = itemView.findViewById(R.id.icon);
    }
}
