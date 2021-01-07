package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import awais.instagrabber.repositories.ProfileRepository;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.repositories.responses.UserFeedResponse;
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

    public void fetchPosts(final long userId,
                           final String maxId,
                           final ServiceCallback<PostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<UserFeedResponse> request = repository.fetch(userId, builder.build());
        request.enqueue(new Callback<UserFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<UserFeedResponse> call, @NonNull final Response<UserFeedResponse> response) {
                if (callback == null) return;
                final UserFeedResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(new PostsFetchResponse(
                        body.getItems(),
                        body.isMoreAvailable(),
                        body.getNextMaxId()
                ));
            }

            @Override
            public void onFailure(@NonNull final Call<UserFeedResponse> call, @NonNull final Throwable t) {
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
        final Call<UserFeedResponse> request = repository.fetchSaved(builder.build());
        request.enqueue(new Callback<UserFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<UserFeedResponse> call, @NonNull final Response<UserFeedResponse> response) {
                if (callback == null) return;
                final UserFeedResponse userFeedResponse = response.body();
                if (userFeedResponse == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(new PostsFetchResponse(
                        userFeedResponse.getItems(),
                        userFeedResponse.isMoreAvailable(),
                        userFeedResponse.getNextMaxId()
                ));
            }

            @Override
            public void onFailure(@NonNull final Call<UserFeedResponse> call, @NonNull final Throwable t) {
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
        final Call<UserFeedResponse> request = repository.fetchLiked(builder.build());
        request.enqueue(new Callback<UserFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<UserFeedResponse> call, @NonNull final Response<UserFeedResponse> response) {
                if (callback == null) return;
                final UserFeedResponse userFeedResponse = response.body();
                if (userFeedResponse == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(new PostsFetchResponse(
                        userFeedResponse.getItems(),
                        userFeedResponse.isMoreAvailable(),
                        userFeedResponse.getNextMaxId()
                ));
            }

            @Override
            public void onFailure(@NonNull final Call<UserFeedResponse> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public void fetchTagged(final long profileId,
                            final String maxId,
                            final ServiceCallback<PostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<UserFeedResponse> request = repository.fetchTagged(profileId, builder.build());
        request.enqueue(new Callback<UserFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<UserFeedResponse> call, @NonNull final Response<UserFeedResponse> response) {
                if (callback == null) return;
                final UserFeedResponse userFeedResponse = response.body();
                if (userFeedResponse == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(new PostsFetchResponse(
                        userFeedResponse.getItems(),
                        userFeedResponse.isMoreAvailable(),
                        userFeedResponse.getNextMaxId()
                ));
            }

            @Override
            public void onFailure(@NonNull final Call<UserFeedResponse> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    // private PostsFetchResponse parseProfilePostsResponse(final String body) throws JSONException {
    //     final JSONObject root = new JSONObject(body);
    //     final boolean moreAvailable = root.optBoolean("more_available");
    //     final String nextMaxId = root.optString("next_max_id");
    //     final JSONArray itemsJson = root.optJSONArray("items");
    //     final List<FeedModel> items = parseItems(itemsJson, false);
    //     return new PostsFetchResponse(
    //             items,
    //             moreAvailable,
    //             nextMaxId
    //     );
    // }

    // private PostsFetchResponse parseSavedPostsResponse(final String body, final boolean isInMedia) throws JSONException {
    //     final JSONObject root = new JSONObject(body);
    //     final boolean moreAvailable = root.optBoolean("more_available");
    //     final String nextMaxId = root.optString("next_max_id");
    //     final int numResults = root.optInt("num_results");
    //     final String status = root.optString("status");
    //     final JSONArray itemsJson = root.optJSONArray("items");
    //     final List<FeedModel> items = parseItems(itemsJson, isInMedia);
    //     return new PostsFetchResponse(
    //             items,
    //             moreAvailable,
    //             nextMaxId
    //     );
    // }

    // private List<FeedModel> parseItems(final JSONArray items, final boolean isInMedia) throws JSONException {
    //     if (items == null) {
    //         return Collections.emptyList();
    //     }
    //     final List<FeedModel> feedModels = new ArrayList<>();
    //     for (int i = 0; i < items.length(); i++) {
    //         final JSONObject itemJson = items.optJSONObject(i);
    //         if (itemJson == null) {
    //             continue;
    //         }
    //         final FeedModel feedModel = ResponseBodyUtils.parseItem(isInMedia ? itemJson.optJSONObject("media") : itemJson);
    //         if (feedModel != null) {
    //             feedModels.add(feedModel);
    //         }
    //     }
    //     return feedModels;
    // }
}
