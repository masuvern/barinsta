package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmProfileBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;

public class DirectMessageProfileViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmProfileBinding binding;

    public DirectMessageProfileViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                          @NonNull final LayoutDmProfileBinding binding,
                                          final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        binding.btnOpenProfile.setOnClickListener(onClickListener);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final ProfileModel profileModel = directItemModel.getProfileModel();
        if (profileModel == null) return;
        binding.profileInfo.setImageURI(profileModel.getSdProfilePic());
        binding.btnOpenProfile.setTag(profileModel);
        binding.tvFullName.setText(profileModel.getName());
        binding.profileInfoText.setText(profileModel.getUsername());
        binding.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
    }
}
