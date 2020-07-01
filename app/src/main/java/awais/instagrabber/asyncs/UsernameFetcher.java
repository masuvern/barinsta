package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class UsernameFetcher extends AsyncTask<Void, Void, String> {
    private final FetchListener<String> fetchListener;
    private final String uid;

    public UsernameFetcher(final String uid, final FetchListener<String> fetchListener) {
        this.uid = uid;
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected String doInBackground(final Void... voids) {
        String result = null;

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://i.instagram.com/api/v1/users/" + uid + "/info/").openConnection();
            conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
            conn.setUseCaches(true);

            final JSONObject user;
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK &&
                    (user = new JSONObject(Utils.readFromConnection(conn)).optJSONObject(Constants.EXTRAS_USER)) != null)
                result = user.getString(Constants.EXTRAS_USERNAME);

            conn.disconnect();
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final String result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}
