package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import awais.instagrabber.adapters.viewholder.FollowsViewHolder;
import awais.instagrabber.databinding.ItemFollowBinding;
import awais.instagrabber.models.ProfileModel;

public final class DirectMessageMembersAdapter extends RecyclerView.Adapter<FollowsViewHolder> {
    private final List<ProfileModel> profileModels;
    private final List<Long> admins;
    private final View.OnClickListener onClickListener;

    public DirectMessageMembersAdapter(final List<ProfileModel> profileModels,
                                       final List<Long> admins,
                                       final View.OnClickListener onClickListener) {
        this.profileModels = profileModels;
        this.admins = admins;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public FollowsViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemFollowBinding binding = ItemFollowBinding.inflate(layoutInflater, parent, false);
        return new FollowsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final FollowsViewHolder holder, final int position) {
        final ProfileModel model = profileModels.get(position);
        holder.bind(model, admins, onClickListener);
    }

    @Override
    public int getItemCount() {
        return profileModels.size();
    }
}