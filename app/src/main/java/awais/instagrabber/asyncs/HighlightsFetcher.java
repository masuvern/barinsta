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
    private final FetchListener<HighlightModel[]> fetchListener;

    public HighlightsFetcher(final String id, final FetchListener<HighlightModel[]> fetchListener) {
        this.id = id;
        this.fetchListener = fetchListener;
    }

    @Override
    protected HighlightModel[] doInBackground(final Void... voids) {
        HighlightModel[] result = null;
        String url = "https://www.instagram.com/graphql/query/?query_hash=7c16654f22c819fb63d1183034a5162f&variables=" +
                "{\"user_id\":\"" + id + "\",\"include_chaining\":false,\"include_reel\":true,\"include_suggested_users\":false," +
                "\"include_logged_out_extras\":false,\"include_highlight_reels\":true}";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONArray highlightsReel = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("data")
                        .getJSONObject(Constants.EXTRAS_USER).getJSONObject("edge_highlight_reels").getJSONArray("edges");

                final int length = highlightsReel.length();
                final HighlightModel[] highlightModels = new HighlightModel[length];
                final String[] highlightIds = new String[length];
                for (int i = 0; i < length; ++i) {
                    final JSONObject highlightNode = highlightsReel.getJSONObject(i).getJSONObject("node");
                    final String id = highlightNode.getString(Constants.EXTRAS_ID);
                    highlightIds[i] = id;
                    highlightModels[i] = new HighlightModel(
                            highlightNode.getString("title"),
                            highlightNode.getJSONObject("cover_media").getString("thumbnail_src")
                    );
                }

                conn.disconnect();

                // a22a50ce4582220909e302d6eb84d259
                // 45246d3fe16ccc6577e0bd297a5db1ab
                url = "https://www.instagram.com/graphql/query/?query_hash=a22a50ce4582220909e302d6eb84d259&variables=" +
                        "{\"highlight_reel_ids\":" + Utils.highlightIdsMerger(highlightIds) + ",\"reel_ids\":[],\"location_ids\":[],\"precomposed_overlay\":false}";
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setUseCaches(false);
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Utils.putHighlightModels(conn, highlightModels);
                }

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