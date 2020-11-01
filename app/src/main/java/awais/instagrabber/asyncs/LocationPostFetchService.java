package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.webservices.LocationService;
import awais.instagrabber.webservices.LocationService.LocationPostsFetchResponse;
import awais.instagrabber.webservices.ServiceCallback;

public class LocationPostFetchService implements PostFetcher.PostFetchService {
    private final LocationService locationService;
    private final LocationModel locationModel;
    private String nextMaxId;
    private boolean moreAvailable;

    public LocationPostFetchService(final LocationModel locationModel) {
        this.locationModel = locationModel;
        locationService = LocationService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        locationService.fetchPosts(locationModel.getId(), nextMaxId, new ServiceCallback<LocationPostsFetchResponse>() {
            @Override
            public void onSuccess(final LocationPostsFetchResponse result) {
                if (result == null) return;
                nextMaxId = result.getNextMaxId();
                moreAvailable = result.isMoreAvailable();
                if (fetchListener != null) {
                    fetchListener.onResult(result.getItems());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                // Log.e(TAG, "onFailure: ", t);
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
            }
        });
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
