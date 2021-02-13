package awais.instagrabber.adapters.viewholder.directmessages;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectMessageInboxAdapter.OnItemClickListener;
import awais.instagrabber.databinding.LayoutDmInboxItemBinding;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReelShare;
import awais.instagrabber.repositories.responses.directmessages.DirectItemVisualMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadDirectStory;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadLastSeenAt;
import awais.instagrabber.repositories.responses.directmessages.RavenExpiringMediaActionSummary;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;

public final class DirectInboxItemViewHolder extends RecyclerView.ViewHolder {
    // private static final String TAG = "DMInboxItemVH";
    private final LayoutDmInboxItemBinding binding;
    private final OnItemClickListener onClickListener;
    private final List<SimpleDraweeView> multipleProfilePics;
    private final int childSmallSize;
    private final int childTinySize;

    public DirectInboxItemViewHolder(@NonNull final LayoutDmInboxItemBinding binding,
                                     final OnItemClickListener onClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onClickListener = onClickListener;
        multipleProfilePics = ImmutableList.of(
                binding.multiPic1,
                binding.multiPic2,
                binding.multiPic3
        );
        childSmallSize = itemView.getResources().getDimensionPixelSize(R.dimen.dm_inbox_avatar_size_small);
        childTinySize = itemView.getResources().getDimensionPixelSize(R.dimen.dm_inbox_avatar_size_tiny);
    }

    public void bind(final DirectThread thread) {
        if (thread == null) return;
        if (onClickListener != null) {
            itemView.setOnClickListener((v) -> onClickListener.onItemClick(thread));
        }
        setProfilePics(thread);
        setTitle(thread);
        final List<DirectItem> items = thread.getItems();
        if (items == null || items.isEmpty()) return;
        final DirectItem item = thread.getFirstDirectItem();
        if (item == null) return;
        setDateTime(item);
        setSubtitle(thread);
        setReadState(thread);
    }

    private void setProfilePics(@NonNull final DirectThread thread) {
        final List<User> users = thread.getUsers();
        if (users.size() > 1) {
            binding.profilePic.setVisibility(View.GONE);
            binding.multiPicContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < Math.min(3, users.size()); ++i) {
                final User user = users.get(i);
                final SimpleDraweeView view = multipleProfilePics.get(i);
                view.setVisibility(user == null ? View.GONE : View.VISIBLE);
                if (user == null) return;
                final String profilePicUrl = user.getProfilePicUrl();
                view.setImageURI(profilePicUrl);
                setChildSize(view, users.size());
                if (i == 1) {
                    updateConstraints(view, users.size());
                }
                view.requestLayout();
            }
            return;
        }
        binding.profilePic.setVisibility(View.VISIBLE);
        binding.multiPicContainer.setVisibility(View.GONE);
        final String profilePicUrl = users.size() == 1 ? users.get(0).getProfilePicUrl() : null;
        if (profilePicUrl == null) {
            binding.profilePic.setController(null);
            return;
        }
        binding.profilePic.setImageURI(profilePicUrl);
    }

    private void updateConstraints(final SimpleDraweeView view, final int length) {
        final ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        if (length >= 2) {
            layoutParams.endToEnd = ConstraintSet.PARENT_ID;
            layoutParams.bottomToBottom = ConstraintSet.PARENT_ID;
        }
        if (length == 3) {
            layoutParams.startToStart = ConstraintSet.PARENT_ID;
            layoutParams.topToTop = ConstraintSet.PARENT_ID;
        }
    }

    private void setChildSize(final SimpleDraweeView view, final int length) {
        final int size = length == 3 ? childTinySize : childSmallSize;
        final ConstraintLayout.LayoutParams viewLayoutParams = new ConstraintLayout.LayoutParams(size, size);
        view.setLayoutParams(viewLayoutParams);
    }

    private void setTitle(@NonNull final DirectThread thread) {
        final String threadTitle = thread.getThreadTitle();
        binding.threadTitle.setText(threadTitle);
    }

    private void setSubtitle(@NonNull final DirectThread thread) {
        // If there is an unopened raven media, give it highest priority
        final DirectThreadDirectStory directStory = thread.getDirectStory();
        final long viewerId = thread.getViewerId();
        if (directStory != null && !directStory.getItems().isEmpty()) {
            final DirectItem item = directStory.getItems().get(0);
            final MediaItemType mediaType = item.getVisualMedia().getMedia().getMediaType();
            final String username = getUsername(thread.getUsers(), item.getUserId(), viewerId);
            final String subtitle = getMediaSpecificSubtitle(username, mediaType);
            binding.subtitle.setText(subtitle);
            return;
        }
        final DirectItem item = thread.getFirstDirectItem();
        if (item == null) return;
        final long senderId = item.getUserId();
        final DirectItemType itemType = item.getItemType();
        String subtitle = null;
        final String username = getUsername(thread.getUsers(), senderId, viewerId);
        String message = "";
        if (itemType == null) {
            message = "Unsupported message";
        } else {
            switch (itemType) {
                case TEXT:
                    message = item.getText();
                    break;
                case LIKE:
                    message = item.getLike();
                    break;
                case LINK:
                    message = item.getLink().getText();
                    break;
                case PLACEHOLDER:
                    message = item.getPlaceholder().getMessage();
                    break;
                case MEDIA_SHARE:
                    subtitle = String.format("%s shared a post", username != null ? username : "");
                    break;
                case ANIMATED_MEDIA:
                    subtitle = String.format("%s shared a gif", username != null ? username : "");
                    break;
                case PROFILE:
                    subtitle = String.format("%s shared a profile: @%s", username != null ? username : "", item.getProfile().getUsername());
                    break;
                case LOCATION:
                    subtitle = String.format("%s shared a location: %s", username != null ? username : "", item.getLocation().getName());
                    break;
                case MEDIA: {
                    final MediaItemType mediaType = item.getMedia().getMediaType();
                    subtitle = getMediaSpecificSubtitle(username, mediaType);
                    break;
                }
                case STORY_SHARE: {
                    final String reelType = item.getStoryShare().getReelType();
                    if (reelType == null) {
                        subtitle = item.getStoryShare().getTitle();
                    } else {
                        String format = "%s shared a story by @%s";
                        if (reelType.equals("highlight_reel")) {
                            format = "%s shared a story highlight by @%s";
                        }
                        subtitle = String.format(format, username != null ? username : "",
                                                 item.getStoryShare().getMedia().getUser().getUsername());
                    }
                    break;
                }
                case VOICE_MEDIA:
                    subtitle = String.format("%s sent a voice message", username != null ? username : "");
                    break;
                case ACTION_LOG:
                    subtitle = item.getActionLog().getDescription();
                    break;
                case VIDEO_CALL_EVENT:
                    subtitle = item.getVideoCallEvent().getDescription();
                    break;
                case CLIP:
                    subtitle = String.format("%s shared a clip by @%s", username != null ? username : "",
                                             item.getClip().getClip().getUser().getUsername());
                    break;
                case FELIX_SHARE:
                    subtitle = String.format("%s shared an IGTV video by @%s", username != null ? username : "",
                                             item.getFelixShare().getVideo().getUser().getUsername());
                    break;
                case RAVEN_MEDIA:
                    subtitle = getRavenMediaSubtitle(item, username);
                    break;
                case REEL_SHARE:
                    final DirectItemReelShare reelShare = item.getReelShare();
                    if (reelShare == null) {
                        subtitle = "";
                        break;
                    }
                    final String reelType = reelShare.getType();
                    switch (reelType) {
                        case "reply":
                            if (viewerId == item.getUserId()) {
                                subtitle = String.format("You replied to their story: %s", reelShare.getText());
                            } else {
                                subtitle = String.format("%s replied to your story: %s", username != null ? username : "", reelShare.getText());
                            }
                            break;
                        case "mention":
                            if (viewerId == item.getUserId()) {
                                // You mentioned the other person
                                final long mentionedUserId = item.getReelShare().getMentionedUserId();
                                final String otherUsername = getUsername(thread.getUsers(), mentionedUserId, viewerId);
                                subtitle = String.format("You mentioned @%s in your story", otherUsername);
                            } else {
                                // They mentioned you
                                subtitle = String.format("%s mentioned you in their story", username != null ? username : "");
                            }
                            break;
                        case "reaction":
                            if (viewerId == item.getUserId()) {
                                subtitle = String.format("You reacted to their story: %s", reelShare.getText());
                            } else {
                                subtitle = String.format("%s reacted to your story: %s", username != null ? username : "", reelShare.getText());
                            }
                            break;
                        default:
                            subtitle = "";
                            break;
                    }
                    break;
                default:
                    message = "Unsupported message";
            }
        }
        if (subtitle == null) {
            if (thread.getUsers().size() > 1
                    || (thread.getUsers().size() == 1 && senderId == viewerId)) {
                subtitle = String.format("%s: %s", username != null ? username : "", message);
            } else {
                subtitle = message;
            }
        }
        binding.subtitle.setText(subtitle != null ? subtitle : "");
    }

    private String getMediaSpecificSubtitle(final String username, final MediaItemType mediaType) {
        final String userSharedAnImage = String.format("%s shared an image", username != null ? username : "");
        final String userSharedAVideo = String.format("%s shared a video", username != null ? username : "");
        final String userSentAMessage = String.format("%s sent a message", username != null ? username : "");
        String subtitle;
        switch (mediaType) {
            case MEDIA_TYPE_IMAGE:
                subtitle = userSharedAnImage;
                break;
            case MEDIA_TYPE_VIDEO:
                subtitle = userSharedAVideo;
                break;
            default:
                subtitle = userSentAMessage;
                break;
        }
        return subtitle;
    }

    private String getRavenMediaSubtitle(final DirectItem item,
                                         final String username) {
        String subtitle = "↗ ";
        final DirectItemVisualMedia visualMedia = item.getVisualMedia();
        final RavenExpiringMediaActionSummary summary = visualMedia.getExpiringMediaActionSummary();
        if (summary != null) {
            final RavenExpiringMediaActionSummary.ActionType expiringMediaType = summary.getType();
            int textRes = 0;
            switch (expiringMediaType) {
                case DELIVERED:
                    textRes = R.string.dms_inbox_raven_media_delivered;
                    break;
                case SENT:
                    textRes = R.string.dms_inbox_raven_media_sent;
                    break;
                case OPENED:
                    textRes = R.string.dms_inbox_raven_media_opened;
                    break;
                case REPLAYED:
                    textRes = R.string.dms_inbox_raven_media_replayed;
                    break;
                case SENDING:
                    textRes = R.string.dms_inbox_raven_media_sending;
                    break;
                case BLOCKED:
                    textRes = R.string.dms_inbox_raven_media_blocked;
                    break;
                case SUGGESTED:
                    textRes = R.string.dms_inbox_raven_media_suggested;
                    break;
                case SCREENSHOT:
                    textRes = R.string.dms_inbox_raven_media_screenshot;
                    break;
                case CANNOT_DELIVER:
                    textRes = R.string.dms_inbox_raven_media_cant_deliver;
                    break;
            }
            if (textRes > 0) {
                subtitle += itemView.getContext().getString(textRes);
            }
            return subtitle;
        }
        final MediaItemType mediaType = visualMedia.getMedia().getMediaType();
        subtitle = getMediaSpecificSubtitle(username, mediaType);
        return subtitle;
    }

    private String getUsername(final List<User> users,
                               final long userId,
                               final long viewerId) {
        if (userId == viewerId) {
            return "You";
        }
        final Optional<User> senderOptional = users.stream()
                                                   .filter(Objects::nonNull)
                                                   .filter(user -> user.getPk() == userId)
                                                   .findFirst();
        return senderOptional.map(User::getUsername).orElse(null);
    }

    private void setDateTime(@NonNull final DirectItem item) {
        final long timestamp = item.getTimestamp() / 1000;
        final String dateTimeString = TextUtils.getRelativeDateTimeString(itemView.getContext(), timestamp);
        binding.tvDate.setText(dateTimeString);
    }

    private void setReadState(@NonNull final DirectThread thread) {
        final DirectItem item = thread.getItems().get(0);
        final Map<Long, DirectThreadLastSeenAt> lastSeenAtMap = thread.getLastSeenAt();
        final boolean read = ResponseBodyUtils.isRead(item, lastSeenAtMap, Collections.singletonList(thread.getViewerId()), thread.getDirectStory());
        binding.unread.setVisibility(read ? View.GONE : View.VISIBLE);
        binding.threadTitle.setTypeface(binding.threadTitle.getTypeface(), read ? Typeface.NORMAL : Typeface.BOLD);
        binding.subtitle.setTypeface(binding.subtitle.getTypeface(), read ? Typeface.NORMAL : Typeface.BOLD);
    }
}