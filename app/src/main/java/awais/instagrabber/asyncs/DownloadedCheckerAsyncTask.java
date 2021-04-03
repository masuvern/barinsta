package awais.instagrabber.asyncs;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.DownloadUtils;

public final class DownloadedCheckerAsyncTask extends AsyncTask<Media, Void, Map<String, List<Boolean>>> {
    private static final String TAG = "DownloadedCheckerAsyncTask";

    private final WeakReference<Context> context;
    private final OnCheckResultListener listener;

    public DownloadedCheckerAsyncTask(final Context context,
                                      final OnCheckResultListener listener) {
        this.context = new WeakReference<>(context);
        this.listener = listener;
    }

    @Override
    protected Map<String, List<Boolean>> doInBackground(final Media... feedModels) {
        if (feedModels == null) {
            return null;
        }
        final Map<String, List<Boolean>> map = new HashMap<>();
        for (final Media media : feedModels) {
            final Context context = this.context.get();
            if (context == null) return map;
            map.put(media.getPk(), DownloadUtils.checkDownloaded(context, media));
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
