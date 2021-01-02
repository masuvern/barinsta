package awais.instagrabber.models.enums;

public enum BroadcastItemType {
    TEXT("text"),
    REACTION("reaction"),
    REELSHARE("reel_share"),
    IMAGE("configure_photo"),
    LINK("link"),
    VIDEO("configure_video"),
    VOICE("share_voice");

    private final String value;

    BroadcastItemType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
