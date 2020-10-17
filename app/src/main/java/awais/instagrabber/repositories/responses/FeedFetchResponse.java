package awais.instagrabber.repositories.responses;

import java.util.List;

import awais.instagrabber.models.FeedModel;

public class FeedFetchResponse {
    private List<FeedModel> feedModels;
    private boolean hasNextPage;
    private String nextCursor;

    public FeedFetchResponse(final List<FeedModel> feedModels, final boolean hasNextPage, final String nextCursor) {
        this.feedModels = feedModels;
        this.hasNextPage = hasNextPage;
        this.nextCursor = nextCursor;
    }

    public List<FeedModel> getFeedModels() {
        return feedModels;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public String getNextCursor() {
        return nextCursor;
    }
}
