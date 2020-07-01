package awais.instagrabber.models;

import java.io.Serializable;

import awais.instagrabber.models.enums.MediaItemType;

public final class StoryModel implements Serializable {
    private final String storyMediaId, storyUrl;
    private final MediaItemType itemType;
    private final long timestamp;
    private String videoUrl, tappableShortCode;
    private int position;
    private boolean isCurrentSlide = false;

    public StoryModel(final String storyMediaId, final String storyUrl, final MediaItemType itemType, final long timestamp) {
        this.storyMediaId = storyMediaId;
        this.storyUrl = storyUrl;
        this.itemType = itemType;
        this.timestamp = timestamp;
    }

    public String getStoryUrl() {
        return storyUrl;
    }

    public String getStoryMediaId() {
        return storyMediaId;
    }

    public MediaItemType getItemType() {
        return itemType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getTappableShortCode() {
        return tappableShortCode;
    }

    public int getPosition() {
        return position;
    }

    public void setVideoUrl(final String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setTappableShortCode(final String tappableShortCode) {
        this.tappableShortCode = tappableShortCode;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public void setCurrentSlide(final boolean currentSlide) {
        this.isCurrentSlide = currentSlide;
    }

    public boolean isCurrentSlide() {
        return isCurrentSlide;
    }
}