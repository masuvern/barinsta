package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter.DirectItemCallback;
import awais.instagrabber.customviews.RamboTextViewV2;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReactions;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.DeepLinkParser;
import awais.instagrabber.utils.ResponseBodyUtils;

public abstract class DirectItemViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = DirectItemViewHolder.class.getSimpleName();

    private final LayoutDmBaseBinding binding;
    private final User currentUser;
    private final DirectThread thread;
    private final int groupMessageWidth;
    private final List<Long> userIds;
    private final DirectItemCallback callback;

    protected final int margin;
    protected final int dmRadius;
    protected final int dmRadiusSmall;
    protected final int messageInfoPaddingSmall;
    protected final int mediaImageMaxHeight;
    protected final int windowWidth;
    protected final int mediaImageMaxWidth;

    public DirectItemViewHolder(@NonNull final LayoutDmBaseBinding binding,
                                @NonNull final User currentUser,
                                @NonNull final DirectThread thread,
                                @NonNull final DirectItemCallback callback) {
        super(binding.getRoot());
        this.binding = binding;
        this.currentUser = currentUser;
        this.thread = thread;
        this.callback = callback;
        userIds = thread.getUsers()
                        .stream()
                        .map(User::getPk)
                        .collect(Collectors.toList());
        binding.ivProfilePic.setVisibility(thread.isGroup() ? View.VISIBLE : View.GONE);
        binding.ivProfilePic.setOnClickListener(null);
        final Resources resources = itemView.getResources();
        margin = resources.getDimensionPixelSize(R.dimen.dm_message_item_margin);
        final int avatarSize = resources.getDimensionPixelSize(R.dimen.dm_message_item_avatar_size);
        dmRadius = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius);
        dmRadiusSmall = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius_small);
        messageInfoPaddingSmall = resources.getDimensionPixelSize(R.dimen.dm_message_info_padding_small);
        windowWidth = resources.getDisplayMetrics().widthPixels;
        mediaImageMaxHeight = resources.getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        final int groupWidthCorrection = avatarSize + messageInfoPaddingSmall * 3;
        mediaImageMaxWidth = windowWidth - margin - (thread.isGroup() ? groupWidthCorrection : 0);
        // messageInfoPaddingSmall is used cuz it's also 4dp, 1 avatar margin + 2 paddings = 3
        groupMessageWidth = windowWidth - margin - groupWidthCorrection;
    }

    public void bind(final DirectItem item) {
        final MessageDirection messageDirection = isSelf(item) ? MessageDirection.OUTGOING : MessageDirection.INCOMING;
        itemView.post(() -> bindBase(item, messageDirection));
        itemView.post(() -> bindItem(item, messageDirection));
    }

    private void bindBase(final DirectItem item, final MessageDirection messageDirection) {
        final FrameLayout.LayoutParams containerLayoutParams = (FrameLayout.LayoutParams) binding.container.getLayoutParams();
        final DirectItemType itemType = item.getItemType();
        setMessageDirectionGravity(messageDirection, containerLayoutParams);
        setGroupUserDetails(item, messageDirection);
        setBackground(messageDirection);
        setMessageInfo(item, messageDirection);
        if (itemType == DirectItemType.REEL_SHARE) {
            containerLayoutParams.setMarginStart(0);
            containerLayoutParams.setMarginEnd(0);
        }
        if (itemType == DirectItemType.TEXT || itemType == DirectItemType.LINK) {
            binding.messageInfo.setPadding(0, 0, dmRadius, dmRadiusSmall);
        } else {
            binding.messageInfo.setPadding(0, 0, messageInfoPaddingSmall, dmRadiusSmall);
        }
        setupReply(item, messageDirection);
        setReactions(item, thread.getUsers());
    }

    private void setBackground(final MessageDirection messageDirection) {
        if (showBackground()) {
            binding.background.setBackgroundResource(messageDirection == MessageDirection.INCOMING ? R.drawable.bg_speech_bubble_incoming
                                                                                                   : R.drawable.bg_speech_bubble_outgoing);
            return;
        }
        binding.background.setBackgroundResource(0);
    }

    private void setGroupUserDetails(final DirectItem item, final MessageDirection messageDirection) {
        if (showUserDetailsInGroup()) {
            binding.ivProfilePic.setVisibility(messageDirection == MessageDirection.INCOMING && thread.isGroup() ? View.VISIBLE : View.GONE);
            binding.tvUsername.setVisibility(messageDirection == MessageDirection.INCOMING && thread.isGroup() ? View.VISIBLE : View.GONE);
            if (messageDirection == MessageDirection.INCOMING && thread.isGroup()) {
                final User user = getUser(item.getUserId(), thread.getUsers());
                if (user != null) {
                    binding.tvUsername.setText(user.getUsername());
                    binding.ivProfilePic.setImageURI(user.getProfilePicUrl());
                }
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) binding.chatMessageLayout.getLayoutParams();
                layoutParams.matchConstraintMaxWidth = groupMessageWidth;
                binding.chatMessageLayout.setLayoutParams(layoutParams);
            }
            return;
        }
        binding.ivProfilePic.setVisibility(View.GONE);
        binding.tvUsername.setVisibility(View.GONE);
    }

    private void setMessageDirectionGravity(final MessageDirection messageDirection,
                                            final FrameLayout.LayoutParams containerLayoutParams) {
        if (allowMessageDirectionGravity()) {
            containerLayoutParams.setMarginStart(messageDirection == MessageDirection.OUTGOING ? margin : 0);
            containerLayoutParams.setMarginEnd(messageDirection == MessageDirection.INCOMING ? margin : 0);
            containerLayoutParams.gravity = messageDirection == MessageDirection.INCOMING ? Gravity.START : Gravity.END;
            return;
        }
        containerLayoutParams.gravity = Gravity.CENTER;
    }

    private void setMessageInfo(final DirectItem item, final MessageDirection messageDirection) {
        if (showMessageInfo()) {
            binding.messageInfo.setVisibility(View.VISIBLE);
            binding.deliveryStatus.setVisibility(messageDirection == MessageDirection.OUTGOING ? View.VISIBLE : View.GONE);
            binding.messageTime.setText(DateFormat.getTimeFormat(itemView.getContext()).format(item.getDate()));
            if (messageDirection == MessageDirection.OUTGOING) {
                if (item.isPending()) {
                    binding.deliveryStatus.setImageResource(R.drawable.ic_check_24);
                } else {
                    final boolean read = ResponseBodyUtils.isRead(item, thread.getLastSeenAt(), userIds, null);
                    binding.deliveryStatus.setImageResource(R.drawable.ic_check_all_24);
                    ImageViewCompat.setImageTintList(
                            binding.deliveryStatus,
                            ColorStateList.valueOf(itemView.getResources().getColor(read ? R.color.blue_500 : R.color.grey_500))
                    );
                }
            }
            return;
        }
        binding.messageInfo.setVisibility(View.GONE);
    }

    private void setupReply(final DirectItem item, final MessageDirection messageDirection) {
        if (item.getRepliedToMessage() != null) {
            setReply(item, messageDirection, thread.getUsers());
        } else {
            binding.quoteLine.setVisibility(View.GONE);
            binding.replyContainer.setVisibility(View.GONE);
            binding.replyInfo.setVisibility(View.GONE);
        }
    }

    private void setReply(final DirectItem item,
                          final MessageDirection messageDirection,
                          final List<User> users) {
        final DirectItem replied = item.getRepliedToMessage();
        final DirectItemType itemType = replied.getItemType();
        String text = null;
        String url = null;
        switch (itemType) {
            case TEXT:
                text = replied.getText();
                break;
            case LINK:
                text = replied.getLink().getText();
                break;
            case PLACEHOLDER:
                text = replied.getPlaceholder().getMessage();
                break;
            case MEDIA:
                url = ResponseBodyUtils.getThumbUrl(replied.getMedia().getImageVersions2());
                break;
            case RAVEN_MEDIA:
                url = ResponseBodyUtils.getThumbUrl(replied.getVisualMedia().getMedia().getImageVersions2());
                break;
            case VOICE_MEDIA:
                text = "Voice message";
                break;
            case MEDIA_SHARE:
                Media mediaShare = replied.getMediaShare();
                if (mediaShare.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                    mediaShare = mediaShare.getCarouselMedia().get(0);
                }
                url = ResponseBodyUtils.getThumbUrl(mediaShare.getImageVersions2());
                break;
            case REEL_SHARE:
                text = replied.getReelShare().getText();
                break;
            // Below types cannot be replied to
            // case LIKE:
            //     text = "❤️";
            //     break;
            // case PROFILE:
            //     text = "@" + replied.getProfile().getUsername();
            //     break;
            // case CLIP:
            //     url = ResponseBodyUtils.getThumbUrl(replied.getClip().getClip().getImageVersions2());
            //     break;
            // case FELIX_SHARE:
            //     url = ResponseBodyUtils.getThumbUrl(replied.getFelixShare().getVideo().getImageVersions2());
            //     break;
            // case STORY_SHARE:
            //     final DirectItemMedia media = replied.getStoryShare().getMedia();
            //     if (media == null) break;
            //     url = ResponseBodyUtils.getThumbUrl(media.getImageVersions2());
            //     break;
        }
        if (text == null && url == null) {
            binding.quoteLine.setVisibility(View.GONE);
            binding.replyContainer.setVisibility(View.GONE);
            binding.replyInfo.setVisibility(View.GONE);
            return;
        }
        setReplyGravity(messageDirection);
        final String info = setReplyInfo(item, replied, users);
        binding.replyInfo.setVisibility(View.VISIBLE);
        binding.replyInfo.setText(info);
        binding.quoteLine.setVisibility(View.VISIBLE);
        binding.replyContainer.setVisibility(View.VISIBLE);
        if (url != null) {
            binding.replyText.setVisibility(View.GONE);
            binding.replyImage.setVisibility(View.VISIBLE);
            binding.replyImage.setImageURI(url);
            return;
        }
        binding.replyImage.setVisibility(View.GONE);
        final Drawable background = binding.replyText.getBackground().mutate();
        final Resources resources = itemView.getResources();
        background.setTint(replied.getUserId() != currentUser.getPk()
                           ? resources.getColor(R.color.grey_600)
                           : resources.getColor(R.color.deep_purple_400));
        binding.replyText.setBackgroundDrawable(background);
        binding.replyText.setVisibility(View.VISIBLE);
        binding.replyText.setText(text);
    }

    private String setReplyInfo(final DirectItem item,
                                final DirectItem replied,
                                final List<User> users) {
        final long repliedToUserId = replied.getUserId();
        if (repliedToUserId == item.getUserId() && item.getUserId() == currentUser.getPk()) {
            // User replied to own message
            return "You replied to yourself";
        }
        if (repliedToUserId == item.getUserId()) {
            // opposite user replied to their own message
            return "Replied to themself";
        }
        final User user = getUser(repliedToUserId, users);
        final String repliedToUsername = user != null ? user.getUsername() : "";
        if (item.getUserId() == currentUser.getPk()) {
            return !thread.isGroup() ? "You replied" : String.format("You replied to %s", repliedToUsername);
        }
        if (repliedToUserId == currentUser.getPk()) {
            return "Replied to you";
        }
        return String.format("Replied to %s", repliedToUsername);
    }

    private void setReplyGravity(final MessageDirection messageDirection) {
        final boolean isIncoming = messageDirection == MessageDirection.INCOMING;
        final ConstraintLayout.LayoutParams quoteLineLayoutParams = (ConstraintLayout.LayoutParams) binding.quoteLine.getLayoutParams();
        final ConstraintLayout.LayoutParams replyContainerLayoutParams = (ConstraintLayout.LayoutParams) binding.replyContainer.getLayoutParams();
        final ConstraintLayout.LayoutParams replyInfoLayoutParams = (ConstraintLayout.LayoutParams) binding.replyInfo.getLayoutParams();
        final int profilePicId = binding.ivProfilePic.getId();
        final int replyContainerId = binding.replyContainer.getId();
        final int quoteLineId = binding.quoteLine.getId();
        quoteLineLayoutParams.startToEnd = isIncoming ? profilePicId : replyContainerId;
        quoteLineLayoutParams.endToStart = isIncoming ? replyContainerId : ConstraintLayout.LayoutParams.UNSET;
        quoteLineLayoutParams.endToEnd = isIncoming ? ConstraintLayout.LayoutParams.UNSET : ConstraintLayout.LayoutParams.PARENT_ID;
        replyContainerLayoutParams.startToEnd = isIncoming ? quoteLineId : profilePicId;
        replyContainerLayoutParams.endToEnd = isIncoming ? ConstraintLayout.LayoutParams.PARENT_ID : ConstraintLayout.LayoutParams.UNSET;
        replyContainerLayoutParams.endToStart = isIncoming ? ConstraintLayout.LayoutParams.UNSET : quoteLineId;
        replyInfoLayoutParams.startToEnd = isIncoming ? quoteLineId : ConstraintLayout.LayoutParams.UNSET;
        replyInfoLayoutParams.endToStart = isIncoming ? ConstraintLayout.LayoutParams.UNSET : quoteLineId;
    }

    private void setReactions(final DirectItem item, final List<User> users) {
        final DirectItemReactions reactions = item.getReactions();
        final List<DirectItemEmojiReaction> emojis = reactions != null ? reactions.getEmojis() : null;
        if (emojis == null || emojis.isEmpty()) {
            binding.reactions.setVisibility(View.GONE);
            return;
        }
        binding.reactions.setVisibility(View.VISIBLE);
        final String emojisJoined = emojis.stream()
                                          .map(DirectItemEmojiReaction::getEmoji)
                                          .collect(Collectors.joining());
        final String text = String.format(Locale.ENGLISH, "%s %d", emojisJoined, emojis.size());
        binding.emojis.setText(text);
        // final List<DirectUser> reactedUsers = emojis.stream()
        //                                             .map(DirectItemEmojiReaction::getSenderId)
        //                                             .distinct()
        //                                             .map(userId -> getUser(userId, users))
        //                                             .collect(Collectors.toList());
        // for (final DirectUser user : reactedUsers) {
        //     if (user == null) continue;
        //     final ProfilePicView profilePicView = new ProfilePicView(itemView.getContext());
        //     profilePicView.setSize(ProfilePicView.Size.TINY);
        //     profilePicView.setImageURI(user.getProfilePicUrl());
        //     binding.reactions.addView(profilePicView);
        // }
    }

    protected boolean isSelf(final DirectItem directItem) {
        return directItem.getUserId() == currentUser.getPk();
    }

    public void setItemView(final View view) {
        this.binding.message.addView(view);
    }

    public abstract void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection);

    @Nullable
    protected User getUser(final long userId, final List<User> users) {
        if (userId == currentUser.getPk()) {
            return currentUser;
        }
        if (users == null) return null;
        for (final User user : users) {
            if (userId != user.getPk()) continue;
            return user;
        }
        return null;
    }

    protected boolean allowMessageDirectionGravity() {
        return true;
    }

    protected boolean showUserDetailsInGroup() {
        return true;
    }

    protected boolean showBackground() {
        return false;
    }

    protected boolean showMessageInfo() {
        return true;
    }

    public void cleanup() {}

    protected void setupRamboTextListeners(@NonNull final RamboTextViewV2 textView) {
        textView.addOnHashtagListener(autoLinkItem -> callback.onHashtagClick(autoLinkItem.getOriginalText().trim()));
        textView.addOnMentionClickListener(autoLinkItem -> openProfile(autoLinkItem.getOriginalText().trim()));
        textView.addOnEmailClickListener(autoLinkItem -> callback.onEmailClick(autoLinkItem.getOriginalText().trim()));
        textView.addOnURLClickListener(autoLinkItem -> openURL(autoLinkItem.getOriginalText().trim()));
    }

    protected void openProfile(final String username) {
        callback.onMentionClick(username);
    }

    protected void openLocation(final long locationId) {
        callback.onLocationClick(locationId);
    }

    protected void openURL(final String url) {
        callback.onURLClick(url);
    }

    protected void openMedia(final Media media) {
        callback.onMediaClick(media);
    }

    protected void handleDeepLink(final String deepLinkText) {
        if (deepLinkText == null) return;
        final DeepLinkParser.DeepLink deepLink = DeepLinkParser.parse(deepLinkText);
        if (deepLink == null) return;
        switch (deepLink.getType()) {
            case USER:
                callback.onMentionClick(deepLink.getValue());
                break;
        }
    }

    public enum MessageDirection {
        INCOMING,
        OUTGOING
    }
}
