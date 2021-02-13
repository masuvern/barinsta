package awais.instagrabber.repositories.responses.directmessages;

import com.google.gson.annotations.SerializedName;

public final class RavenExpiringMediaActionSummary {
    private final ActionType type;
    private final long timestamp;
    private final int count;

    public RavenExpiringMediaActionSummary(final long timestamp, final int count, final ActionType type) {
        this.timestamp = timestamp;
        this.count = count;
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCount() {
        return count;
    }

    public ActionType getType() {
        return type;
    }

    // thanks to http://github.com/warifp/InstagramAutoPostImageUrl/blob/master/vendor/mgp25/instagram-php/src/Response/Model/ActionBadge.php
    public enum ActionType {
        @SerializedName("raven_delivered")
        DELIVERED,
        @SerializedName("raven_sent")
        SENT,
        @SerializedName("raven_opened")
        OPENED,
        @SerializedName("raven_screenshot")
        SCREENSHOT,
        @SerializedName("raven_replayed")
        REPLAYED,
        @SerializedName("raven_cannot_deliver")
        CANNOT_DELIVER,
        @SerializedName("raven_sending")
        SENDING,
        @SerializedName("raven_blocked")
        BLOCKED,
        @SerializedName("raven_unknown")
        UNKNOWN,
        @SerializedName("raven_suggested")
        SUGGESTED,
    }
}
