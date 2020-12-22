package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public class PostModel extends BasePostModel {
    protected final String thumbnailUrl;
    protected String endCursor;
    protected boolean hasNextPage;

    public PostModel(final String shortCode, final boolean isid) {
        if (!isid) this.shortCode = shortCode;
        else this.postId = shortCode;
        this.thumbnailUrl = null;
    }

    public PostModel(final MediaItemType itemType,
                     final String postId,
                     final String displayUrl,
                     final String thumbnailUrl,
                     final String shortCode,
                     final CharSequence postCaption,
                     final String captionId,
                     long timestamp,
                     boolean liked,
                     boolean bookmarked) {
        this.itemType = itemType;
        this.postId = postId;
        this.displayUrl = displayUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.shortCode = shortCode;
        this.postCaption = postCaption;
        this.captionId = captionId;
        this.timestamp = timestamp;
        this.liked = liked;
        this.saved = bookmarked;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getEndCursor() {
        return endCursor;
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