package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.FriendshipStatus;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.TextUtils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class ProfileFetcher extends AsyncTask<Void, Void, User> {
    private static final String TAG = ProfileFetcher.class.getSimpleName();

    private final FetchListener<User> fetchListener;
    private final String userName;

    public ProfileFetcher(String userName, FetchListener<User> fetchListener) {
        this.userName = userName;
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected User doInBackground(final Void... voids) {
        User result = null;

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://www.instagram.com/" + userName + "/?__a=1").openConnection();
            conn.setUseCaches(true);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final String json = NetworkUtils.readFromConnection(conn);
                // Log.d(TAG, "doInBackground: " + json);
                final JSONObject userJson = new JSONObject(json).getJSONObject("graphql")
                                                                .getJSONObject(Constants.EXTRAS_USER);

                final String cookie = settingsHelper.getString(Constants.COOKIE);

                boolean isPrivate = userJson.getBoolean("is_private");
                final long id = userJson.optLong(Constants.EXTRAS_ID, 0);
                final long uid = CookieUtils.getUserIdFromCookie(cookie);
                final JSONObject timelineMedia = userJson.getJSONObject("edge_owner_to_timeline_media");
                // if (timelineMedia.has("edges")) {
                //     final JSONArray edges = timelineMedia.getJSONArray("edges");
                // }

                String url = userJson.optString("external_url");
                if (TextUtils.isEmpty(url)) url = null;

                return new User(
                        id,
                        userName,
                        userJson.getString("full_name"),
                        isPrivate,
                        userJson.getString("profile_pic_url_hd"),
                        null,
                        new FriendshipStatus(
                                userJson.optBoolean("followed_by_viewer"),
                                userJson.optBoolean("follows_viewer"),
                                userJson.optBoolean("blocked_by_viewer"),
                                false,
                                isPrivate,
                                false,
                                userJson.optBoolean("restricted_by_viewer"),
                                false,
                                userJson.optBoolean("restricted_by_viewer"),
                                false
                        ),
                        userJson.getBoolean("is_verified"),
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        timelineMedia.getLong("count"),
                        userJson.getJSONObject("edge_followed_by").getLong("count"),
                        userJson.getJSONObject("edge_follow").getLong("count"),
                        0,
                        userJson.getString("biography"),
                        url,
                        0,
                        null);

                // result = new ProfileModel(isPrivate,
                //                           !user.optBoolean("followed_by_viewer") && (id != uid && isPrivate),
                //                           user.getBoolean("is_verified"),
                //                           id,
                //                           userName,
                //                           user.getString("full_name"),
                //                           user.getString("biography"),
                //                           url,
                //                           user.getString("profile_pic_url"),
                //                           user.getString("profile_pic_url_hd"),
                //                           timelineMedia.getLong("count"),
                //                           user.getJSONObject("edge_followed_by").getLong("count"),
                //                           user.getJSONObject("edge_follow").getLong("count"),
                //                           user.optBoolean("followed_by_viewer"),
                //                           user.optBoolean("follows_viewer"),
                //                           user.optBoolean("restricted_by_viewer"),
                //                           user.optBoolean("blocked_by_viewer"),
                //                           user.optBoolean("requested_by_viewer"));
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_PROFILE_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(final User result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}
