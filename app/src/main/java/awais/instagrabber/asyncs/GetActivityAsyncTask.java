package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public class GetActivityAsyncTask extends AsyncTask<Void, Void, GetActivityAsyncTask.NotificationCounts> {
    private static final String TAG = "GetActivityAsyncTask";
    private String uid;
    private String cookie;
    private OnTaskCompleteListener onTaskCompleteListener;

    public GetActivityAsyncTask(final String uid, final String cookie, final OnTaskCompleteListener onTaskCompleteListener) {
        this.uid = uid;
        this.cookie = cookie;
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    protected NotificationCounts doInBackground(Void... voids) {
        if (Utils.isEmpty(cookie)) {
            return null;
        }
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
            final JSONObject data = new JSONObject(Utils.readFromConnection(urlConnection)).getJSONObject("data")
                    .getJSONObject("user").getJSONObject("edge_activity_count").getJSONArray("edges").getJSONObject(0)
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
        if (onTaskCompleteListener == null) {
            return;
        }
        onTaskCompleteListener.onTaskComplete(result);
    }

    public static class NotificationCounts {
        private int relationshipsCount;
        private int userTagsCount;
        private int commentsCount;
        private int commentLikesCount;
        private int likesCount;

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
    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final NotificationCounts result);
    }
}
