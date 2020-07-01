package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public final class ViewerPostModel extends BasePostModel {
    protected final String username;
    protected final long videoViews;
    protected String sliderDisplayUrl, commentsEndCursor;
    protected long commentsCount;
    private boolean isCurrentSlide = false;

    public ViewerPostModel(final MediaItemType itemType, final String postId, final String displayUrl, final String shortCode,
                           final String postCaption, final String username, final long videoViews, final long timestamp) {
        this.itemType = itemType;
        this.postId = postId;
        this.displayUrl = displayUrl;
        this.postCaption = postCaption;
        this.username = username;
        this.shortCode = shortCode;
        this.videoViews = videoViews;
        this.timestamp = timestamp;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public String getSliderDisplayUrl() {
        return sliderDisplayUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getCommentsEndCursor() {
        return commentsEndCursor;
    }

    public final long getVideoViews() {
        return videoViews;
    }

    public void setSliderDisplayUrl(final String sliderDisplayUrl) {
        this.sliderDisplayUrl = sliderDisplayUrl;
    }

    public void setCommentsCount(final long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public void setCommentsEndCursor(final String commentsEndCursor) {
        this.commentsEndCursor = commentsEndCursor;
    }

    public void setCurrentSlide(final boolean currentSlide) {
        this.isCurrentSlide = currentSlide;
    }

    public boolean isCurrentSlide() {
        return isCurrentSlide;
    }
}