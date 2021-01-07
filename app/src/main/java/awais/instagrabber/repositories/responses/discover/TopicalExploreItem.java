package awais.instagrabber.repositories.responses.discover;

import awais.instagrabber.repositories.responses.Media;

public class TopicalExploreItem {
    private final Media media;

    public TopicalExploreItem(final Media media) {
        this.media = media;
    }

    public Media getMedia() {
        return media;
    }
}
