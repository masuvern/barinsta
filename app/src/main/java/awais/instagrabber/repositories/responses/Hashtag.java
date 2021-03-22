package awais.instagrabber.repositories.responses;

import java.io.Serializable;

import awais.instagrabber.models.enums.FollowingType;

public final class Hashtag implements Serializable {
    private final FollowingType following; // 0 false 1 true
    private final long mediaCount;
    private final String id;
    private final String name;

    public Hashtag(final String id,
                   final String name,
                   final long mediaCount,
                   final FollowingType following) {
        this.id = id;
        this.name = name;
        this.mediaCount = mediaCount;
        this.following = following;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getMediaCount() {
        return mediaCount;
    }

    public FollowingType getFollowing() {
        return following;
    }
}