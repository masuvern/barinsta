package awais.instagrabber.utils;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;

public final class UpdateChecker extends AsyncTask<Void, Void, Boolean> {
    private final FetchListener<String> fetchListener;
    private String version;

    public UpdateChecker(final FetchListener<String> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @NonNull
    @Override
    protected Boolean doInBackground(final Void... voids) {
        try {
            version = "";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL("https://f-droid.org/api/v1/packages/me.austinhuang.instagrabber").openConnection();
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", "https://Barinsta.AustinHuang.me / mailto:Barinsta@AustinHuang.me");
            conn.connect();

            final int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                final JSONObject data = new JSONObject(NetworkUtils.readFromConnection(conn));
                if (BuildConfig.VERSION_CODE < data.getInt("suggestedVersionCode")) {
                    version = data.getJSONArray("packages").getJSONObject(0).getString("versionName");
                    return true;
                }
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if (result != null && result && fetchListener != null)
            fetchListener.onResult("v"+version);
    }
}
