package awais.instagrabber.repositories.responses.directmessages;

public class DirectThreadLastSeenAt {
    private final String timestamp;
    private final String itemId;

    public DirectThreadLastSeenAt(final String timestamp, final String itemId) {
        this.timestamp = timestamp;
        this.itemId = itemId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getItemId() {
        return itemId;
    }
}
