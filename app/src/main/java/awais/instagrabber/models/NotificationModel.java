package awais.instagrabber.models;

import androidx.annotation.NonNull;

import java.util.Date;

import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.utils.Utils;

public final class NotificationModel {
    private final String id, username, profilePicUrl, shortcode, previewUrl;
    private final NotificationType type;
    private final CharSequence text;
    private final long timestamp;

    public NotificationModel(final String id, final String text, final long timestamp, final String username,
                             final String profilePicUrl, final String shortcode, final String previewUrl, final NotificationType type) {
        this.id = id;
        this.text = Utils.hasMentions(text) ? Utils.getMentionText(text) : text;
        this.timestamp = timestamp;
        this.username = username;
        this.profilePicUrl = profilePicUrl;
        this.shortcode = shortcode;
        this.previewUrl = previewUrl;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public CharSequence getText() {
        return text;
    }

    @NonNull
    public String getDateTime() {
        return Utils.datetimeParser.format(new Date(timestamp * 1000L));
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePic() {
        return profilePicUrl;
    }

    public String getShortcode() {
        return shortcode;
    }

    public String getPreviewPic() {
        return previewUrl;
    }

    public NotificationType getType() { return type; }
}