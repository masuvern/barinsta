package awais.instagrabber.models;

import androidx.annotation.NonNull;

import java.util.Date;

import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.utils.Utils;

public final class NotificationModel {
    private final String id;
    private final long userId;
    private final String username;
    private final String profilePicUrl;
    private final long postId;
    private final String previewUrl;
    private final NotificationType type;
    private final CharSequence text;
    private final long timestamp;

    public NotificationModel(final String id,
                             final String text,
                             final long timestamp,
                             final long userId,
                             final String username,
                             final String profilePicUrl,
                             final long postId,
                             final String previewUrl,
                             final NotificationType type) {
        this.id = id;
        this.text = text;
        this.timestamp = timestamp;
        this.userId = userId;
        this.username = username;
        this.profilePicUrl = profilePicUrl;
        this.postId = postId;
        this.previewUrl = previewUrl;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public CharSequence getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getDateTime() {
        return Utils.datetimeParser.format(new Date(timestamp * 1000L));
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePic() {
        return profilePicUrl;
    }

    public long getPostId() {
        return postId;
    }

    public String getPreviewPic() {
        return previewUrl;
    }

    public NotificationType getType() { return type; }
}