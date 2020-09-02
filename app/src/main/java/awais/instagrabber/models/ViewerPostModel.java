package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public final class ViewerPostModel extends BasePostModel {
    protected final ProfileModel profileModel;
    protected final String locationName;
    protected final String location;
    protected final long videoViews;
    protected String sliderDisplayUrl;
    protected long commentsCount, likes;
    private boolean isCurrentSlide = false;

    public ViewerPostModel(final MediaItemType itemType,
                           final String postId,
                           final String displayUrl,
                           final String shortCode,
                           final String postCaption,
                           final ProfileModel profileModel,
                           final long videoViews,
                           final long timestamp,
                           boolean liked,
                           boolean bookmarked,
                           long likes,
                           final String locationName,
                           final String location) {
        this.itemType = itemType;
        this.postId = postId;
        this.displayUrl = displayUrl;
        this.postCaption = postCaption;
        this.profileModel = profileModel;
        this.shortCode = shortCode;
        this.videoViews = videoViews;
        this.timestamp = timestamp;
        this.liked = liked;
        this.likes = likes;
        this.bookmarked = bookmarked;
        this.locationName = locationName;
        this.location = location;
    }

    public static ViewerPostModel getDefaultModel(final int postId, final String shortCode) {
        return new ViewerPostModel(null, String.valueOf(postId), null, "", null, null, -1, -1, false, false, -1, null, null);
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public String getSliderDisplayUrl() {
        return sliderDisplayUrl;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocation() {
        return location;
    }

    public final long getVideoViews() {
        return videoViews;
    }

    public long getLikes() {
        return likes;
    }

    // setManualLike means user liked from InstaGrabber
    public boolean setManualLike(final boolean like) {
        liked = like;
        likes = (like) ? (likes + 1) : (likes - 1);
        return liked;
    }

    public void setSliderDisplayUrl(final String sliderDisplayUrl) {
        this.sliderDisplayUrl = sliderDisplayUrl;
    }

    public void setCommentsCount(final long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public void setCurrentSlide(final boolean currentSlide) {
        this.isCurrentSlide = currentSlide;
    }

    public boolean isCurrentSlide() {
        return isCurrentSlide;
    }
}