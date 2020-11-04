package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.logCollector;

public final class PostFetcher extends AsyncTask<Void, Void, FeedModel> {
    private static final String TAG = "PostFetcher";

    private final String shortCode;
    private final FetchListener<FeedModel> fetchListener;

    public PostFetcher(final String shortCode, final FetchListener<FeedModel> fetchListener) {
        this.shortCode = shortCode;
        this.fetchListener = fetchListener;
    }

    @Override
    protected FeedModel doInBackground(final Void... voids) {
        CookieUtils.setupCookies(Utils.settingsHelper.getString(Constants.COOKIE)); // <- direct download
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://www.instagram.com/p/" + shortCode + "/?__a=1").openConnection();
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                final JSONObject media = new JSONObject(NetworkUtils.readFromConnection(conn)).getJSONObject("graphql")
                                                                                              .getJSONObject("shortcode_media");
                ProfileModel profileModel = null;
                if (media.has("owner")) {
                    final JSONObject owner = media.getJSONObject("owner");
                    profileModel = new ProfileModel(
                            owner.optBoolean("is_private"),
                            owner.optBoolean("is_private"),
                            owner.optBoolean("is_verified"),
                            owner.optString("id"),
                            owner.optString("username"),
                            owner.optString("full_name"),
                            null,
                            null,
                            owner.optString("profile_pic_url"),
                            owner.optString("profile_pic_url"),
                            owner.optInt("edge_owner_to_timeline_media"),
                            owner.optInt("edge_followed_by"),
                            -1,
                            owner.optBoolean("followed_by_viewer"),
                            owner.optBoolean("restricted_by_viewer"),
                            owner.optBoolean("blocked_by_viewer"),
                            owner.optBoolean("requested_by_viewer")
                    );
                }
                final String username = profileModel == null ? "" : profileModel.getUsername();
                // to check if file exists
                final File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download" +
                        (Utils.settingsHelper.getBoolean(DOWNLOAD_USER_FOLDER) ? ("/" + username) : ""));
                File customDir = null;
                if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
                    final String customPath = Utils.settingsHelper.getString(FOLDER_PATH +
                                                                                     (Utils.settingsHelper.getBoolean(DOWNLOAD_USER_FOLDER)
                                                                                      ? ("/" + username)
                                                                                      : ""));
                    if (!TextUtils.isEmpty(customPath)) customDir = new File(customPath);
                }

                final long timestamp = media.getLong("taken_at_timestamp");

                final boolean isVideo = media.has("is_video") && media.optBoolean("is_video");
                final boolean isSlider = media.has("edge_sidecar_to_children");

                final MediaItemType mediaItemType;
                if (isSlider) mediaItemType = MediaItemType.MEDIA_TYPE_SLIDER;
                else if (isVideo) mediaItemType = MediaItemType.MEDIA_TYPE_VIDEO;
                else mediaItemType = MediaItemType.MEDIA_TYPE_IMAGE;

                final String postCaption;
                final JSONObject mediaToCaption = media.optJSONObject("edge_media_to_caption");
                if (mediaToCaption == null) postCaption = null;
                else {
                    final JSONArray captions = mediaToCaption.optJSONArray("edges");
                    postCaption = captions != null && captions.length() > 0 ?
                                  captions.getJSONObject(0).getJSONObject("node").optString("text") : null;
                }

                JSONObject commentObject = media.optJSONObject("edge_media_to_parent_comment");
                final long commentsCount = commentObject != null ? commentObject.optLong("count") : 0;
                final FeedModel.Builder feedModelBuilder = new FeedModel.Builder()
                        .setItemType(mediaItemType)
                        .setPostId(media.getString(Constants.EXTRAS_ID))
                        .setDisplayUrl(isVideo ? media.getString("video_url")
                                               : ResponseBodyUtils.getHighQualityImage(media))
                        .setShortCode(shortCode)
                        .setPostCaption(TextUtils.isEmpty(postCaption) ? null : postCaption)
                        .setProfileModel(profileModel)
                        .setViewCount(isVideo && media.has("video_view_count")
                                      ? media.getLong("video_view_count")
                                      : -1)
                        .setTimestamp(timestamp)
                        .setLiked(media.getBoolean("viewer_has_liked"))
                        .setBookmarked(media.getBoolean("viewer_has_saved"))
                        .setLikesCount(media.getJSONObject("edge_media_preview_like")
                                            .getLong("count"))
                        .setLocationName(media.isNull("location")
                                         ? null
                                         : media.getJSONObject("location").optString("name"))
                        .setLocationId(media.isNull("location")
                                       ? null
                                       : media.getJSONObject("location").optString("id"))
                        .setCommentsCount(commentsCount);
                // DownloadUtils.checkExistence(downloadDir, customDir, false, feedModelBuilder);
                if (isSlider) {
                    final JSONArray children = media.getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
                    final List<PostChild> postModels = new ArrayList<>();
                    for (int i = 0; i < children.length(); ++i) {
                        final JSONObject childNode = children.getJSONObject(i).getJSONObject("node");
                        final boolean isChildVideo = childNode.getBoolean("is_video");
                        postModels.add(new PostChild.Builder()
                                               .setItemType(isChildVideo ? MediaItemType.MEDIA_TYPE_VIDEO
                                                                         : MediaItemType.MEDIA_TYPE_IMAGE)
                                               .setDisplayUrl(isChildVideo ? childNode.getString("video_url")
                                                                           : childNode.getString("display_url"))
                                               .setShortCode(media.getString(Constants.EXTRAS_SHORTCODE))
                                               .setVideoViews(isChildVideo && childNode.has("video_view_count")
                                                              ? childNode.getLong("video_view_count")
                                                              : -1)
                                               .setThumbnailUrl(childNode.getString("display_url"))
                                               .setHeight(childNode.getJSONObject("dimensions").getInt("height"))
                                               .setWidth(childNode.getJSONObject("dimensions").getInt("width"))
                                               .build());
                        // DownloadUtils.checkExistence(downloadDir, customDir, true, postModels.get(i));
                    }
                    feedModelBuilder.setSliderItems(postModels);
                }
                return feedModelBuilder.build();
            }
        } catch (Exception e) {
            if (logCollector != null) {
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_POST_FETCHER, "doInBackground");
            }
            Log.e(TAG, "Error fetching post", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final FeedModel feedModel) {
        if (fetchListener != null) fetchListener.onResult(feedModel);
    }
}
