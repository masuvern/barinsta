package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemReelShareReactionInfo {
    private final String emoji;
    private final String intensity;

    public DirectItemReelShareReactionInfo(final String emoji, final String intensity) {
        this.emoji = emoji;
        this.intensity = intensity;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getIntensity() {
        return intensity;
    }
}
