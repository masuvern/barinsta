package awais.instagrabber.repositories.responses.directmessages;

public class Caption {
    private final long pk;
    private final long userId;
    private final String text;

    public Caption(final long pk, final long userId, final String text) {
        this.pk = pk;
        this.userId = userId;
        this.text = text;
    }

    public long getPk() {
        return pk;
    }

    public long getUserId() {
        return userId;
    }

    public String getText() {
        return text;
    }
}
