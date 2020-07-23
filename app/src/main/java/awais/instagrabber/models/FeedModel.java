package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public final class FeedModel extends PostModel {
    private final ProfileModel profileModel;
    private final long commentsCount, viewCount;
    private boolean captionExpanded = false, mentionClicked = false;
    private ViewerPostModel[] sliderItems;

    public FeedModel(final ProfileModel profileModel, final MediaItemType itemType, final long viewCount, final String postId,
                     final String displayUrl, final String thumbnailUrl, final String shortCode, final String postCaption,
                     final long commentsCount, final long timestamp, boolean liked, boolean bookmarked) {
        super(itemType, postId, displayUrl, thumbnailUrl, shortCode, postCaption, timestamp, liked, bookmarked);
        this.profileModel = profileModel;
        this.commentsCount = commentsCount;
        this.viewCount = viewCount;
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
}