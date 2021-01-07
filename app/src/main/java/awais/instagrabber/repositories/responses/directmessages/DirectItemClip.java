package awais.instagrabber.repositories.responses.directmessages;

import awais.instagrabber.repositories.responses.Media;

public class DirectItemClip {
    private final Media clip;

    public DirectItemClip(final Media clip) {
        this.clip = clip;
    }

    public Media getClip() {
        return clip;
    }
}
