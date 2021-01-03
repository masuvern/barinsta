package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.repositories.ProfileRepository;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ProfileService extends BaseService {
    private static final String TAG = "ProfileService";

    private final ProfileRepository repository;

    private static ProfileService instance;

    private ProfileService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(ProfileRepository.class);
    }

    public static ProfileService getInstance() {
        if (instance == null) {
            instance = new ProfileService();
        }
        return instance;
    }

    public void fetchPosts(final String userId,
                           final String maxId,
                           final ServiceCallback<PostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<String> request = repository.fetch(userId, builder.build());
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
                    final PostsFetchResponse postsFetchResponse = parseProfilePostsResponse(body, false);
                    callback.onSuccess(postsFetchResponse);
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

    public void fetchSaved(final String maxId,
                           final ServiceCallback<PostsFetchResponse> callback) {
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
                    final PostsFetchResponse PostsFetchResponse = parseSavedPostsResponse(body, true);
                    callback.onSuccess(PostsFetchResponse);
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
                           final ServiceCallback<PostsFetchResponse> callback) {
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
                    final PostsFetchResponse PostsFetchResponse = parseSavedPostsResponse(body, false);
                    callback.onSuccess(PostsFetchResponse);
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
                            final ServiceCallback<PostsFetchResponse> callback) {
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
                    final PostsFetchResponse PostsFetchResponse = parseSavedPostsResponse(body, false);
                    callback.onSuccess(PostsFetchResponse);
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

    private PostsFetchResponse parseProfilePostsResponse(final String body, final boolean isInMedia) throws JSONException {
        final JSONObject root = new JSONObject(body);
        final boolean moreAvailable = root.optBoolean("more_available");
        final String nextMaxId = root.optString("next_max_id");
        final JSONArray itemsJson = root.optJSONArray("items");
        final List<FeedModel> items = parseItems(itemsJson, isInMedia);
        return new PostsFetchResponse(
                items,
                moreAvailable,
                nextMaxId
        );
    }

    private PostsFetchResponse parseSavedPostsResponse(final String body, final boolean isInMedia) throws JSONException {
        final JSONObject root = new JSONObject(body);
        final boolean moreAvailable = root.optBoolean("more_available");
        final String nextMaxId = root.optString("next_max_id");
        final int numResults = root.optInt("num_results");
        final String status = root.optString("status");
        final JSONArray itemsJson = root.optJSONArray("items");
        final List<FeedModel> items = parseItems(itemsJson, isInMedia);
        return new PostsFetchResponse(
                items,
                moreAvailable,
                nextMaxId
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
}
