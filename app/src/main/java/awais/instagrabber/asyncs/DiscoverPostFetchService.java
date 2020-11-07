package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.webservices.DiscoverService;
import awais.instagrabber.webservices.ServiceCallback;

public class DiscoverPostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "DiscoverPostFetchService";
    private final DiscoverService discoverService;
    private final DiscoverService.TopicalExploreRequest topicalExploreRequest;
    private boolean moreAvailable = false;

    public DiscoverPostFetchService(final DiscoverService.TopicalExploreRequest topicalExploreRequest) {
        this.topicalExploreRequest = topicalExploreRequest;
        discoverService = DiscoverService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        discoverService.topicalExplore(topicalExploreRequest, new ServiceCallback<DiscoverService.TopicalExploreResponse>() {
            @Override
            public void onSuccess(final DiscoverService.TopicalExploreResponse result) {
                if (result == null) {
                    onFailure(new RuntimeException("result is null"));
                    return;
                }
                moreAvailable = result.isMoreAvailable();
                topicalExploreRequest.setMaxId(result.getNextMaxId());
                if (fetchListener != null) {
                    fetchListener.onResult(result.getItems());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
            }
        });
    }

    @Override
    public void reset() {
        topicalExploreRequest.setMaxId(-1);
    }

    @Override
    public boolean hasNextPage() {
        return moreAvailable;
    }
}
