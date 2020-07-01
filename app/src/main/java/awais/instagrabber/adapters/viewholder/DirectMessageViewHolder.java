package awais.instagrabber.adapters.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;

public final class DirectMessageViewHolder extends RecyclerView.ViewHolder {
    public final LinearLayout multipleProfilePicsContainer;
    public final ImageView[] multipleProfilePics;
    public final ImageView ivProfilePic, notTextType;
    public final TextView tvUsername, tvDate, tvMessage;

    public DirectMessageViewHolder(@NonNull final View itemView, final View.OnClickListener clickListener) {
        super(itemView);

        if (clickListener != null) itemView.setOnClickListener(clickListener);

        itemView.findViewById(R.id.tvLikes).setVisibility(View.GONE);

        tvDate = itemView.findViewById(R.id.tvDate);
        tvMessage = itemView.findViewById(R.id.tvComment);
        tvUsername = itemView.findViewById(R.id.tvUsername);
        notTextType = itemView.findViewById(R.id.notTextType);
        ivProfilePic = itemView.findViewById(R.id.ivProfilePic);

        multipleProfilePicsContainer = itemView.findViewById(R.id.container);
        final LinearLayout containerChild = (LinearLayout) multipleProfilePicsContainer.getChildAt(1);
        multipleProfilePics = new ImageView[]{
                (ImageView) multipleProfilePicsContainer.getChildAt(0),
                (ImageView) containerChild.getChildAt(0),
                (ImageView) containerChild.getChildAt(1)
        };

        tvDate.setSelected(true);
        tvUsername.setSelected(true);
    }
}