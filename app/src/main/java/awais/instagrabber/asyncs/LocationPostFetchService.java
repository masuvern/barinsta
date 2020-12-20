package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.webservices.GraphQLService;
import awais.instagrabber.webservices.LocationService;
import awais.instagrabber.webservices.ServiceCallback;

public class LocationPostFetchService implements PostFetcher.PostFetchService {
    private final LocationService locationService;
    private final GraphQLService graphQLService;
    private final LocationModel locationModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public LocationPostFetchService(final LocationModel locationModel, final boolean isLoggedIn) {
        this.locationModel = locationModel;
        this.isLoggedIn = isLoggedIn;
        locationService = isLoggedIn ? LocationService.getInstance() : null;
        graphQLService = isLoggedIn ? null : GraphQLService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        final ServiceCallback cb = new ServiceCallback<PostsFetchResponse>() {
            @Override
            public void onSuccess(final PostsFetchResponse result) {
                if (result == null) return;
                nextMaxId = result.getNextCursor();
                moreAvailable = result.hasNextPage();
                if (fetchListener != null) {
                    fetchListener.onResult(result.getFeedModels());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                // Log.e(TAG, "onFailure: ", t);
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
            }
        };
        if (isLoggedIn) locationService.fetchPosts(locationModel.getId(), nextMaxId, cb);
        else graphQLService.fetchLocationPosts(locationModel.getId(), nextMaxId, cb);
    }

    @Override
    public void reset() {
        nextMaxId = null;
    }

    @Override
    public boolean hasNextPage() {
        return moreAvailable;
    }
}
