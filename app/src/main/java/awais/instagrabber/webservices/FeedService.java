package awais.instagrabber.webservices;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.FeedRepository;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FeedService extends BaseService {
    private static final String TAG = "FeedService";
    private static final boolean loadFromMock = false;

    private final FeedRepository repository;

    private static FeedService instance;

    private FeedService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com")
                .build();
        repository = retrofit.create(FeedRepository.class);
    }

    public static FeedService getInstance() {
        if (instance == null) {
            instance = new FeedService();
        }
        return instance;
    }

    public void fetch(final int maxItemsToLoad,
                      final String cursor,
                      final ServiceCallback<PostsFetchResponse> callback) {
        if (loadFromMock) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                final ClassLoader classLoader = getClass().getClassLoader();
                if (classLoader == null) {
                    Log.e(TAG, "fetch: classLoader is null!");
                    return;
                }
                try (InputStream resourceAsStream = classLoader.getResourceAsStream("feed_response.json");
                     Reader in = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8)) {
                    final int bufferSize = 1024;
                    final char[] buffer = new char[bufferSize];
                    final StringBuilder out = new StringBuilder();
                    int charsRead;
                    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0) {
                        out.append(buffer, 0, charsRead);
                    }
                    callback.onSuccess(parseResponseBody(out.toString()));
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "fetch: ", e);
                }
            }, 1000);
            return;
        }
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", "6b838488258d7a4820e48d209ef79eb1");
        queryMap.put("variables", "{" +
                "\"fetch_media_item_count\":" + maxItemsToLoad + "," +
                "\"has_threaded_comments\":true," +
                "\"fetch_media_item_cursor\":\"" + (cursor == null ? "" : cursor) + "\"" +
                "}");
        final Call<String> request = repository.fetch(queryMap);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    // Log.d(TAG, "onResponse: body: " + response.body());
                    final PostsFetchResponse postsFetchResponse = parseResponse(response);
                    if (callback != null) {
                        callback.onSuccess(postsFetchResponse);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });

    }

    @NonNull
    private PostsFetchResponse parseResponse(@NonNull final Response<String> response) throws JSONException {
        if (TextUtils.isEmpty(response.body())) {
            Log.e(TAG, "parseResponse: feed response body is empty with status code: " + response.code());
            return new PostsFetchResponse(Collections.emptyList(), false, null);
        }
        return parseResponseBody(response.body());
    }

    @NonNull
    private PostsFetchResponse parseResponseBody(@NonNull final String body)
            throws JSONException {
        final List<FeedModel> feedModels = new ArrayList<>();
        final JSONObject timelineFeed = new JSONObject(body)
                .getJSONObject("data")
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
            tempJsonObject = feedItem.optJSONObject("edge_media_preview_like");
            final long likesCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
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
                    .setLikesCount(likesCount)
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
            feedModels.add(feedModel);
        }
        return new PostsFetchResponse(feedModels, hasNextPage, endCursor);
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
                    .setVideoViews(childNode.optLong("video_view_count", 0))
                    .setHeight(height)
                    .setWidth(width)
                    .build();
            // Log.d(TAG, "getSliderItems: sliderItem: " + sliderItem);
            sliderItems.add(sliderItem);
        }
        return sliderItems;
    }
}
