package awais.instagrabber.repositories.responses;

import java.util.List;

import awais.instagrabber.repositories.responses.directmessages.DirectUser;

public class UserSearchResponse {
    private final int numResults;
    private final List<DirectUser> users;
    private final boolean hasMore;
    private final String status;

    public UserSearchResponse(final int numResults, final List<DirectUser> users, final boolean hasMore, final String status) {
        this.numResults = numResults;
        this.users = users;
        this.hasMore = hasMore;
        this.status = status;
    }

    public int getNumResults() {
        return numResults;
    }

    public List<DirectUser> getUsers() {
        return users;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public String getStatus() {
        return status;
    }
}
