package awais.instagrabber.models;

public final class HighlightModel {
    private final String title, id, thumbnailUrl;

    public HighlightModel(final String title, final String id, final String thumbnailUrl) {
        this.title = title;
        this.id = id;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}