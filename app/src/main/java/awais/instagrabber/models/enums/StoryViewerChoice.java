package awais.instagrabber.models.enums;

import java.io.Serializable;

public enum StoryViewerChoice implements Serializable {
    NONE(0),
    ALOINSTAGRAM(1),
    INSTADP(2);

    private int value;

    StoryViewerChoice(int value) {
        this.value = value;
    }

    public String getValue() {
        return String.valueOf(value);
    }
}