package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemClip {
    private final DirectItemMedia clip;

    public DirectItemClip(final DirectItemMedia clip) {
        this.clip = clip;
    }

    public DirectItemMedia getClip() {
        return clip;
    }
}
