package awais.instagrabber.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;

public final class UpdateChecker extends AsyncTask<Void, Void, String> {
    private static final String TAG = "UpdateChecker";

    private final FetchListener<String> fetchListener;
    private String version;

    public UpdateChecker(final FetchListener<String> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected String doInBackground(final Void... voids) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://github.com/austinhuang0131/instagrabber/releases/latest").openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", Constants.A_USER_AGENT);
            conn.connect();

            final int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                version = conn.getHeaderField("Location").split("/v")[1];
                return version;
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final String result) {
        if (result == null || fetchListener == null) {
            return;
        }
        fetchListener.onResult(version);
    }
}