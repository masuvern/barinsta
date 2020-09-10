package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class QuizAction extends AsyncTask<Integer, Void, Integer> {
    private static final String TAG = "QuizAction";

    private final StoryModel storyModel;
    private final QuizModel quizModel;
    private final String cookie;
    private final OnTaskCompleteListener onTaskCompleteListener;

    public QuizAction(final StoryModel storyModel,
                      final QuizModel quizModel,
                      final String cookie,
                      final OnTaskCompleteListener onTaskCompleteListener) {
        this.storyModel = storyModel;
        this.quizModel = quizModel;
        this.cookie = cookie;
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    protected Integer doInBackground(Integer... rawChoice) {
        int choice = rawChoice[0];
        final String url = "https://i.instagram.com/api/v1/media/" + storyModel.getStoryMediaId().split("_")[0] + "/" + quizModel.getId() + "/story_quiz_answer/";
        HttpURLConnection urlConnection = null;
        try {
            JSONObject ogBody = new JSONObject("{\"client_context\":\"" + UUID.randomUUID().toString()
                    + "\",\"mutation_token\":\"" + UUID.randomUUID().toString()
                    + "\",\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                    + "\",\"_uid\":\"" + CookieUtils.getUserIdFromCookie(cookie)
                    + "\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                    + "\"}");
            ogBody.put("answer", String.valueOf(choice));
            String urlParameters = Utils.sign(ogBody.toString());
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (urlParameters != null) {
                urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            }
            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            Log.d(TAG, "quiz: " + url + " " + cookie + " " + urlParameters);
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return choice;
            }
        } catch (Throwable ex) {
            Log.e(TAG, "quiz: " + ex);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return -1;
    }

    @Override
    protected void onPostExecute(final Integer choice) {
        if (onTaskCompleteListener == null || choice == null) return;
        onTaskCompleteListener.onTaskComplete(choice);
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final int choice);
    }
}
