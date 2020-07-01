package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FollowModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class FollowFetcher extends AsyncTask<Void, Void, FollowModel[]> {
    private final String endCursor, id;
    private final boolean isFollowers;
    private final FetchListener<FollowModel[]> fetchListener;

    public FollowFetcher(final String id, final boolean isFollowers, final FetchListener<FollowModel[]> fetchListener) {
        this.id = id;
        this.endCursor = "";
        this.isFollowers = isFollowers;
        this.fetchListener = fetchListener;
    }

    public FollowFetcher(final String id, final boolean isFollowers, final String endCursor, final FetchListener<FollowModel[]> fetchListener) {
        this.id = id;
        this.endCursor = endCursor == null ? "" : endCursor;
        this.isFollowers = isFollowers;
        this.fetchListener = fetchListener;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected FollowModel[] doInBackground(final Void... voids) {
        FollowModel[] result = null;
        final String url = "https://www.instagram.com/graphql/query/?query_id=" + (isFollowers ? "17851374694183129" : "17874545323001329")
                + "&id=" + id + "&first=50&after=" + endCursor;

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject data = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("data")
                        .getJSONObject(Constants.EXTRAS_USER).getJSONObject(isFollowers ? "edge_followed_by" : "edge_follow");

                final String endCursor;
                final boolean hasNextPage;

                final JSONObject pageInfo = data.getJSONObject("page_info");
                if (pageInfo.has("has_next_page")) {
                    hasNextPage = pageInfo.getBoolean("has_next_page");
                    endCursor = hasNextPage ? pageInfo.getString("end_cursor") : null;
                } else {
                    hasNextPage = false;
                    endCursor = null;
                }

                final JSONArray edges = data.getJSONArray("edges");
                final FollowModel[] models = new FollowModel[edges.length()];
                for (int i = 0; i < models.length; ++i) {
                    final JSONObject followNode = edges.getJSONObject(i).getJSONObject("node");
                    models[i] = new FollowModel(followNode.getString(Constants.EXTRAS_ID), followNode.getString(Constants.EXTRAS_USERNAME),
                            followNode.getString("full_name"), followNode.getString("profile_pic_url"));
                }

                if (models[models.length - 1] != null)
                    models[models.length - 1].setPageCursor(hasNextPage, endCursor);

                result = models;
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_FOLLOW_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final FollowModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}