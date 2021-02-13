package awais.instagrabber.repositories.responses;

public class AnimatedMediaFixedHeight {
    private final int height;
    private final int width;
    private final String mp4;
    private final String url;
    private final String webp;

    public AnimatedMediaFixedHeight(final int height, final int width, final String mp4, final String url, final String webp) {
        this.height = height;
        this.width = width;
        this.mp4 = mp4;
        this.url = url;
        this.webp = webp;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getMp4() {
        return mp4;
    }

    public String getUrl() {
        return url;
    }

    public String getWebp() {
        return webp;
    }
}
