package awais.instagrabber.adapters.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;

public final class PostViewHolder extends RecyclerView.ViewHolder {
    public final ImageView postImage, typeIcon;
    public final View selectedView, progressView, isDownloaded;

    public PostViewHolder(@NonNull final View itemView) {
        super(itemView);
        typeIcon = itemView.findViewById(R.id.typeIcon);
        postImage = itemView.findViewById(R.id.postImage);
        isDownloaded = itemView.findViewById(R.id.isDownloaded);
        selectedView = itemView.findViewById(R.id.selectedView);
        progressView = itemView.findViewById(R.id.progressView);
    }
}