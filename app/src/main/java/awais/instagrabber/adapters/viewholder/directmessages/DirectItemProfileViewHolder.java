package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.view.View;

import androidx.annotation.NonNull;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmProfileBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemLocation;
import awais.instagrabber.repositories.responses.directmessages.DirectItemMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectUser;
import awais.instagrabber.repositories.responses.directmessages.ImageVersions2;
import awais.instagrabber.utils.ResponseBodyUtils;

public class DirectItemProfileViewHolder extends DirectItemViewHolder {

    private final LayoutDmProfileBinding binding;
    private final ImmutableList<SimpleDraweeView> previewViews;

    public DirectItemProfileViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                       @NonNull final LayoutDmProfileBinding binding,
                                       final ProfileModel currentUser,
                                       final DirectThread thread,
                                       final MentionClickListener mentionClickListener,
                                       final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
        previewViews = ImmutableList.of(
                binding.preview1,
                binding.preview2,
                binding.preview3,
                binding.preview4,
                binding.preview5,
                binding.preview6
        );
        final Resources resources = itemView.getResources();
        binding.preview4.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                                              .setRoundingParams(RoundingParams.fromCornersRadii(0, 0, 0, dmRadius))
                                              .build());
        binding.preview6.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                                              .setRoundingParams(RoundingParams.fromCornersRadii(0, 0, dmRadius, 0))
                                              .build());
    }

    @Override
    public void bindItem(@NonNull final DirectItem item,
                         final MessageDirection messageDirection) {
        removeBg();
        binding.getRoot().setBackgroundResource(messageDirection == MessageDirection.INCOMING
                                                ? R.drawable.bg_speech_bubble_incoming
                                                : R.drawable.bg_speech_bubble_outgoing);
        if (item.getItemType() == DirectItemType.PROFILE) {
            setProfile(item);
        } else if (item.getItemType() == DirectItemType.LOCATION) {
            setLocation(item);
        } else {
            return;
        }
        for (final SimpleDraweeView previewView : previewViews) {
            previewView.setImageURI((String) null);
        }
        final List<DirectItemMedia> previewMedias = item.getPreviewMedias();
        if (previewMedias.size() <= 0) {
            binding.firstRow.setVisibility(View.GONE);
            binding.secondRow.setVisibility(View.GONE);
            return;
        }
        if (previewMedias.size() <= 3) {
            binding.firstRow.setVisibility(View.VISIBLE);
            binding.secondRow.setVisibility(View.GONE);
        }
        for (int i = 0; i < previewMedias.size(); i++) {
            final DirectItemMedia previewMedia = previewMedias.get(i);
            if (previewMedia == null) continue;
            final ImageVersions2 imageVersions2 = previewMedia.getImageVersions2();
            final String url = ResponseBodyUtils.getThumbUrl(imageVersions2);
            if (url == null) continue;
            previewViews.get(i).setImageURI(url);
        }
    }

    private void setProfile(@NonNull final DirectItem item) {
        final DirectUser profile = item.getProfile();
        if (profile == null) return;
        binding.profilePic.setImageURI(profile.getProfilePicUrl());
        binding.username.setText(profile.getUsername());
        binding.fullName.setText(profile.getFullName());
        binding.isVerified.setVisibility(profile.isVerified() ? View.VISIBLE : View.GONE);
    }

    private void setLocation(@NonNull final DirectItem item) {
        final DirectItemLocation location = item.getLocation();
        if (location == null) return;
        binding.profilePic.setVisibility(View.GONE);
        binding.username.setText(location.getName());
        binding.fullName.setText(location.getAddress());
        binding.isVerified.setVisibility(View.GONE);
    }
}
