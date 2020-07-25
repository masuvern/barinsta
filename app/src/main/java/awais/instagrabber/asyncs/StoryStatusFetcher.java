package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class StoryStatusFetcher extends AsyncTask<Void, Void, StoryModel[]> {
    private final String id, hashtag;
    private final FetchListener<StoryModel[]> fetchListener;

    public StoryStatusFetcher(final String id, final String hashtag, final FetchListener<StoryModel[]> fetchListener) {
        this.id = id;
        this.hashtag = hashtag;
        this.fetchListener = fetchListener;
    }

    @Override
    protected StoryModel[] doInBackground(final Void... voids) {
        StoryModel[] result = null;
        final String url = "https://www.instagram.com/graphql/query/?query_hash=52a36e788a02a3c612742ed5146f1676&variables=" +
                "{\"precomposed_overlay\":false,\"reel_ids\":[\"" + id + "\"]"
                +(!Utils.isEmpty(hashtag) ? (",\"tag_names\":\""+hashtag+"\"") : "")+"}";

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject data = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("data");

                JSONArray media;
                if ((media = data.optJSONArray("reels_media")) != null && media.length() > 0 &&
                        (data = media.optJSONObject(0)) != null &&
                        (media = data.optJSONArray("items")) != null) {

                    final int mediaLen = media.length();

                    final StoryModel[] models = new StoryModel[mediaLen];
                    for (int i = 0; i < mediaLen; ++i) {
                        data = media.getJSONObject(i);
                        final boolean isVideo = data.getBoolean("is_video");

                        final JSONArray tappableObjects = data.optJSONArray("tappable_objects");
                        final int tappableLength = tappableObjects != null ? tappableObjects.length() : 0;

                        models[i] = new StoryModel(data.getString(Constants.EXTRAS_ID),
                                data.getString("display_url"),
                                isVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                data.optLong("taken_at_timestamp", 0),
                                data.getJSONObject("owner").getString("username"));

                        final JSONArray videoResources = data.optJSONArray("video_resources");
                        if (isVideo && videoResources != null)
                            models[i].setVideoUrl(Utils.getHighQualityPost(videoResources, true));

                        for (int j = 0; j < tappableLength; ++j) {
                            JSONObject tappableObject = tappableObjects.getJSONObject(j);
                            if (tappableObject.optString("__typename").equals("GraphTappableFeedMedia")) {
                                tappableObject = tappableObject.getJSONObject("media");
                                models[i].setTappableShortCode(tappableObject.getString(Constants.EXTRAS_SHORTCODE));
                                break;
                            }
                        }
                    }
                    result = models;
                }
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_STORY_STATUS_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final StoryModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}