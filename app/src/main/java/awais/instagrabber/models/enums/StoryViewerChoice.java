package awais.instagrabber.models.enums;

import java.io.Serializable;

public enum StoryViewerChoice implements Serializable {
    NONE(0),
    STORIESIG(1),
    ALOINSTAGRAM(2),
    INSTADP(3);

    private int value;

    StoryViewerChoice(int value) {
        this.value = value;
    }

    public String getValue() {
        return String.valueOf(value);
    }
}