package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.utils.Constants;

public class VoteAction extends AsyncTask<Integer, Void, Integer> {

    private static final String TAG = "VoteAction";

    private final StoryModel storyModel;
    private final PollModel pollModel;
    private final String cookie;
    private final OnTaskCompleteListener onTaskCompleteListener;

    public VoteAction(final StoryModel storyModel,
                      final PollModel pollModel,
                      final String cookie,
                      final OnTaskCompleteListener onTaskCompleteListener) {
        this.storyModel = storyModel;
        this.pollModel = pollModel;
        this.cookie = cookie;
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    protected Integer doInBackground(Integer... rawChoice) {
        int choice = rawChoice[0];
        final String url = "https://www.instagram.com/media/" + storyModel.getStoryMediaId().split("_")[0] + "/" + pollModel.getId() + "/story_poll_vote/";
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
            urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length", "6");
            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes("vote=" + choice);
            wr.flush();
            wr.close();
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return choice;
            }
            urlConnection.disconnect();
        } catch (Exception ex) {
            Log.e(TAG, "Error", ex);
        }
        return -1;
    }

    @Override
    protected void onPostExecute(final Integer result) {
        if (result == null || onTaskCompleteListener == null) return;
        onTaskCompleteListener.onTaskComplete(result);
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final int choice);
    }
}
