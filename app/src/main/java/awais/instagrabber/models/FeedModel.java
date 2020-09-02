package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public final class FeedModel extends PostModel {
    private final ProfileModel profileModel;
    private final long commentsCount;
    private final long viewCount;
    private boolean captionExpanded = false;
    private boolean mentionClicked = false;
    private ViewerPostModel[] sliderItems;
    private int imageWidth;
    private int imageHeight;
    private String locationName;
    private String locationId;

    public FeedModel(final ProfileModel profileModel,
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
                     final long likes,
                     final String locationName,
                     final String locationId) {
        super(itemType, postId, displayUrl, thumbnailUrl, shortCode, postCaption, timestamp, liked, bookmarked, likes);
        this.profileModel = profileModel;
        this.commentsCount = commentsCount;
        this.viewCount = viewCount;
        this.locationName = locationName;
        this.locationId = locationId;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public ViewerPostModel[] getSliderItems() {
        return sliderItems;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public boolean isCaptionExpanded() {
        return captionExpanded;
    }

    public boolean isMentionClicked() {
        return !mentionClicked;
    }

    public void setMentionClicked(final boolean mentionClicked) {
        this.mentionClicked = mentionClicked;
    }

    public void setSliderItems(final ViewerPostModel[] sliderItems) {
        this.sliderItems = sliderItems;
        setItemType(MediaItemType.MEDIA_TYPE_SLIDER);
    }

    public void toggleCaption() {
        captionExpanded = !captionExpanded;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(final int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public void setImageHeight(final int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(final String locationName) {
        this.locationName = locationName;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(final String locationId) {
        this.locationId = locationId;
    }
}