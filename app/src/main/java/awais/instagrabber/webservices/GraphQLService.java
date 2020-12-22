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
import awais.instagrabber.repositories.GraphQLRepository;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class GraphQLService extends BaseService {
    private static final String TAG = "GraphQLService";
    private static final boolean loadFromMock = false;

    private final GraphQLRepository repository;

    private static GraphQLService instance;

    private GraphQLService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com")
                .build();
        repository = retrofit.create(GraphQLRepository.class);
    }

    public static GraphQLService getInstance() {
        if (instance == null) {
            instance = new GraphQLService();
        }
        return instance;
    }

    private void fetch(final String queryHash,
                       final String variables,
                       final String arg1,
                       final String arg2,
                       final ServiceCallback<PostsFetchResponse> callback) {
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", queryHash);
        queryMap.put("variables", variables);
        final Call<String> request = repository.fetch(queryMap);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    // Log.d(TAG, "onResponse: body: " + response.body());
                    final PostsFetchResponse postsFetchResponse = parsePostResponse(response, arg1, arg2);
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

    public void fetchLocationPosts(@NonNull final String locationId,
                                   final String maxId,
                                   final ServiceCallback<PostsFetchResponse> callback) {
        fetch("36bd0f2bf5911908de389b8ceaa3be6d",
                "{\"id\":\"" + locationId + "\"," +
                        "\"first\":25," +
                        "\"after\":\"" + (maxId == null ? "" : maxId) + "\"}",
                Constants.EXTRAS_LOCATION,
                "edge_location_to_media",
                callback);
    }

    public void fetchHashtagPosts(@NonNull final String tag,
                                  final String maxId,
                                  final ServiceCallback<PostsFetchResponse> callback) {
        fetch("9b498c08113f1e09617a1703c22b2f32",
                "{\"tag_name\":\"" + tag + "\"," +
                        "\"first\":25," +
                        "\"after\":\"" + (maxId == null ? "" : maxId) + "\"}",
                Constants.EXTRAS_HASHTAG,
                "edge_hashtag_to_media",
                callback);
    }

    public void fetchProfilePosts(@NonNull final String profileId,
                                  final int postsPerPage,
                                  final String maxId,
                                  final ServiceCallback<PostsFetchResponse> callback) {
        fetch("18a7b935ab438c4514b1f742d8fa07a7",
                "{\"id\":\"" + profileId + "\"," +
                        "\"first\":" + postsPerPage + "," +
                        "\"after\":\"" + (maxId == null ? "" : maxId) + "\"}",
                Constants.EXTRAS_USER,
                "edge_owner_to_timeline_media",
                callback);
    }

    public void fetchTaggedPosts(@NonNull final String profileId,
                                 final int postsPerPage,
                                 final String maxId,
                                 final ServiceCallback<PostsFetchResponse> callback) {
        fetch("31fe64d9463cbbe58319dced405c6206",
                "{\"id\":\"" + profileId + "\"," +
                        "\"first\":" + postsPerPage + "," +
                        "\"after\":\"" + (maxId == null ? "" : maxId) + "\"}",
                Constants.EXTRAS_USER,
                "edge_user_to_photos_of_you",
                callback);
    }

    @NonNull
    private PostsFetchResponse parsePostResponse(@NonNull final Response<String> response, @NonNull final String arg1, @NonNull final String arg2) throws JSONException {
        if (TextUtils.isEmpty(response.body())) {
            Log.e(TAG, "parseResponse: feed response body is empty with status code: " + response.code());
            return new PostsFetchResponse(Collections.emptyList(), false, null);
        }
        return parseResponseBody(response.body(), arg1, arg2);
    }

    @NonNull
    private PostsFetchResponse parseResponseBody(@NonNull final String body, @NonNull final String arg1, @NonNull final String arg2)
            throws JSONException {
        final List<FeedModel> feedModels = new ArrayList<>();
        final JSONObject timelineFeed = new JSONObject(body)
                .getJSONObject("data")
                .getJSONObject(arg1)
                .getJSONObject(arg2);
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
            final JSONObject itemJson = feedItems.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseGraphQLItem(itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return new PostsFetchResponse(feedModels, hasNextPage, endCursor);
    }
}
