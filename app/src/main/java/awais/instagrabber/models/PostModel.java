package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public class PostModel extends BasePostModel {
    protected final String thumbnailUrl;
    protected String endCursor;
    protected boolean hasNextPage;

    public PostModel(final String shortCode) {
        this.shortCode = shortCode;
        this.thumbnailUrl = null;
    }

    public PostModel(final MediaItemType itemType, final String postId, final String displayUrl, final String thumbnailUrl,
                     final String shortCode, final CharSequence postCaption, long timestamp, boolean liked, boolean bookmarked) {
        this.itemType = itemType;
        this.postId = postId;
        this.displayUrl = displayUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.shortCode = shortCode;
        this.postCaption = postCaption;
        this.timestamp = timestamp;
        this.liked = liked;
        this.bookmarked = bookmarked;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getEndCursor() {
        return endCursor;
    }

    public boolean getLike() {
        return liked;
    }
    public boolean getBookmark() {
        return bookmarked;
    }

    public boolean setLike() {
        liked = liked == true ? false : true; this.liked = liked; return liked;
    }
    public boolean setBookmark() {
        bookmarked = bookmarked == true ? false : true; this.bookmarked = bookmarked; return bookmarked;
    }

    public boolean hasNextPage() {
        return endCursor != null && hasNextPage;
    }

    public void setPostCaption(final CharSequence postCaption) {
        this.postCaption = postCaption;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public void setPageCursor(final boolean hasNextPage, final String endCursor) {
        this.endCursor = endCursor;
        this.hasNextPage = hasNextPage;
    }
}