package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class HighlightsFetcher extends AsyncTask<Void, Void, HighlightModel[]> {
    private final String id;
    private final boolean storiesig;
    private final FetchListener<HighlightModel[]> fetchListener;

    public HighlightsFetcher(final String id, final boolean storiesig, final FetchListener<HighlightModel[]> fetchListener) {
        this.id = id;
        this.storiesig = storiesig;
        this.fetchListener = fetchListener;
    }

    @Override
    protected HighlightModel[] doInBackground(final Void... voids) {
        HighlightModel[] result = null;
        String url = "https://" + (storiesig ? "storiesig" : "i.instagram") + ".com/api/v1/highlights/" + id + "/highlights_tray/";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", storiesig ? Constants.A_USER_AGENT : Constants.I_USER_AGENT);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONArray highlightsReel = new JSONObject(Utils.readFromConnection(conn)).getJSONArray("tray");

                final int length = highlightsReel.length();
                final HighlightModel[] highlightModels = new HighlightModel[length];
                final String[] highlightIds = new String[length];
                for (int i = 0; i < length; ++i) {
                    final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                    highlightModels[i] = new HighlightModel(
                            highlightNode.getString("title"),
                            highlightNode.getString(Constants.EXTRAS_ID),
                            highlightNode.getJSONObject("cover_media").getJSONObject("cropped_image_version").getString("url")
                    );
                }

                conn.disconnect();
                result = highlightModels;
            }

            conn.disconnect();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final HighlightModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}