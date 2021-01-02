package awais.instagrabber.repositories.responses.directmessages;

public final class DirectItemAnimatedMedia {
    private final String id;
    private final AnimatedMediaImages images;
    private final boolean isRandom;
    private final boolean isSticker;

    public DirectItemAnimatedMedia(final String id, final AnimatedMediaImages images, final boolean isRandom, final boolean isSticker) {

        this.id = id;
        this.images = images;
        this.isRandom = isRandom;
        this.isSticker = isSticker;
    }

    public String getId() {
        return id;
    }

    public AnimatedMediaImages getImages() {
        return images;
    }

    public boolean isRandom() {
        return isRandom;
    }

    public boolean isSticker() {
        return isSticker;
    }
}
