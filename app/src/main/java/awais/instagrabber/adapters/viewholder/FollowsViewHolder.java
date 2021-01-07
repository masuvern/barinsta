package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import awais.instagrabber.databinding.ItemFollowBinding;
import awais.instagrabber.models.FollowModel;
import awais.instagrabber.repositories.responses.User;

public final class FollowsViewHolder extends RecyclerView.ViewHolder {

    private final ItemFollowBinding binding;

    public FollowsViewHolder(final ItemFollowBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final User model,
                     final List<Long> admins,
                     final View.OnClickListener onClickListener) {
        if (model == null) return;
        itemView.setTag(model);
        itemView.setOnClickListener(onClickListener);
        binding.tvUsername.setText(model.getUsername());
        binding.tvFullName.setText(model.getFullName());
        if (admins != null && admins.contains(model.getPk())) {
            binding.isAdmin.setVisibility(View.VISIBLE);
        }
        binding.ivProfilePic.setImageURI(model.getProfilePicUrl());
    }

    public void bind(final FollowModel model,
                     final View.OnClickListener onClickListener) {
        if (model == null) return;
        itemView.setTag(model);
        itemView.setOnClickListener(onClickListener);
        binding.tvUsername.setText(model.getUsername());
        binding.tvFullName.setText(model.getFullName());
        binding.ivProfilePic.setImageURI(model.getProfilePicUrl());
    }
}