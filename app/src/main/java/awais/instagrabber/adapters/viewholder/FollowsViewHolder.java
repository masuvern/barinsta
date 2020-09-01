package awais.instagrabber.adapters.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;

public final class FollowsViewHolder extends RecyclerView.ViewHolder {
    public final ImageView profileImage, isAdmin;
    public final TextView tvFullName, tvUsername;

    public FollowsViewHolder(@NonNull final View itemView) {
        super(itemView);
        profileImage = itemView.findViewById(R.id.ivProfilePic);
        tvFullName = itemView.findViewById(R.id.tvFullName);
        tvUsername = itemView.findViewById(R.id.tvUsername);
        isAdmin = itemView.findViewById(R.id.isAdmin);
    }
}