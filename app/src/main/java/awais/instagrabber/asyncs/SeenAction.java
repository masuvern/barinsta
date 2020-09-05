package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.models.StoryModel;
import awais.instagrabber.utils.Utils;

public class SeenAction extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "SeenAction";

    private final String cookie;
    private final StoryModel storyModel;

    public SeenAction(final String cookie, final StoryModel storyModel) {
        this.cookie = cookie;
        this.storyModel = storyModel;
    }

    protected Void doInBackground(Void... voids) {
        final String url = "https://www.instagram.com/stories/reel/seen";
        try {
            final String urlParameters = "reelMediaId=" + storyModel.getStoryMediaId().split("_")[0]
                    + "&reelMediaOwnerId=" + storyModel.getUserId()
                    + "&reelId=" + storyModel.getUserId()
                    + "&reelMediaTakenAt=" + storyModel.getTimestamp()
                    + "&viewSeenAt=" + storyModel.getTimestamp();
            final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            urlConnection.connect();
            Log.d(TAG, urlConnection.getResponseCode() + " " + Utils.readFromConnection(urlConnection));
            urlConnection.disconnect();
        } catch (Throwable ex) {
            Log.e(TAG, "Error", ex);
        }
        return null;
    }
}
