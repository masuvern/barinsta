package awais.instagrabber.models;

import java.io.Serializable;

public final class LocationModel implements Serializable {
    private final long postCount;
    private final String id;
    private final String slug;
    private final String name;
    private final String bio;
    private final String url;
    private final String sdProfilePic;
    private final String lat;
    private final String lng;

    public LocationModel(final String id,
                         final String slug,
                         final String name,
                         final String bio,
                         final String url,
                         final String sdProfilePic,
                         final long postCount,
                         final String lat,
                         final String lng) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.bio = bio;
        this.url = url;
        this.sdProfilePic = sdProfilePic;
        this.postCount = postCount;
        this.lat = lat;
        this.lng = lng;
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getBio() {
        return bio;
    }

    public String getUrl() {
        return url;
    }

    public String getGeo() { return "geo:" + lat + "," + lng + "?z=17&q=" + lat + "," + lng + "(" + name + ")"; }

    public String getSdProfilePic() {
        return sdProfilePic;
    }

    public Long getPostCount() { return postCount; }
}