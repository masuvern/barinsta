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
        fetch(null);
    }

    public void fetchNextPage() {
        fetch(postFetchService.getNextCursor());
    }

    public void fetch(final String cursor) {
        fetching = true;
        postFetchService.fetch(cursor, result -> {
            fetching = false;
            fetchListener.onResult(result);
        });
    }

    public boolean isFetching() {
        return fetching;
    }

    public boolean hasMore() {
        return postFetchService.hasNextPage();
    }

    public interface PostFetchService {
        void fetch(String cursor, FetchListener<List<FeedModel>> fetchListener);

        String getNextCursor();

        boolean hasNextPage();
    }
}
