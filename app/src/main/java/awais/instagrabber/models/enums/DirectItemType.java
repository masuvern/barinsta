package awais.instagrabber.models.enums;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum DirectItemType implements Serializable {
    TEXT(1),
    LIKE(2),
    LINK(3),
    MEDIA(4),
    RAVEN_MEDIA(5),
    PROFILE(6),
    VIDEO_CALL_EVENT(7),
    ANIMATED_MEDIA(8),
    VOICE_MEDIA(9),
    MEDIA_SHARE(10),
    REEL_SHARE(11),
    ACTION_LOG(12),
    PLACEHOLDER(13),
    STORY_SHARE(14),
    CLIP(15),        // media_share but reel
    FELIX_SHARE(16); // media_share but igtv

    private final int id;
    private static Map<Integer, DirectItemType> map = new HashMap<>();

    static {
        for (DirectItemType type : DirectItemType.values()) {
            map.put(type.id, type);
        }
    }

    DirectItemType(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static DirectItemType valueOf(final int id) {
        return map.get(id);
    }
}