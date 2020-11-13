package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.HashtagModel;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.TagsService;
import awais.instagrabber.webservices.TagsService.TagPostsFetchResponse;

public class HashtagPostFetchService implements PostFetcher.PostFetchService {
    private final TagsService tagsService;
    private final HashtagModel hashtagModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public HashtagPostFetchService(final HashtagModel hashtagModel, final boolean isLoggedIn) {
        this.hashtagModel = hashtagModel;
        this.isLoggedIn = isLoggedIn;
        tagsService = TagsService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        final ServiceCallback cb = new ServiceCallback<TagPostsFetchResponse>() {
            @Override
            public void onSuccess(final TagPostsFetchResponse result) {
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
        if (isLoggedIn) tagsService.fetchPosts(hashtagModel.getName().toLowerCase(), nextMaxId, cb);
        else tagsService.fetchGraphQLPosts(hashtagModel.getName().toLowerCase(), nextMaxId, cb);
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
