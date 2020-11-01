package awais.instagrabber.customviews.helpers;

import java.util.List;

import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;

public class PostFetcher {
    private final PostFetchService postFetchService;
    private final FetchListener<List<FeedModel>> fetchListener;
    private boolean fetching;

    public PostFetcher(final PostFetchService postFetchService,
                       final FetchListener<List<FeedModel>> fetchListener) {
        this.postFetchService = postFetchService;
        this.fetchListener = fetchListener;
    }

    public void fetch() {
        fetching = true;
        postFetchService.fetch(result -> {
            fetching = false;
            fetchListener.onResult(result);
        });
    }

    public void reset() {
        postFetchService.reset();
    }

    public boolean isFetching() {
        return fetching;
    }

    public boolean hasMore() {
        return postFetchService.hasNextPage();
    }

    public interface PostFetchService {
        void fetch(FetchListener<List<FeedModel>> fetchListener);

        void reset();

        boolean hasNextPage();
    }
}
