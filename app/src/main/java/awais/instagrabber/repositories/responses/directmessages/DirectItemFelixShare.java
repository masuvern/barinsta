package awais.instagrabber.repositories.responses.directmessages;

import awais.instagrabber.repositories.responses.Media;

public class DirectItemFelixShare {
    private final Media video;

    public DirectItemFelixShare(final Media video) {
        this.video = video;
    }

    public Media getVideo() {
        return video;
    }
}
