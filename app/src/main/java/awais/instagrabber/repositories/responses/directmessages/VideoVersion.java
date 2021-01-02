package awais.instagrabber.repositories.responses.directmessages;

public class VideoVersion {
    private final String id;
    private final String type;
    private final int width;
    private final int height;
    private final String url;

    public VideoVersion(final String id, final String type, final int width, final int height, final String url) {
        this.id = id;
        this.type = type;
        this.width = width;
        this.height = height;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getUrl() {
        return url;
    }
}
