package awais.instagrabber.models;

public final class HighlightModel {
    private final String title, thumbnailUrl;
    private StoryModel[] storyModels;

    public HighlightModel(final String title, final String thumbnailUrl) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public StoryModel[] getStoryModels() {
        return storyModels;
    }

    public void setStoryModels(final StoryModel[] storyModels) {
        this.storyModels = storyModels;
    }
}