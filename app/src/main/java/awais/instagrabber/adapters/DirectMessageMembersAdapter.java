package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.FollowsViewHolder;
import awais.instagrabber.models.ProfileModel;

public final class DirectMessageMembersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final ProfileModel[] profileModels;
    private final View.OnClickListener onClickListener;
    private final LayoutInflater layoutInflater;

    public DirectMessageMembersAdapter(final ProfileModel[] profileModels, final Context context, final View.OnClickListener onClickListener) {
        this.profileModels = profileModels;
        this.layoutInflater = LayoutInflater.from(context);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = layoutInflater.inflate(R.layout.item_follow, parent, false);
        return new FollowsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final ProfileModel model = profileModels[position];

        final FollowsViewHolder followHolder = (FollowsViewHolder) holder;
        if (model != null) {
            followHolder.itemView.setTag(model);
            followHolder.itemView.setOnClickListener(onClickListener);

            followHolder.tvUsername.setText(model.getUsername());
            followHolder.tvFullName.setText(model.getName());

            Glide.with(layoutInflater.getContext()).load(model.getSdProfilePic()).into(followHolder.profileImage);
        }
    }

    @Override
    public int getItemCount() {
        return profileModels.length;
    }
}