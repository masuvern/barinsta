package awais.instagrabber.asyncs;

//import android.os.Handler;
//import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.webservices.FeedService;
import awais.instagrabber.webservices.ServiceCallback;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedPostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "FeedPostFetchService";
    private final FeedService feedService;
    private String nextCursor;
//    private final Handler handler;
    private boolean hasNextPage;
//    private static final int DELAY_MILLIS = 500;

    public FeedPostFetchService() {
        feedService = FeedService.getInstance();
//        handler = new Handler();
    }

    @Override
    public void fetch(final FetchListener<List<FeedModel>> fetchListener) {
        final List<FeedModel> feedModels = new ArrayList<>();
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        feedModels.clear();
        feedService.fetch(csrfToken, nextCursor, new ServiceCallback<PostsFetchResponse>() {
            @Override
            public void onSuccess(final PostsFetchResponse result) {
                if (result == null && feedModels.size() > 0) {
                    fetchListener.onResult(feedModels);
                    return;
                }
                else if (result == null) return;
                nextCursor = result.getNextCursor();
                hasNextPage = result.hasNextPage();
                feedModels.addAll(result.getFeedModels());
                if (fetchListener != null) {
                    if (feedModels.size() < 15 && hasNextPage) {
//                        handler.postDelayed(() -> {
                            feedService.fetch(csrfToken, nextCursor, this);
//                        }, DELAY_MILLIS);
                    }
                    else {
                        fetchListener.onResult(feedModels);
                    }
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
