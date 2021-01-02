package awais.instagrabber.models;

import java.io.Serializable;

public final class HashtagModel implements Serializable {
    private final boolean following;
    private final long postCount;
    private final String id;
    private final String name;
    private final String sdProfilePic;

    public HashtagModel(final String id, final String name, final String sdProfilePic, final long postCount, final boolean following) {
        this.id = id;
        this.name = name;
        this.sdProfilePic = sdProfilePic;
        this.postCount = postCount;
        this.following = following;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSdProfilePic() {
        return sdProfilePic;
    }

    public Long getPostCount() { return postCount; }

    public boolean getFollowing() { return following; }
}