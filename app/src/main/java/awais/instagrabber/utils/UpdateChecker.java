package awais.instagrabber.utils;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

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
        final String UPDATE_BASE_URL = "https://github.com/austinhuang0131/instagrabber/releases/tag/";
        final String versionName = BuildConfig.VERSION_NAME;
        final int index = versionName.indexOf('.');

        try {
            final int verMajor = Integer.parseInt(versionName.substring(0, index));

            version = "v" + (verMajor + 1) + ".0";

            // check major version first
            HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_BASE_URL + version).openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("HEAD");
            conn.connect();

            final int responseCode = conn.getResponseCode();
            conn.disconnect();

            if (responseCode == HttpURLConnection.HTTP_OK) return true;
            else {
                final String substring = versionName.substring(index + 1);
                final int verMinor = Integer.parseInt(substring) + 1;

                for (int i = verMinor; i < 10; ++i) {
                    version = "v" + verMajor + '.' + i;
                    conn.disconnect();

                    conn = (HttpURLConnection) new URL(UPDATE_BASE_URL + version).openConnection();
                    conn.setUseCaches(false);
                    conn.setRequestMethod("HEAD");
                    conn.connect();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        conn.disconnect();
                        return true;
                    }
                }
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if (result != null && result && fetchListener != null)
            fetchListener.onResult(version);
    }
}