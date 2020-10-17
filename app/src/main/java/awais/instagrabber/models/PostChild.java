package awais.instagrabber.models;

import java.io.Serializable;

import awais.instagrabber.models.enums.MediaItemType;

public final class PostChild implements Serializable {
    private String postId;
    private MediaItemType itemType;
    private String displayUrl;
    private final String thumbnailUrl;
    private final long videoViews;
    private int width;
    private int height;

    public static class Builder {
        private String postId;
        private MediaItemType itemType;
        private String displayUrl;
        private long videoViews;
        private String thumbnailUrl;
        private int width;
        private int height;

        public Builder setPostId(final String postId) {
            this.postId = postId;
            return this;
        }

        public Builder setItemType(final MediaItemType itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder setDisplayUrl(final String displayUrl) {
            this.displayUrl = displayUrl;
            return this;
        }

        public Builder setVideoViews(final long videoViews) {
            this.videoViews = videoViews;
            return this;
        }

        public Builder setHeight(final int height) {
            this.height = height;
            return this;
        }

        public Builder setWidth(final int width) {
            this.width = width;
            return this;
        }

        public Builder setThumbnailUrl(final String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public PostChild build() {
            return new PostChild(postId, itemType, displayUrl, thumbnailUrl, videoViews, height, width);
        }
    }

    public PostChild(final String postId,
                     final MediaItemType itemType,
                     final String displayUrl,
                     final String thumbnailUrl,
                     final long videoViews,
                     final int height,
                     final int width) {
        this.postId = postId;
        this.itemType = itemType;
        this.displayUrl = displayUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.height = height;
        this.width = width;
        this.videoViews = videoViews;
    }

    public String getPostId() {
        return postId;
    }

    public MediaItemType getItemType() {
        return itemType;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public long getVideoViews() {
        return videoViews;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "PostChild{" +
                "postId='" + postId + '\'' +
                ", itemType=" + itemType +
                ", displayUrl='" + displayUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", videoViews=" + videoViews +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}