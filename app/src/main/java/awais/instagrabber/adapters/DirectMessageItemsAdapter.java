package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;

import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageActionLogViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageAnimatedMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageItemViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageLinkViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageMediaShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessagePlaceholderViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageProfileViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageRavenMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageReelShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageStoryShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageTextViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageVideoCallEventViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectMessageVoiceMediaViewHolder;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmLinkBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.databinding.LayoutDmProfileBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.databinding.LayoutDmVoiceMediaBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.DirectItemType;

public final class DirectMessageItemsAdapter extends ListAdapter<DirectItemModel, DirectMessageItemViewHolder> {
    private final List<ProfileModel> users;
    private final List<ProfileModel> leftUsers;
    private final View.OnClickListener onClickListener;
    private final MentionClickListener mentionClickListener;

    private static final DiffUtil.ItemCallback<DirectItemModel> diffCallback = new DiffUtil.ItemCallback<DirectItemModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final DirectItemModel oldItem, @NonNull final DirectItemModel newItem) {
            return oldItem.getItemId().equals(newItem.getItemId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final DirectItemModel oldItem, @NonNull final DirectItemModel newItem) {
            return oldItem.getItemId().equals(newItem.getItemId());
        }
    };

    public DirectMessageItemsAdapter(final List<ProfileModel> users,
                                     final List<ProfileModel> leftUsers,
                                     final View.OnClickListener onClickListener,
                                     final MentionClickListener mentionClickListener) {
        super(diffCallback);
        this.users = users;
        this.leftUsers = leftUsers;
        this.onClickListener = onClickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public DirectMessageItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final DirectItemType directItemType = DirectItemType.valueOf(type);
        final LayoutDmBaseBinding baseBinding = LayoutDmBaseBinding.inflate(layoutInflater, parent, false);
        final ViewGroup itemViewParent = baseBinding.messageCard;
        switch (directItemType) {
            default:
            case LIKE:
            case TEXT: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageTextViewHolder(baseBinding, binding, onClickListener, mentionClickListener);
            }
            case PLACEHOLDER: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessagePlaceholderViewHolder(baseBinding, binding, onClickListener);
            }
            case ACTION_LOG: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageActionLogViewHolder(baseBinding, binding, onClickListener);
            }
            case LINK: {
                final LayoutDmLinkBinding binding = LayoutDmLinkBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageLinkViewHolder(baseBinding, binding, onClickListener);
            }
            case MEDIA: {
                final LayoutDmMediaBinding binding = LayoutDmMediaBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageMediaViewHolder(baseBinding, binding, onClickListener);
            }
            case PROFILE: {
                final LayoutDmProfileBinding binding = LayoutDmProfileBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageProfileViewHolder(baseBinding, binding, onClickListener);
            }
            case REEL_SHARE: {
                final LayoutDmRavenMediaBinding binding = LayoutDmRavenMediaBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageReelShareViewHolder(baseBinding, binding, onClickListener);
            }
            case MEDIA_SHARE: {
                final LayoutDmMediaShareBinding binding = LayoutDmMediaShareBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageMediaShareViewHolder(baseBinding, binding, onClickListener);
            }
            case RAVEN_MEDIA: {
                final LayoutDmRavenMediaBinding binding = LayoutDmRavenMediaBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageRavenMediaViewHolder(baseBinding, binding, onClickListener);
            }
            case STORY_SHARE: {
                final LayoutDmStoryShareBinding binding = LayoutDmStoryShareBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageStoryShareViewHolder(baseBinding, binding, onClickListener);
            }
            case VOICE_MEDIA: {
                final LayoutDmVoiceMediaBinding binding = LayoutDmVoiceMediaBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageVoiceMediaViewHolder(baseBinding, binding, onClickListener);
            }
            case ANIMATED_MEDIA: {
                final LayoutDmAnimatedMediaBinding binding = LayoutDmAnimatedMediaBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageAnimatedMediaViewHolder(baseBinding, binding, onClickListener);
            }
            case VIDEO_CALL_EVENT: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, itemViewParent, false);
                return new DirectMessageVideoCallEventViewHolder(baseBinding, binding, onClickListener);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final DirectMessageItemViewHolder holder, final int position) {
        final DirectItemModel directItemModel = getItem(position);
        holder.bind(directItemModel, users, leftUsers);
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position).getItemType().getId();
    }
}