package awais.instagrabber.repositories.responses.directmessages;

import awais.instagrabber.repositories.responses.Media;

public class DirectItemReelShare {
    private final String text;
    private final String type;
    private final long reelOwnerId;
    private final long mentionedUserId;
    private final boolean isReelPersisted;
    private final String reelType;
    private final Media media;
    private final DirectItemReelShareReactionInfo reactionInfo;

    public DirectItemReelShare(final String text,
                               final String type,
                               final long reelOwnerId,
                               final long mentionedUserId,
                               final boolean isReelPersisted,
                               final String reelType,
                               final Media media,
                               final DirectItemReelShareReactionInfo reactionInfo) {
        this.text = text;
        this.type = type;
        this.reelOwnerId = reelOwnerId;
        this.mentionedUserId = mentionedUserId;
        this.isReelPersisted = isReelPersisted;
        this.reelType = reelType;
        this.media = media;
        this.reactionInfo = reactionInfo;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public long getReelOwnerId() {
        return reelOwnerId;
    }

    public boolean isReelPersisted() {
        return isReelPersisted;
    }

    public String getReelType() {
        return reelType;
    }

    public Media getMedia() {
        return media;
    }

    public DirectItemReelShareReactionInfo getReactionInfo() {
        return reactionInfo;
    }

    public long getMentionedUserId() {
        return mentionedUserId;
    }
}
