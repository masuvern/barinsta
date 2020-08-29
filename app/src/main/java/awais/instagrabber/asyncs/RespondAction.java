package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class RespondAction extends AsyncTask<String, Void, Boolean> {

    private final StoryModel storyModel;
    private final QuestionModel questionModel;
    private final String cookie;
    private final OnTaskCompleteListener onTaskCompleteListener;

    public RespondAction(final StoryModel storyModel,
                         final QuestionModel questionModel,
                         final String cookie,
                         final OnTaskCompleteListener onTaskCompleteListener) {
        this.storyModel = storyModel;
        this.questionModel = questionModel;
        this.cookie = cookie;
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    protected Boolean doInBackground(String... rawChoice) {
        final String url = "https://i.instagram.com/api/v1/media/"
                + storyModel.getStoryMediaId().split("_")[0] + "/" + questionModel.getId() + "/story_question_response/";
        HttpURLConnection urlConnection = null;
        try {
            JSONObject ogbody = new JSONObject("{\"client_context\":\"" + UUID.randomUUID().toString()
                    + "\",\"mutation_token\":\"" + UUID.randomUUID().toString()
                    + "\",\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                    + "\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                    + "\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                    + "\"}");
            String choice = rawChoice[0].replaceAll("\"", ("\\\""));
            ogbody.put("response", choice);
            String urlParameters = Utils.sign(ogbody.toString());
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return true;
            }

        } catch (Throwable ex) {
            Log.e("austin_debug", "respond: " + ex);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Boolean ok) {
        if (onTaskCompleteListener == null) return;
        onTaskCompleteListener.onTaskComplete(ok);

    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final boolean result);
    }
}
