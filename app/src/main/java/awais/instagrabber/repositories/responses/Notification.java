package awais.instagrabber.repositories.responses;

import awais.instagrabber.models.enums.NotificationType;

public class Notification {
    private final NotificationArgs args;
    private final String storyType;
    private final String pk;

    public Notification(final NotificationArgs args,
                        final String storyType,
                        final String pk) {
        this.args = args;
        this.storyType = storyType;
        this.pk = pk;
    }

    public NotificationArgs getArgs() {
        return args;
    }

    public NotificationType getType() {
        return NotificationType.valueOfType(storyType);
    }

    public String getPk() {
        return pk;
    }
}
