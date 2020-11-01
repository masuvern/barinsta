package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.webservices.ProfileService;
import awais.instagrabber.webservices.ServiceCallback;

public class ProfilePostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "ProfilePostFetchService";
    private final ProfileService profileService;
    private final ProfileModel profileModel;
    private String nextCursor;
    private boolean hasNextPage;

    public ProfilePostFetchService(final ProfileModel profileModel) {
        this.profileModel = profileModel;
        profileService = ProfileService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        profileService.fetchPosts(profileModel, 30, nextCursor, new ServiceCallback<PostsFetchResponse>() {
            @Override
            public void onSuccess(final PostsFetchResponse result) {
                if (result == null) return;
                nextCursor = result.getNextCursor();
                hasNextPage = result.hasNextPage();
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
        });
    }

    @Override
    public void reset() {
        nextCursor = null;
    }

    @Override
    public boolean hasNextPage() {
        return hasNextPage;
    }
}
