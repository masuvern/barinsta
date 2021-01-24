package awais.instagrabber.repositories.responses.directmessages;

import java.io.Serializable;

public class ThreadContext implements Serializable {
    private final int type;
    private final String text;

    public ThreadContext(final int type, final String text) {
        this.type = type;
        this.text = text;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
