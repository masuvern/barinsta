package awais.instagrabber.repositories.responses.directmessages;

import awais.instagrabber.repositories.responses.Media;

public class DirectItemVoiceMedia {
    private final Media media;
    private final int seenCount;
    private final String viewMode;

    public DirectItemVoiceMedia(final Media media, final int seenCount, final String viewMode) {
        this.media = media;
        this.seenCount = seenCount;
        this.viewMode = viewMode;
    }

    public Media getMedia() {
        return media;
    }

    public int getSeenCount() {
        return seenCount;
    }

    public String getViewMode() {
        return viewMode;
    }
}
