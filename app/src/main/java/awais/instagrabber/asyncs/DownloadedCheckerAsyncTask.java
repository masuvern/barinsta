package awais.instagrabber.asyncs;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.DownloadUtils;

public final class DownloadedCheckerAsyncTask extends AsyncTask<FeedModel, Void, Map<String, List<Boolean>>> {
    private static final String TAG = "DownloadedCheckerAsyncTask";

    private final OnCheckResultListener listener;

    public DownloadedCheckerAsyncTask(final OnCheckResultListener listener) {
        this.listener = listener;
    }

    @Override
    protected Map<String, List<Boolean>> doInBackground(final FeedModel... feedModels) {
        if (feedModels == null) {
            return null;
        }
        final Map<String, List<Boolean>> map = new HashMap<>();
        for (final FeedModel feedModel : feedModels) {
            map.put(feedModel.getPostId(), DownloadUtils.checkDownloaded(feedModel));
        }
        return map;
    }

    @Override
    protected void onPostExecute(final Map<String, List<Boolean>> result) {
        if (listener == null) return;
        listener.onResult(result);
    }

    public interface OnCheckResultListener {
        void onResult(final Map<String, List<Boolean>> result);
    }
}
