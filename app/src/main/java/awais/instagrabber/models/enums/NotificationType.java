package awais.instagrabber.models.enums;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum NotificationType implements Serializable {
    // web
    LIKE("GraphLikeAggregatedStory"),
    FOLLOW("GraphFollowAggregatedStory"),
    COMMENT("GraphCommentMediaStory"),
    MENTION("GraphMentionStory"),
    TAGGED("GraphUserTaggedStory"),
    // app story_type
    COMMENT_LIKE("13"),
    TAGGED_COMMENT("14"),
    RESPONDED_STORY("213"),
    // efr
    REQUEST("REQUEST");

    private final String itemType;
    private static final Map<String, NotificationType> map = new HashMap<>();

    static {
        for (NotificationType type : NotificationType.values()) {
            map.put(type.itemType, type);
        }
    }

    NotificationType(final String itemType) {
        this.itemType = itemType;
    }

    public String getItemType() {
        return itemType;
    }

    public static NotificationType valueOfType(final String itemType) {
        return map.get(itemType);
    }
}