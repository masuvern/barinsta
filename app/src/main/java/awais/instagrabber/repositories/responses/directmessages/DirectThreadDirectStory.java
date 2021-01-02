package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

public class DirectThreadDirectStory {
    private final List<DirectItem> items;
    private final int unseenCount;

    public DirectThreadDirectStory(final List<DirectItem> items, final int unseenCount) {
        this.items = items;
        this.unseenCount = unseenCount;
    }

    public List<DirectItem> getItems() {
        return items;
    }

    public int getUnseenCount() {
        return unseenCount;
    }
}
