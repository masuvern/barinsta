package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemEmojiReaction {
    private final long senderId;
    private final long timestamp;
    private final String emoji;
    private final String superReactType;

    public DirectItemEmojiReaction(final long senderId, final long timestamp, final String emoji, final String superReactType) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.emoji = emoji;
        this.superReactType = superReactType;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getSuperReactType() {
        return superReactType;
    }
}
