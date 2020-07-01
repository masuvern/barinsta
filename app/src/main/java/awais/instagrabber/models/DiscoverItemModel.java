package awais.instagrabber.models;

import awais.instagrabber.models.enums.MediaItemType;

public final class DiscoverItemModel extends BasePostModel {
    private boolean moreAvailable;
    private String nextMaxId;

    public DiscoverItemModel(final MediaItemType mediaType, final String postId, final String shortCode, final String thumbnail) {
        this.postId = postId;
        this.itemType = mediaType;
        this.shortCode = shortCode;
        this.displayUrl = thumbnail;
    }


    public void setMore(final boolean moreAvailable, final String nextMaxId) {
        this.moreAvailable = moreAvailable;
        this.nextMaxId = nextMaxId;
    }

    public boolean hasMore() {
        return moreAvailable;
    }

    public String getNextMaxId() {
        return nextMaxId;
    }
}