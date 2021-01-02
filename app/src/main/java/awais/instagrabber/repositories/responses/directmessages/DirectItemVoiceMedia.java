package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemVoiceMedia {
    private final DirectItemMedia media;
    private final int seenCount;
    private final String viewMode;

    public DirectItemVoiceMedia(final DirectItemMedia media, final int seenCount, final String viewMode) {
        this.media = media;
        this.seenCount = seenCount;
        this.viewMode = viewMode;
    }

    public DirectItemMedia getMedia() {
        return media;
    }

    public int getSeenCount() {
        return seenCount;
    }

    public String getViewMode() {
        return viewMode;
    }
}
