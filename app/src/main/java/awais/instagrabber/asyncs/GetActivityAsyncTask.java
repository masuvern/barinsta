package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.TextUtils;

public class GetActivityAsyncTask extends AsyncTask<String, Void, GetActivityAsyncTask.NotificationCounts> {
    private static final String TAG = "GetActivityAsyncTask";

    private final OnTaskCompleteListener onTaskCompleteListener;

    public GetActivityAsyncTask(final OnTaskCompleteListener onTaskCompleteListener) {
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    /*
    This needs to be redone to fetch i inbox instead
    Within inbox, data is (body JSON => counts)
    Then we have these counts:
    new_posts, activity_feed_dot_badge, relationships, campaign_notification
    usertags, likes, comment_likes, shopping_notification, comments
    photos_of_you (not sure about difference to usertags), requests
     */

    protected NotificationCounts doInBackground(final String... cookiesArray) {
        if (cookiesArray == null) return null;
        final String cookie = cookiesArray[0];
        if (TextUtils.isEmpty(cookie)) return null;
        final String uid = CookieUtils.getUserIdFromCookie(cookie);
        final String url = "https://www.instagram.com/graphql/query/?query_hash=0f318e8cfff9cc9ef09f88479ff571fb"
                + "&variables={\"id\":\"" + uid + "\"}";
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
            urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
            urlConnection.connect();
            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            final JSONObject data = new JSONObject(NetworkUtils.readFromConnection(urlConnection))
                    .getJSONObject("data")
                    .getJSONObject("user")
                    .getJSONObject("edge_activity_count")
                    .getJSONArray("edges")
                    .getJSONObject(0)
                    .getJSONObject("node");
            return new NotificationCounts(
                    data.getInt("relationships"),
                    data.getInt("usertags"),
                    data.getInt("comments"),
                    data.getInt("comment_likes"),
                    data.getInt("likes")
            );
        } catch (Throwable ex) {
            Log.e(TAG, "Error", ex);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final NotificationCounts result) {
        if (onTaskCompleteListener == null) return;
        onTaskCompleteListener.onTaskComplete(result);
    }

    public static class NotificationCounts {
        private final int relationshipsCount;
        private final int userTagsCount;
        private final int commentsCount;
        private final int commentLikesCount;
        private final int likesCount;

        public NotificationCounts(final int relationshipsCount,
                                  final int userTagsCount,
                                  final int commentsCount,
                                  final int commentLikesCount,
                                  final int likesCount) {
            this.relationshipsCount = relationshipsCount;
            this.userTagsCount = userTagsCount;
            this.commentsCount = commentsCount;
            this.commentLikesCount = commentLikesCount;
            this.likesCount = likesCount;
        }

        public int getRelationshipsCount() {
            return relationshipsCount;
        }

        public int getUserTagsCount() {
            return userTagsCount;
        }

        public int getCommentsCount() {
            return commentsCount;
        }

        public int getCommentLikesCount() {
            return commentLikesCount;
        }

        public int getLikesCount() {
            return likesCount;
        }

        @Override
        public String toString() {
            return "NotificationCounts{" +
                    "relationshipsCount=" + relationshipsCount +
                    ", userTagsCount=" + userTagsCount +
                    ", commentsCount=" + commentsCount +
                    ", commentLikesCount=" + commentLikesCount +
                    ", likesCount=" + likesCount +
                    '}';
        }
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final NotificationCounts result);
    }
}
