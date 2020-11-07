package awais.instagrabber.models;

import java.util.List;

import awais.instagrabber.models.enums.MediaItemType;

public final class FeedModel extends PostModel {
    private final ProfileModel profileModel;
    private final long commentsCount;
    private long likesCount;
    private final long viewCount;
    private final List<PostChild> sliderItems;
    private final int imageWidth;
    private final int imageHeight;
    private final String locationName;
    private final String locationId;

    public static class Builder {

        private ProfileModel profileModel;
        private MediaItemType itemType;
        private long viewCount;
        private String postId;
        private String displayUrl;
        private String thumbnailUrl;
        private String shortCode;
        private String postCaption;
        private long commentsCount;
        private long timestamp;
        private boolean liked;
        private boolean bookmarked;
        private long likesCount;
        private String locationName;
        private String locationId;
        private List<PostChild> sliderItems;
        private int imageWidth;
        private int imageHeight;

        public Builder setProfileModel(final ProfileModel profileModel) {
            this.profileModel = profileModel;
            return this;
        }

        public Builder setItemType(final MediaItemType itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder setViewCount(final long viewCount) {
            this.viewCount = viewCount;
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

        public Builder setThumbnailUrl(final String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public Builder setShortCode(final String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder setPostCaption(final String postCaption) {
            this.postCaption = postCaption;
            return this;
        }

        public Builder setCommentsCount(final long commentsCount) {
            this.commentsCount = commentsCount;
            return this;
        }

        public Builder setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setLiked(final boolean liked) {
            this.liked = liked;
            return this;
        }

        public Builder setBookmarked(final boolean bookmarked) {
            this.bookmarked = bookmarked;
            return this;
        }

        public Builder setLikesCount(final long likesCount) {
            this.likesCount = likesCount;
            return this;
        }

        public Builder setLocationName(final String locationName) {
            this.locationName = locationName;
            return this;
        }

        public Builder setLocationId(final String locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder setSliderItems(final List<PostChild> sliderItems) {
            this.sliderItems = sliderItems;
            return this;
        }

        public Builder setImageHeight(final int imageHeight) {
            this.imageHeight = imageHeight;
            return this;
        }

        public Builder setImageWidth(final int imageWidth) {
            this.imageWidth = imageWidth;
            return this;
        }

        public FeedModel build() {
            return new FeedModel(profileModel, itemType, viewCount, postId, displayUrl, thumbnailUrl, shortCode, postCaption, commentsCount,
                                 timestamp, liked, bookmarked, likesCount, locationName, locationId, sliderItems, imageHeight, imageWidth);
        }
    }

    private FeedModel(final ProfileModel profileModel,
                      final MediaItemType itemType,
                      final long viewCount,
                      final String postId,
                      final String displayUrl,
                      final String thumbnailUrl,
                      final String shortCode,
                      final String postCaption,
                      final long commentsCount,
                      final long timestamp,
                      final boolean liked,
                      final boolean bookmarked,
                      final long likesCount,
                      final String locationName,
                      final String locationId,
                      final List<PostChild> sliderItems,
                      final int imageHeight,
                      final int imageWidth) {
        super(itemType, postId, displayUrl, thumbnailUrl, shortCode, postCaption, timestamp, liked, bookmarked);
        this.profileModel = profileModel;
        this.commentsCount = commentsCount;
        this.likesCount = likesCount;
        this.viewCount = viewCount;
        this.locationName = locationName;
        this.locationId = locationId;
        this.sliderItems = sliderItems;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public List<PostChild> getSliderItems() {
        return sliderItems;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLiked(final boolean liked) {
        this.liked = liked;
    }

    public void setLikesCount(final long count) {
        this.likesCount = count;
    }

    public void setSaved(final boolean saved) {
        this.saved = saved;
    }

    @Override
    public String toString() {
        return "FeedModel{" +
                "type=" + itemType +
                ", displayUrl=" + displayUrl +
                ", thumbnailUrl=" + thumbnailUrl +
                ", commentsCount=" + commentsCount +
                ", viewCount=" + viewCount +
                // ", sliderItems=" + sliderItems +
                ", imageWidth=" + imageWidth +
                ", imageHeight=" + imageHeight +
                ", locationName='" + locationName + '\'' +
                ", locationId='" + locationId + '\'' +
                '}';
    }
}