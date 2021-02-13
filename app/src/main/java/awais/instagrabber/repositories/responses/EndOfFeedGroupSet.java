package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;

public class EndOfFeedGroupSet implements Serializable {
    private final long id;
    private final String activeGroupId;
    private final String connectedGroupId;
    private final String nextMaxId;
    private final String paginationSource;
    private final List<EndOfFeedGroup> groups;

    public EndOfFeedGroupSet(final long id,
                             final String activeGroupId,
                             final String connectedGroupId,
                             final String nextMaxId,
                             final String paginationSource,
                             final List<EndOfFeedGroup> groups) {
        this.id = id;
        this.activeGroupId = activeGroupId;
        this.connectedGroupId = connectedGroupId;
        this.nextMaxId = nextMaxId;
        this.paginationSource = paginationSource;
        this.groups = groups;
    }

    public long getId() {
        return id;
    }

    public String getActiveGroupId() {
        return activeGroupId;
    }

    public String getConnectedGroupId() {
        return connectedGroupId;
    }

    public String getNextMaxId() {
        return nextMaxId;
    }

    public String getPaginationSource() {
        return paginationSource;
    }

    public List<EndOfFeedGroup> getGroups() {
        return groups;
    }
}
