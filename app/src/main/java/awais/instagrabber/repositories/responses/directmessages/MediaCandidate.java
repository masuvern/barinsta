package awais.instagrabber.repositories.responses.directmessages;

public class MediaCandidate {
    private int width;
    private int height;
    private String url;

    public MediaCandidate(final int width, final int height, final String url) {
        this.width = width;
        this.height = height;
        this.url = url;
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
