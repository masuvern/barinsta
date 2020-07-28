package awais.instagrabber.models;

import java.io.Serializable;

public final class LocationModel implements Serializable {
    private final long postCount;
    private final String id, name, bio, url, sdProfilePic, lat, lng;

    public LocationModel(final String id, final String name, final String bio, final String url,
                         final String sdProfilePic, final long postCount, final String lat, final String lng) {
        this.id = id; // <- id + "/" + slug
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

    public String getName() {
        return name;
    }

    public String getBio() {
        return bio;
    }

    public String getUrl() {
        return url;
    }

    public String getGeo() { return "geo:"+lat+","+lng+"?z=17&q="+lat+","+lng+"("+name+")"; }

    public String getSdProfilePic() {
        return sdProfilePic;
    }

    public long getPostCount() { return postCount; }
}