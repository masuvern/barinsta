package awais.instagrabber.models.enums;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum MediaItemType implements Serializable {
    MEDIA_TYPE_IMAGE(1),
    MEDIA_TYPE_VIDEO(2),
    MEDIA_TYPE_SLIDER(3),
    MEDIA_TYPE_VOICE(4);

    private final int id;
    private static Map<Integer, MediaItemType> map = new HashMap<>();

    static {
        for (MediaItemType type : MediaItemType.values()) {
            map.put(type.id, type);
        }
    }

    MediaItemType(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MediaItemType valueOf(final int id) {
        return map.get(id);
    }
}