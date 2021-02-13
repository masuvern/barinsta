package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

public class DirectItemActionLog {
    private final String description;
    private final List<TextRange> bold;
    private final List<TextRange> textAttributes;

    public DirectItemActionLog(final String description,
                               final List<TextRange> bold,
                               final List<TextRange> textAttributes) {
        this.description = description;
        this.bold = bold;
        this.textAttributes = textAttributes;
    }

    public String getDescription() {
        return description;
    }

    public List<TextRange> getBold() {
        return bold;
    }

    public List<TextRange> getTextAttributes() {
        return textAttributes;
    }

    public static class TextRange {
        private final int start;
        private final int end;
        private final String color;
        private final String intent;

        public TextRange(final int start, final int end, final String color, final String intent) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.intent = intent;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getColor() {
            return color;
        }

        public String getIntent() {
            return intent;
        }
    }
}
