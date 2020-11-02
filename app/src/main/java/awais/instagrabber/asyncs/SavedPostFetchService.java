package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.webservices.ProfileService;
import awais.instagrabber.webservices.ProfileService.SavedPostsFetchResponse;
import awais.instagrabber.webservices.ServiceCallback;

public class SavedPostFetchService implements PostFetcher.PostFetchService {
    private final ProfileService profileService;
    private final String profileId;
    private final PostItemType type;

    private String nextMaxId;
    private boolean moreAvailable;

    public SavedPostFetchService(final String profileId, final PostItemType type) {
        this.profileId = profileId;
        this.type = type;
        profileService = ProfileService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        final ServiceCallback<SavedPostsFetchResponse> callback = new ServiceCallback<SavedPostsFetchResponse>() {
            @Override
            public void onSuccess(final SavedPostsFetchResponse result) {
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
        };
        switch (type) {
            case LIKED:
                profileService.fetchLiked(nextMaxId, callback);
                break;
            case TAGGED:
                profileService.fetchTagged(profileId, nextMaxId, callback);
                break;
            case SAVED:
            default:
                profileService.fetchSaved(nextMaxId, callback);
                break;
        }
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
