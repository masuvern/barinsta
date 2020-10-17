package awais.instagrabber.asyncs.i;

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

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.logCollector;

public final class iPostFetcher extends AsyncTask<Void, Void, FeedModel> {
    private static final String TAG = "iPostFetcher";

    private final String id;
    private final FetchListener<FeedModel> fetchListener;

    public iPostFetcher(final String id, final FetchListener<FeedModel> fetchListener) {
        this.id = id;
        this.fetchListener = fetchListener;
    }

    @Override
    protected FeedModel doInBackground(final Void... voids) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://i.instagram.com/api/v1/media/" + id + "/info").openConnection();
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject media = new JSONObject(NetworkUtils.readFromConnection(conn))
                        .getJSONArray("items")
                        .getJSONObject(0);
                ProfileModel profileModel = null;
                if (media.has("user")) {
                    final JSONObject user = media.getJSONObject("user");
                    final JSONObject friendshipStatus = user.optJSONObject("friendship_status");
                    boolean following = false;
                    boolean isRestricted = false;
                    boolean outgoingRequest = false;
                    if (friendshipStatus != null) {
                        following = friendshipStatus.optBoolean("following");
                        isRestricted = friendshipStatus.optBoolean("is_restricted");
                        outgoingRequest = friendshipStatus.optBoolean("outgoing_request");
                    }
                    profileModel = new ProfileModel(
                            user.optBoolean("is_private"),
                            user.optBoolean("is_private"),
                            user.optBoolean("is_verified"),
                            user.optString("pk"),
                            user.getString(Constants.EXTRAS_USERNAME),
                            user.optString("fullname"),
                            null,
                            null,
                            user.getString("profile_pic_url"),
                            null,
                            -1,
                            -1,
                            -1,
                            following,
                            isRestricted,
                            false,
                            outgoingRequest
                    );
                }
                if (profileModel == null) {
                    return new FeedModel.Builder().build();
                }

                // to check if file exists
                final boolean shouldDownloadToUserFolder = Utils.settingsHelper.getBoolean(DOWNLOAD_USER_FOLDER);
                final File downloadDir = new File(
                        Environment.getExternalStorageDirectory(),
                        "Download" + (shouldDownloadToUserFolder ? "/" + profileModel.getUsername() : ""));
                File customDir = null;
                if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
                    final String customPath = Utils.settingsHelper.getString(FOLDER_PATH)
                            + (shouldDownloadToUserFolder ? "/" + profileModel.getUsername() : "");
                    if (!TextUtils.isEmpty(customPath)) customDir = new File(customPath);
                }

                final long timestamp = media.getLong("taken_at");

                final boolean isVideo = media.has("has_audio") && media.optBoolean("has_audio");
                final boolean isSlider = !media.isNull("carousel_media_count");

                final MediaItemType mediaItemType;
                if (isSlider) mediaItemType = MediaItemType.MEDIA_TYPE_SLIDER;
                else if (isVideo) mediaItemType = MediaItemType.MEDIA_TYPE_VIDEO;
                else mediaItemType = MediaItemType.MEDIA_TYPE_IMAGE;

                final String postCaption;
                final JSONObject mediaToCaption = media.optJSONObject("caption");
                if (mediaToCaption == null) postCaption = null;
                else postCaption = mediaToCaption.optString("text");

                final long commentsCount = media.optLong("comment_count");

                final JSONObject location = media.optJSONObject("location");
                String locationId = null;
                String locationName = null;
                if (location != null) {
                    locationName = location.optString("name");
                    if (location.has("id")) {
                        locationId = location.getString("id");
                    } else if (location.has("pk")) {
                        locationId = location.getString("pk");
                    }
                }
                // final String locationString = location.optString("id") + "/" + location.optString("slug");
                final FeedModel.Builder feedModelBuilder = new FeedModel.Builder()
                        .setItemType(mediaItemType)
                        .setPostId(media.getString(Constants.EXTRAS_ID))
                        .setDisplayUrl(isVideo ? ResponseBodyUtils.getHighQualityPost(media.optJSONArray("video_versions"), true, true, false)
                                               : ResponseBodyUtils.getHighQualityImage(media))
                        .setShortCode(media.getString("code"))
                        .setPostCaption(TextUtils.isEmpty(postCaption) ? null : postCaption)
                        .setProfileModel(profileModel)
                        .setViewCount(isVideo && media.has("view_count")
                                      ? media.getLong("view_count")
                                      : -1)
                        .setTimestamp(timestamp)
                        .setLiked(media.optBoolean("has_liked"))
                        .setBookmarked(media.optBoolean("has_viewer_saved"))
                        .setLikesCount(media.getLong("like_count"))
                        .setLocationName(locationName)
                        .setLocationId(locationId)
                        .setCommentsCount(commentsCount);
                // DownloadUtils.checkExistence(downloadDir, customDir, false, postModel);

                if (isSlider) {
                    final JSONArray children = media.getJSONArray("carousel_media");
                    final List<PostChild> postModels = new ArrayList<>();
                    for (int i = 0; i < children.length(); ++i) {
                        final JSONObject childNode = children.getJSONObject(i);
                        final boolean isChildVideo = childNode.has("video_duration");
                        postModels.add(new PostChild.Builder()
                                               .setItemType(isChildVideo ? MediaItemType.MEDIA_TYPE_VIDEO
                                                                         : MediaItemType.MEDIA_TYPE_IMAGE)
                                               .setPostId(childNode.has(Constants.EXTRAS_ID)
                                                          ? childNode.getString(Constants.EXTRAS_ID)
                                                          : media.getString(Constants.EXTRAS_ID))
                                               // .setShortCode(childNode.optString(Constants.EXTRAS_SHORTCODE))
                                               .setDisplayUrl(
                                                       isChildVideo ? ResponseBodyUtils.getHighQualityPost(
                                                               childNode.optJSONArray("video_versions"), true, true, false)
                                                                    : ResponseBodyUtils.getHighQualityImage(childNode))
                                               .setVideoViews(isChildVideo && childNode.has("video_view_count")
                                                              ? childNode.getLong("video_view_count")
                                                              : -1)
                                               .build());
                        // DownloadUtils.checkExistence(downloadDir, customDir, true, postModels[i]);
                    }
                    feedModelBuilder.setSliderItems(postModels);
                }
                return feedModelBuilder.build();
            }
        } catch (Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_POST_FETCHER, "doInBackground (i)");
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "", e);
            }
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
