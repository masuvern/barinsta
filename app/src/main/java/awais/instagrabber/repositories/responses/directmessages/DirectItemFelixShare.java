package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemFelixShare {
    private final DirectItemMedia video;

    public DirectItemFelixShare(final DirectItemMedia video) {
        this.video = video;
    }

    public DirectItemMedia getVideo() {
        return video;
    }
}
