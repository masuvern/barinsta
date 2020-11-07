package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.ProfileRepository;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.repositories.responses.UserInfo;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ProfileService extends BaseService {
    private static final String TAG = "ProfileService";

    private final ProfileRepository repository;
    private final ProfileRepository wwwRepository;

    private static ProfileService instance;

    private ProfileService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        final Retrofit wwwRetrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com")
                .build();
        repository = retrofit.create(ProfileRepository.class);
        wwwRepository = wwwRetrofit.create(ProfileRepository.class);
    }

    public static ProfileService getInstance() {
        if (instance == null) {
            instance = new ProfileService();
        }
        return instance;
    }

    public void getUserInfo(final String uid, final ServiceCallback<UserInfo> callback) {
        final Call<String> request = repository.getUserInfo(uid);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) return;
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final JSONObject user = jsonObject.optJSONObject(Constants.EXTRAS_USER);
                    if (user == null) return;
                    // Log.d(TAG, "user: " + user.toString());
                    final UserInfo userInfo = new UserInfo(
                            uid,
                            user.getString(Constants.EXTRAS_USERNAME),
                            user.optString("full_name"),
                            user.optString("profile_pic_url")
                    );
                    callback.onSuccess(userInfo);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing json", e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }


    public void fetchPosts(final ProfileModel profileModel,
                           final int postsPerPage,
                           final String cursor,
                           final ServiceCallback<PostsFetchResponse> callback) {
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", "18a7b935ab438c4514b1f742d8fa07a7");
        queryMap.put("variables", "{" +
                "\"id\":\"" + profileModel.getId() + "\"," +
                "\"first\":" + postsPerPage + "," +
                "\"after\":\"" + (cursor == null ? "" : cursor) + "\"" +
                "}");
        final Call<String> request = wwwRepository.fetch(queryMap);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    // Log.d(TAG, "onResponse: body: " + response.body());
                    final PostsFetchResponse postsFetchResponse = parseResponse(profileModel, response);
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

    private PostsFetchResponse parseResponse(final ProfileModel profileModel, final Response<String> response) throws JSONException {
        if (TextUtils.isEmpty(response.body())) {
            Log.e(TAG, "parseResponse: feed response body is empty with status code: " + response.code());
            return new PostsFetchResponse(Collections.emptyList(), false, null);
        }
        return parseResponseBody(profileModel, response.body());
    }

    private PostsFetchResponse parseResponseBody(final ProfileModel profileModel, final String body) throws JSONException {
        // Log.d(TAG, "parseResponseBody: body: " + body);
        final List<FeedModel> feedModels = new ArrayList<>();
        // return new FeedFetchResponse(feedModels, false, null);
        final JSONObject mediaPosts = new JSONObject(body)
                .getJSONObject("data")
                .getJSONObject(Constants.EXTRAS_USER)
                .getJSONObject("edge_owner_to_timeline_media");
        final String endCursor;
        final boolean hasNextPage;
        final JSONObject pageInfo = mediaPosts.getJSONObject("page_info");
        if (pageInfo.has("has_next_page")) {
            hasNextPage = pageInfo.getBoolean("has_next_page");
            endCursor = hasNextPage ? pageInfo.getString("end_cursor") : null;
        } else {
            hasNextPage = false;
            endCursor = null;
        }
        final JSONArray edges = mediaPosts.getJSONArray("edges");
        for (int i = 0; i < edges.length(); ++i) {
            final JSONObject mediaNode = edges.getJSONObject(i).getJSONObject("node");
            final String mediaType = mediaNode.optString("__typename");
            if (mediaType.isEmpty() || "GraphSuggestedUserFeedUnit".equals(mediaType))
                continue;
            final boolean isVideo = mediaNode.getBoolean("is_video");
            final long videoViews = mediaNode.optLong("video_view_count", 0);

            final String displayUrl = mediaNode.optString("display_url");
            if (TextUtils.isEmpty(displayUrl)) continue;
            final String resourceUrl;

            if (isVideo) {
                resourceUrl = mediaNode.getString("video_url");
            } else {
                resourceUrl = mediaNode.has("display_resources") ? ResponseBodyUtils.getHighQualityImage(mediaNode) : displayUrl;
            }
            JSONObject tempJsonObject = mediaNode.optJSONObject("edge_media_to_comment");
            final long commentsCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
            tempJsonObject = mediaNode.optJSONObject("edge_media_preview_like");
            final long likesCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
            tempJsonObject = mediaNode.optJSONObject("edge_media_to_caption");
            final JSONArray captions = tempJsonObject != null ? tempJsonObject.getJSONArray("edges") : null;
            String captionText = null;
            if (captions != null && captions.length() > 0) {
                if ((tempJsonObject = captions.optJSONObject(0)) != null &&
                        (tempJsonObject = tempJsonObject.optJSONObject("node")) != null) {
                    captionText = tempJsonObject.getString("text");
                }
            }
            final JSONObject location = mediaNode.optJSONObject("location");
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
            final JSONObject dimensions = mediaNode.optJSONObject("dimensions");
            if (dimensions != null) {
                height = dimensions.optInt("height");
                width = dimensions.optInt("width");
            }
            String thumbnailUrl = null;
            try {
                thumbnailUrl = mediaNode.getJSONArray("display_resources")
                                        .getJSONObject(0)
                                        .getString("src");
            } catch (JSONException ignored) {}
            final FeedModel.Builder builder = new FeedModel.Builder()
                    .setProfileModel(profileModel)
                    .setItemType(isVideo ? MediaItemType.MEDIA_TYPE_VIDEO
                                         : MediaItemType.MEDIA_TYPE_IMAGE)
                    .setViewCount(videoViews)
                    .setPostId(mediaNode.getString(Constants.EXTRAS_ID))
                    .setDisplayUrl(resourceUrl)
                    .setThumbnailUrl(thumbnailUrl != null ? thumbnailUrl : displayUrl)
                    .setShortCode(mediaNode.getString(Constants.EXTRAS_SHORTCODE))
                    .setPostCaption(captionText)
                    .setCommentsCount(commentsCount)
                    .setTimestamp(mediaNode.optLong("taken_at_timestamp", -1))
                    .setLiked(mediaNode.getBoolean("viewer_has_liked"))
                    .setBookmarked(mediaNode.getBoolean("viewer_has_saved"))
                    .setLikesCount(likesCount)
                    .setLocationName(locationName)
                    .setLocationId(locationId)
                    .setImageHeight(height)
                    .setImageWidth(width);
            final boolean isSlider = "GraphSidecar".equals(mediaType) && mediaNode.has("edge_sidecar_to_children");
            if (isSlider) {
                builder.setItemType(MediaItemType.MEDIA_TYPE_SLIDER);
                final JSONObject sidecar = mediaNode.optJSONObject("edge_sidecar_to_children");
                if (sidecar != null) {
                    final JSONArray children = sidecar.optJSONArray("edges");
                    if (children != null) {
                        final List<PostChild> sliderItems = getSliderItems(children);
                        builder.setSliderItems(sliderItems);
                    }
                }
            }
            final FeedModel feedModel = builder.build();
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

    public void fetchSaved(final String maxId,
                           final ServiceCallback<SavedPostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<String> request = repository.fetchSaved(builder.build());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    if (callback == null) {
                        return;
                    }
                    final String body = response.body();
                    if (TextUtils.isEmpty(body)) {
                        callback.onSuccess(null);
                        return;
                    }
                    final SavedPostsFetchResponse savedPostsFetchResponse = parseSavedPostsResponse(body, true);
                    callback.onSuccess(savedPostsFetchResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    callback.onFailure(e);
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

    public void fetchLiked(final String maxId,
                           final ServiceCallback<SavedPostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<String> request = repository.fetchLiked(builder.build());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    if (callback == null) {
                        return;
                    }
                    final String body = response.body();
                    if (TextUtils.isEmpty(body)) {
                        callback.onSuccess(null);
                        return;
                    }
                    final SavedPostsFetchResponse savedPostsFetchResponse = parseSavedPostsResponse(body, false);
                    callback.onSuccess(savedPostsFetchResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    callback.onFailure(e);
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

    public void fetchTagged(final String profileId,
                            final String maxId,
                            final ServiceCallback<SavedPostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<String> request = repository.fetchTagged(profileId, builder.build());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    if (callback == null) {
                        return;
                    }
                    final String body = response.body();
                    if (TextUtils.isEmpty(body)) {
                        callback.onSuccess(null);
                        return;
                    }
                    final SavedPostsFetchResponse savedPostsFetchResponse = parseSavedPostsResponse(body, false);
                    callback.onSuccess(savedPostsFetchResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    callback.onFailure(e);
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

    private SavedPostsFetchResponse parseSavedPostsResponse(final String body, final boolean isInMedia) throws JSONException {
        final JSONObject root = new JSONObject(body);
        final boolean moreAvailable = root.optBoolean("more_available");
        final String nextMaxId = root.optString("next_max_id");
        final int numResults = root.optInt("num_results");
        final String status = root.optString("status");
        final JSONArray itemsJson = root.optJSONArray("items");
        final List<FeedModel> items = parseItems(itemsJson, isInMedia);
        return new SavedPostsFetchResponse(
                moreAvailable,
                nextMaxId,
                numResults,
                status,
                items
        );
    }

    private List<FeedModel> parseItems(final JSONArray items, final boolean isInMedia) throws JSONException {
        if (items == null) {
            return Collections.emptyList();
        }
        final List<FeedModel> feedModels = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            final JSONObject itemJson = items.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseItem(isInMedia ? itemJson.optJSONObject("media") : itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return feedModels;
    }

    public static class SavedPostsFetchResponse {
        private boolean moreAvailable;
        private String nextMaxId;
        private int numResults;
        private String status;
        private List<FeedModel> items;

        public SavedPostsFetchResponse(final boolean moreAvailable,
                                       final String nextMaxId,
                                       final int numResults,
                                       final String status,
                                       final List<FeedModel> items) {
            this.moreAvailable = moreAvailable;
            this.nextMaxId = nextMaxId;
            this.numResults = numResults;
            this.status = status;
            this.items = items;
        }

        public boolean isMoreAvailable() {
            return moreAvailable;
        }

        public SavedPostsFetchResponse setMoreAvailable(final boolean moreAvailable) {
            this.moreAvailable = moreAvailable;
            return this;
        }

        public String getNextMaxId() {
            return nextMaxId;
        }

        public SavedPostsFetchResponse setNextMaxId(final String nextMaxId) {
            this.nextMaxId = nextMaxId;
            return this;
        }

        public int getNumResults() {
            return numResults;
        }

        public SavedPostsFetchResponse setNumResults(final int numResults) {
            this.numResults = numResults;
            return this;
        }

        public String getStatus() {
            return status;
        }

        public SavedPostsFetchResponse setStatus(final String status) {
            this.status = status;
            return this;
        }

        public List<FeedModel> getItems() {
            return items;
        }

        public SavedPostsFetchResponse setItems(final List<FeedModel> items) {
            this.items = items;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SavedPostsFetchResponse that = (SavedPostsFetchResponse) o;
            return moreAvailable == that.moreAvailable &&
                    numResults == that.numResults &&
                    Objects.equals(nextMaxId, that.nextMaxId) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moreAvailable, nextMaxId, numResults, status, items);
        }

        @NonNull
        @Override
        public String toString() {
            return "SavedPostsFetchResponse{" +
                    "moreAvailable=" + moreAvailable +
                    ", nextMaxId='" + nextMaxId + '\'' +
                    ", numResults=" + numResults +
                    ", status='" + status + '\'' +
                    ", items=" + items +
                    '}';
        }
    }
}
