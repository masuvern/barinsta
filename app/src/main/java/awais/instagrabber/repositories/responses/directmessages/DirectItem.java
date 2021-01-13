package awais.instagrabber.repositories.responses.directmessages;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;

import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;

public class DirectItem implements Cloneable {
    private final String itemId;
    private final long userId;
    private long timestamp;
    private final DirectItemType itemType;
    private final String text;
    private final String like;
    private final DirectItemLink link;
    private final String clientContext;
    private final DirectItemReelShare reelShare;
    private final DirectItemStoryShare storyShare;
    private final Media mediaShare;
    private final User profile;
    private final DirectItemPlaceholder placeholder;
    private final Media media;
    private final List<Media> previewMedias;
    private final DirectItemActionLog actionLog;
    private final DirectItemVideoCallEvent videoCallEvent;
    private final DirectItemClip clip;
    private final DirectItemFelixShare felixShare;
    private final DirectItemVisualMedia visualMedia;
    private final DirectItemAnimatedMedia animatedMedia;
    private DirectItemReactions reactions;
    private final DirectItem repliedToMessage;
    private final DirectItemVoiceMedia voiceMedia;
    private final Location location;
    private final int hideInThread;
    private Date date;
    private boolean isPending;

    public DirectItem(final String itemId,
                      final long userId,
                      final long timestamp,
                      final DirectItemType itemType,
                      final String text,
                      final String like,
                      final DirectItemLink link,
                      final String clientContext,
                      final DirectItemReelShare reelShare,
                      final DirectItemStoryShare storyShare,
                      final Media mediaShare,
                      final User profile,
                      final DirectItemPlaceholder placeholder,
                      final Media media,
                      final List<Media> previewMedias,
                      final DirectItemActionLog actionLog,
                      final DirectItemVideoCallEvent videoCallEvent,
                      final DirectItemClip clip,
                      final DirectItemFelixShare felixShare,
                      final DirectItemVisualMedia visualMedia,
                      final DirectItemAnimatedMedia animatedMedia,
                      final DirectItemReactions reactions,
                      final DirectItem repliedToMessage,
                      final DirectItemVoiceMedia voiceMedia,
                      final Location location,
                      final int hideInThread) {
        this.itemId = itemId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.itemType = itemType;
        this.text = text;
        this.like = like;
        this.link = link;
        this.clientContext = clientContext;
        this.reelShare = reelShare;
        this.storyShare = storyShare;
        this.mediaShare = mediaShare;
        this.profile = profile;
        this.placeholder = placeholder;
        this.media = media;
        this.previewMedias = previewMedias;
        this.actionLog = actionLog;
        this.videoCallEvent = videoCallEvent;
        this.clip = clip;
        this.felixShare = felixShare;
        this.visualMedia = visualMedia;
        this.animatedMedia = animatedMedia;
        this.reactions = reactions;
        this.repliedToMessage = repliedToMessage;
        this.voiceMedia = voiceMedia;
        this.location = location;
        this.hideInThread = hideInThread;
    }

    public String getItemId() {
        return itemId;
    }

    public long getUserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
        this.date = null;
    }

    public DirectItemType getItemType() {
        return itemType;
    }

    public String getText() {
        return text;
    }

    public DirectItemLink getLink() {
        return link;
    }

    public String getClientContext() {
        return clientContext;
    }

    public DirectItemReelShare getReelShare() {
        return reelShare;
    }

    public User getProfile() {
        return profile;
    }

    public DirectItemPlaceholder getPlaceholder() {
        return placeholder;
    }

    public Media getMediaShare() {
        return mediaShare;
    }

    public String getLike() {
        return like;
    }

    public Media getMedia() {
        return media;
    }

    public List<Media> getPreviewMedias() {
        return previewMedias;
    }

    public DirectItemStoryShare getStoryShare() {
        return storyShare;
    }

    public DirectItemActionLog getActionLog() {
        return actionLog;
    }

    public DirectItemVideoCallEvent getVideoCallEvent() {
        return videoCallEvent;
    }

    public DirectItemClip getClip() {
        return clip;
    }

    public DirectItemFelixShare getFelixShare() {
        return felixShare;
    }

    public DirectItemVisualMedia getVisualMedia() {
        return visualMedia;
    }

    public DirectItemAnimatedMedia getAnimatedMedia() {
        return animatedMedia;
    }

    public DirectItemReactions getReactions() {
        return reactions;
    }

    public DirectItem getRepliedToMessage() {
        return repliedToMessage;
    }

    public DirectItemVoiceMedia getVoiceMedia() {
        return voiceMedia;
    }

    public Location getLocation() {
        return location;
    }

    public int getHideInThread() {
        return hideInThread;
    }

    public Date getDate() {
        if (date == null) {
            date = new Date(timestamp / 1000);
        }
        return date;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(final boolean pending) {
        isPending = pending;
    }

    public void setReactions(final DirectItemReactions reactions) {
        this.reactions = reactions;
    }

    @NonNull
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // @Override
    // public boolean equals(final Object o) {
    //     if (this == o) return true;
    //     if (o == null || getClass() != o.getClass()) return false;
    //     final DirectItem that = (DirectItem) o;
    //     return userId == that.userId &&
    //             timestamp == that.timestamp &&
    //             isPending == that.isPending &&
    //             Objects.equals(itemId, that.itemId) &&
    //             itemType == that.itemType &&
    //             Objects.equals(clientContext, that.clientContext);
    // }
    //
    // @Override
    // public int hashCode() {
    //     return Objects.hash(itemId, userId, timestamp, itemType, clientContext, isPending);
    // }
}
