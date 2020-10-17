package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class FeedFetcher extends AsyncTask<Void, Void, List<FeedModel>> {
    private static final String TAG = "FeedFetcher";

    private static final int maxItemsToLoad = 25; // max is 50, but that's too many posts
    private final String endCursor;
    private final FetchListener<List<FeedModel>> fetchListener;

    public FeedFetcher(final FetchListener<List<FeedModel>> fetchListener) {
        this.endCursor = "";
        this.fetchListener = fetchListener;
    }

    public FeedFetcher(final String endCursor, final FetchListener<List<FeedModel>> fetchListener) {
        this.endCursor = endCursor == null ? "" : endCursor;
        this.fetchListener = fetchListener;
    }

    @Override
    protected final List<FeedModel> doInBackground(final Void... voids) {
        final List<FeedModel> result = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        try {
            //
            //  stories: 04334405dbdef91f2c4e207b84c204d7 && https://i.instagram.com/api/v1/feed/reels_tray/
            //  https://www.instagram.com/graphql/query/?query_hash=04334405dbdef91f2c4e207b84c204d7&variables={"only_stories":true,"stories_prefetch":false,"stories_video_dash_manifest":false}
            //  ///////////////////////////////////////////////
            //  feed:
            //  https://www.instagram.com/graphql/query/?query_hash=6b838488258d7a4820e48d209ef79eb1&variables=
            //    {"cached_feed_item_ids":[],"fetch_media_item_count":12,"fetch_media_item_cursor":"<end_cursor>","fetch_comment_count":4,"fetch_like":3,"has_stories":false,"has_threaded_comments":true}
            //  only used: fetch_media_item_cursor, fetch_media_item_count: 100 (max 50), has_threaded_comments = true
            //  //////////////////////////////////////////////
            //  more unknowns: https://github.com/qsniyg/rssit/blob/master/rssit/generators/instagram.py
            //

            final String url = "https://www.instagram.com/graphql/query/?query_hash=6b838488258d7a4820e48d209ef79eb1&variables=" +
                    "{\"fetch_media_item_count\":" + maxItemsToLoad + ",\"has_threaded_comments\":true,\"fetch_media_item_cursor\":\"" + endCursor + "\"}";
            urlConnection = (HttpURLConnection) new URL(url).openConnection();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final String json = NetworkUtils.readFromConnection(urlConnection);
                // Log.d(TAG, json);
                final JSONObject timelineFeed = new JSONObject(json).getJSONObject("data")
                                                                    .getJSONObject(Constants.EXTRAS_USER)
                                                                    .getJSONObject("edge_web_feed_timeline");

                final String endCursor;
                final boolean hasNextPage;

                final JSONObject pageInfo = timelineFeed.getJSONObject("page_info");
                if (pageInfo.has("has_next_page")) {
                    hasNextPage = pageInfo.getBoolean("has_next_page");
                    endCursor = hasNextPage ? pageInfo.getString("end_cursor") : null;
                } else {
                    hasNextPage = false;
                    endCursor = null;
                }

                final JSONArray feedItems = timelineFeed.getJSONArray("edges");

                for (int i = 0; i < feedItems.length(); ++i) {
                    final JSONObject feedItem = feedItems.getJSONObject(i).getJSONObject("node");
                    final String mediaType = feedItem.optString("__typename");
                    if (mediaType.isEmpty() || "GraphSuggestedUserFeedUnit".equals(mediaType))
                        continue;

                    final boolean isVideo = feedItem.optBoolean("is_video");
                    final long videoViews = feedItem.optLong("video_view_count", 0);

                    final String displayUrl = feedItem.optString("display_url");
                    if (TextUtils.isEmpty(displayUrl)) continue;
                    final String resourceUrl;

                    if (isVideo) {
                        resourceUrl = feedItem.getString("video_url");
                    } else {
                        resourceUrl = feedItem.has("display_resources") ? ResponseBodyUtils.getHighQualityImage(feedItem) : displayUrl;
                    }

                    ProfileModel profileModel = null;
                    if (feedItem.has("owner")) {
                        final JSONObject owner = feedItem.getJSONObject("owner");
                        profileModel = new ProfileModel(
                                owner.optBoolean("is_private"),
                                false, // if you can see it then you def follow
                                owner.optBoolean("is_verified"),
                                owner.getString(Constants.EXTRAS_ID),
                                owner.getString(Constants.EXTRAS_USERNAME),
                                owner.optString("full_name"),
                                null,
                                null,
                                owner.getString("profile_pic_url"),
                                null,
                                0,
                                0,
                                0,
                                false,
                                false,
                                false,
                                false);
                    }
                    JSONObject tempJsonObject = feedItem.optJSONObject("edge_media_preview_comment");
                    final long commentsCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
                    tempJsonObject = feedItem.optJSONObject("edge_media_to_caption");
                    final JSONArray captions = tempJsonObject != null ? tempJsonObject.getJSONArray("edges") : null;
                    String captionText = null;
                    if (captions != null && captions.length() > 0) {
                        if ((tempJsonObject = captions.optJSONObject(0)) != null &&
                                (tempJsonObject = tempJsonObject.optJSONObject("node")) != null) {
                            captionText = tempJsonObject.getString("text");
                        }
                    }
                    final JSONObject location = feedItem.optJSONObject("location");
                    // Log.d(TAG, "location: " + (location == null ? null : location.toString()));
                    String locationId = null;
                    String locationName = null;
                    if (location != null) {
                        locationName = location.optString("name");
                        if (location.has("id")) {
                            locationId = location.getString("id");
                        } else if (location.has("pk")) {
                            locationId = location.getString("pk");
                        }
                        // Log.d(TAG, "locationId: " + locationId);
                    }
                    int height = 0;
                    int width = 0;
                    final JSONObject dimensions = feedItem.optJSONObject("dimensions");
                    if (dimensions != null) {
                        height = dimensions.optInt("height");
                        width = dimensions.optInt("width");
                    }
                    String thumbnailUrl = null;
                    try {
                        thumbnailUrl = feedItem.getJSONArray("display_resources")
                                               .getJSONObject(0)
                                               .getString("src");
                    } catch (JSONException ignored) {}
                    final FeedModel.Builder feedModelBuilder = new FeedModel.Builder()
                            .setProfileModel(profileModel)
                            .setItemType(isVideo ? MediaItemType.MEDIA_TYPE_VIDEO
                                                 : MediaItemType.MEDIA_TYPE_IMAGE)
                            .setViewCount(videoViews)
                            .setPostId(feedItem.getString(Constants.EXTRAS_ID))
                            .setDisplayUrl(resourceUrl)
                            .setThumbnailUrl(thumbnailUrl != null ? thumbnailUrl : displayUrl)
                            .setShortCode(feedItem.getString(Constants.EXTRAS_SHORTCODE))
                            .setPostCaption(captionText)
                            .setCommentsCount(commentsCount)
                            .setTimestamp(feedItem.optLong("taken_at_timestamp", -1))
                            .setLiked(feedItem.getBoolean("viewer_has_liked"))
                            .setBookmarked(feedItem.getBoolean("viewer_has_saved"))
                            .setLikesCount(feedItem.getJSONObject("edge_media_preview_like")
                                                   .getLong("count"))
                            .setLocationName(locationName)
                            .setLocationId(locationId)
                            .setImageHeight(height)
                            .setImageWidth(width);

                    final boolean isSlider = "GraphSidecar".equals(mediaType) && feedItem.has("edge_sidecar_to_children");

                    if (isSlider) {
                        feedModelBuilder.setItemType(MediaItemType.MEDIA_TYPE_SLIDER);
                        final JSONObject sidecar = feedItem.optJSONObject("edge_sidecar_to_children");
                        if (sidecar != null) {
                            final JSONArray children = sidecar.optJSONArray("edges");
                            if (children != null) {
                                final List<PostChild> sliderItems = getSliderItems(children);
                                feedModelBuilder.setSliderItems(sliderItems);
                            }
                        }
                    }
                    final FeedModel feedModel = feedModelBuilder.build();
                    result.add(feedModel);
                }
                if (!result.isEmpty() && result.get(result.size() - 1) != null) {
                    result.get(result.size() - 1).setPageCursor(hasNextPage, endCursor);
                }
            }
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_FEED_FETCHER, "doInBackground");
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "", e);
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }

    @NonNull
    private List<PostChild> getSliderItems(final JSONArray children) throws JSONException {
        final List<PostChild> sliderItems = new ArrayList<>();
        for (int j = 0; j < children.length(); ++j) {
            final JSONObject childNode = children.optJSONObject(j).getJSONObject("node");
            final boolean isChildVideo = childNode.optBoolean("is_video");
            int height = 0;
            int width = 0;
            final JSONObject dimensions = childNode.optJSONObject("dimensions");
            if (dimensions != null) {
                height = dimensions.optInt("height");
                width = dimensions.optInt("width");
            }
            String thumbnailUrl = null;
            try {
                thumbnailUrl = childNode.getJSONArray("display_resources")
                                        .getJSONObject(0)
                                        .getString("src");
            } catch (JSONException ignored) {}
            final PostChild sliderItem = new PostChild.Builder()
                    .setItemType(isChildVideo ? MediaItemType.MEDIA_TYPE_VIDEO
                                              : MediaItemType.MEDIA_TYPE_IMAGE)
                    .setPostId(childNode.getString(Constants.EXTRAS_ID))
                    .setDisplayUrl(isChildVideo ? childNode.getString("video_url")
                                                : childNode.getString("display_url"))
                    .setThumbnailUrl(thumbnailUrl != null ? thumbnailUrl
                                                          : childNode.getString("display_url"))
                    .setVideoViews(childNode.optLong("video_view_count", -1))
                    .setHeight(height)
                    .setWidth(width)
                    .build();
            // Log.d(TAG, "getSliderItems: sliderItem: " + sliderItem);
            sliderItems.add(sliderItem);
        }
        return sliderItems;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final List<FeedModel> postModels) {
        if (fetchListener != null) fetchListener.onResult(postModels);
    }
}