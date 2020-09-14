package awais.instagrabber.models;

public final class HighlightModel {
    private final String title;
    private final String id;
    private final String thumbnailUrl;

    public HighlightModel(final String title,
                          final String id,
                          final String thumbnailUrl) {
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