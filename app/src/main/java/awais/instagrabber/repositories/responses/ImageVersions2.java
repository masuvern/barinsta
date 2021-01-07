package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;

public class ImageVersions2 implements Serializable {
    private final List<MediaCandidate> candidates;

    public ImageVersions2(final List<MediaCandidate> candidates) {
        this.candidates = candidates;
    }

    public List<MediaCandidate> getCandidates() {
        return candidates;
    }
}
