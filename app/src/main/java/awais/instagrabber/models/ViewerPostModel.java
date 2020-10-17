package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public final class ViewerPostModel extends BasePostModel {
    protected final ProfileModel profileModel;
    protected final String locationName;
    protected final String location;
    protected final long videoViews;
    private final String thumbnailUrl;
    protected long commentsCount;
    protected long likes;
    private int imageWidth;
    private int imageHeight;
    private boolean isCurrentSlide = false;

    public static class Builder {
        private MediaItemType itemType;
        private String postId;
        private String displayUrl;
        private String shortCode;
        private String postCaption;
        private ProfileModel profileModel;
        private long videoViews;
        private long timestamp;
        private boolean liked;
        private boolean bookmarked;
        private long likes;
        private String locationName;
        private String location;
        private String thumbnailUrl;
        private int imageWidth;
        private int imageHeight;

        public Builder setItemType(final MediaItemType itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder setPostId(final String postId) {
            this.postId = postId;
            return this;
        }

        public Builder setDisplayUrl(final String displayUrl) {
            this.displayUrl = displayUrl;
            return this;
        }

        public Builder setShortCode(final String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        // public Builder setPostCaption(final String postCaption) {
        //     this.postCaption = postCaption;
        //     return this;
        // }

        // public Builder setProfileModel(final ProfileModel profileModel) {
        //     this.profileModel = profileModel;
        //     return this;
        // }

        public Builder setVideoViews(final long videoViews) {
            this.videoViews = videoViews;
            return this;
        }

        // public Builder setTimestamp(final long timestamp) {
        //     this.timestamp = timestamp;
        //     return this;
        // }
        //
        // public Builder setLiked(final boolean liked) {
        //     this.liked = liked;
        //     return this;
        // }
        //
        // public Builder setBookmarked(final boolean bookmarked) {
        //     this.bookmarked = bookmarked;
        //     return this;
        // }
        //
        // public Builder setLikes(final long likes) {
        //     this.likes = likes;
        //     return this;
        // }

        // public Builder setLocationName(final String locationName) {
        //     this.locationName = locationName;
        //     return this;
        // }
        //
        // public Builder setLocation(final String location) {
        //     this.location = location;
        //     return this;
        // }


        public Builder setImageHeight(final int imageHeight) {
            this.imageHeight = imageHeight;
            return this;
        }

        public Builder setImageWidth(final int imageWidth) {
            this.imageWidth = imageWidth;
            return this;
        }

        public Builder setThumbnailUrl(final String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public ViewerPostModel build() {
            return new ViewerPostModel(itemType, postId, displayUrl, thumbnailUrl, imageHeight, imageWidth, shortCode, postCaption, profileModel,
                                       videoViews, timestamp, liked, bookmarked, likes, locationName, location);
        }
    }

    public ViewerPostModel(final MediaItemType itemType,
                           final String postId,
                           final String displayUrl,
                           final String thumbnailUrl,
                           final int imageHeight,
                           final int imageWidth,
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
        this.thumbnailUrl = thumbnailUrl;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
        this.postCaption = postCaption;
        this.profileModel = profileModel;
        this.shortCode = shortCode;
        this.videoViews = videoViews;
        this.timestamp = timestamp;
        this.liked = liked;
        this.likes = likes;
        this.saved = bookmarked;
        this.locationName = locationName;
        this.location = location;
    }

    public long getCommentsCount() {
        return commentsCount;
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
    public void setManualLike(final boolean like) {
        liked = like;
        likes = (like) ? (likes + 1) : (likes - 1);
    }

    public boolean isCurrentSlide() {
        return isCurrentSlide;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    @Override
    public String toString() {
        return "ViewerPostModel{" +
                "type=" + itemType +
                ", displayUrl=" + displayUrl +
                ", thumbnailUrl=" + thumbnailUrl +
                ", locationName='" + locationName + '\'' +
                ", location='" + location + '\'' +
                ", videoViews=" + videoViews +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", commentsCount=" + commentsCount +
                ", likes=" + likes +
                ", imageWidth=" + imageWidth +
                ", imageHeight=" + imageHeight +
                ", isCurrentSlide=" + isCurrentSlide +
                '}';
    }
}