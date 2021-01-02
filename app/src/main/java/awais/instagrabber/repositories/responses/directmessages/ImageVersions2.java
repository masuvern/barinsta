package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

public class ImageVersions2 {
    private final List<MediaCandidate> candidates;

    public ImageVersions2(final List<MediaCandidate> candidates) {
        this.candidates = candidates;
    }

    public List<MediaCandidate> getCandidates() {
        return candidates;
    }
}
