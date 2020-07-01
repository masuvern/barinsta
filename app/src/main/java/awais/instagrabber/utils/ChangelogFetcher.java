package awais.instagrabber.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;

public final class ChangelogFetcher extends AsyncTask<Void, Void, String> {
    private final FetchListener<CharSequence> fetchListener;

    public ChangelogFetcher(final FetchListener<CharSequence> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected String doInBackground(final Void... voids) {
        String result = null;
        final String changelogUrl = "https://gitlab.com/AwaisKing/instagrabber/-/raw/master/CHANGELOG";

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(changelogUrl).openConnection();
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result = Utils.readFromConnection(conn);
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final String result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}