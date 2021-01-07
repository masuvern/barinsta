package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;

public class EndOfFeedGroup implements Serializable {
    private final String id;
    private final String title;
    private final String nextMaxId;
    private final List<Media> feedItems;

    public EndOfFeedGroup(final String id, final String title, final String nextMaxId, final List<Media> feedItems) {
        this.id = id;
        this.title = title;
        this.nextMaxId = nextMaxId;
        this.feedItems = feedItems;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getNextMaxId() {
        return nextMaxId;
    }

    public List<Media> getFeedItems() {
        return feedItems;
    }
}
