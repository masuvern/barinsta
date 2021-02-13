package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemPlaceholder {
    private final boolean isLinked;
    private final String title;
    private final String message;

    public DirectItemPlaceholder(final boolean isLinked,
                                 final String title,
                                 final String message) {
        this.isLinked = isLinked;
        this.title = title;
        this.message = message;
    }

    public boolean isLinked() {
        return isLinked;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
