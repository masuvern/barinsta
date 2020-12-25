package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.webservices.NewsService;
import awais.instagrabber.webservices.ServiceCallback;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class NotificationsFetcher extends AsyncTask<Void, Void, List<NotificationModel>> {
    private static final String TAG = "NotificationsFetcher";

    private final FetchListener<List<NotificationModel>> fetchListener;
    private final NewsService newsService;
    private final boolean markAsSeen;
    private boolean fetchedWeb = false;

    public NotificationsFetcher(final boolean markAsSeen,
                                final FetchListener<List<NotificationModel>> fetchListener) {
        this.markAsSeen = markAsSeen;
        this.fetchListener = fetchListener;
        newsService = NewsService.getInstance();
    }

    @Override
    protected List<NotificationModel> doInBackground(final Void... voids) {
        List<NotificationModel> notificationModels = new ArrayList<>();

        newsService.fetchAppInbox(markAsSeen, new ServiceCallback<List<NotificationModel>>() {
            @Override
            public void onSuccess(final List<NotificationModel> result) {
                if (result == null) return;
                notificationModels.addAll(result);
                if (fetchedWeb) {
                    fetchListener.onResult(notificationModels);
                }
                else {
                    fetchedWeb = true;
                    newsService.fetchWebInbox(markAsSeen, this);
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
        return notificationModels;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }
}