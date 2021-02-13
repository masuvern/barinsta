package awais.instagrabber.repositories.responses;

import java.io.Serializable;

public class EndOfFeedDemarcator implements Serializable {
    private final long id;
    private final EndOfFeedGroupSet groupSet;

    public EndOfFeedDemarcator(final long id, final EndOfFeedGroupSet groupSet) {
        this.id = id;
        this.groupSet = groupSet;
    }

    public long getId() {
        return id;
    }

    public EndOfFeedGroupSet getGroupSet() {
        return groupSet;
    }
}
