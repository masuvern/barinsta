package awais.instagrabber.asyncs.direct_messages;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class CommentAction extends AsyncTask<Void, Void, Boolean> {
    private final String text;
    private final String threadId;

    private OnTaskCompleteListener listener;

    public CommentAction(String text, String threadId) {
        this.text = text;
        this.threadId = threadId;
    }

    protected Boolean doInBackground(Void... lmao) {
        boolean ok = false;
        final String url2 = "https://i.instagram.com/api/v1/direct_v2/threads/broadcast/text/";
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        try {
            final HttpURLConnection urlConnection2 = (HttpURLConnection) new URL(url2).openConnection();
            urlConnection2.setRequestMethod("POST");
            urlConnection2.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            urlConnection2.setUseCaches(false);
            final String commentText = URLEncoder.encode(text, "UTF-8")
                    .replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
            final String cc = UUID.randomUUID().toString();
            final String urlParameters2 = Utils.sign("{\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                    + "\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                    + "\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                    + "\",\"client_context\":\"" + cc
                    + "\",\"mutation_token\":\"" + cc
                    + "\",\"text\":\"" + commentText
                    + "\",\"thread_ids\":\"[" + threadId
                    + "]\",\"action\":\"send_item\"}");
            urlConnection2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection2.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters2.getBytes().length));
            urlConnection2.setDoOutput(true);
            DataOutputStream wr2 = new DataOutputStream(urlConnection2.getOutputStream());
            wr2.writeBytes(urlParameters2);
            wr2.flush();
            wr2.close();
            urlConnection2.connect();
            Log.d("austin_debug", urlConnection2.getResponseCode() + " " + urlParameters2 + " " + cookie);
            if (urlConnection2.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ok = true;
            }
            urlConnection2.disconnect();
        } catch (Throwable ex) {
            Log.e("austin_debug", "dm send: " + ex);
        }
        return ok;
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if (listener != null) {
            listener.onTaskComplete(result);
        }
    }

    public void setOnTaskCompleteListener(final OnTaskCompleteListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(boolean ok);
    }
}
