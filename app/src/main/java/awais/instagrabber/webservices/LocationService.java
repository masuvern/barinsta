package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import awais.instagrabber.repositories.LocationRepository;
import awais.instagrabber.repositories.responses.LocationFeedResponse;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationService extends BaseService {
    private static final String TAG = "LocationService";

    private final LocationRepository repository;

    private static LocationService instance;

    private LocationService() {
        repository = RetrofitFactory.getInstance()
                                    .getRetrofit()
                                    .create(LocationRepository.class);
    }

    public static LocationService getInstance() {
        if (instance == null) {
            instance = new LocationService();
        }
        return instance;
    }

    public void fetchPosts(final long locationId,
                           final String maxId,
                           final ServiceCallback<PostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<LocationFeedResponse> request = repository.fetchPosts(locationId, builder.build());
        request.enqueue(new Callback<LocationFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<LocationFeedResponse> call, @NonNull final Response<LocationFeedResponse> response) {
                if (callback == null) return;
                final LocationFeedResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                final PostsFetchResponse postsFetchResponse = new PostsFetchResponse(
                        body.getItems(),
                        body.isMoreAvailable(),
                        body.getNextMaxId()
                );
                callback.onSuccess(postsFetchResponse);

            }

            @Override
            public void onFailure(@NonNull final Call<LocationFeedResponse> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    // private PostsFetchResponse parseResponse(@NonNull final String body) throws JSONException {
    //     final JSONObject root = new JSONObject(body);
    //     final boolean moreAvailable = root.optBoolean("more_available");
    //     final String nextMaxId = root.optString("next_max_id");
    //     final JSONArray itemsJson = root.optJSONArray("items");
    //     final List<FeedModel> items = parseItems(itemsJson);
    //     return new PostsFetchResponse(
    //             items,
    //             moreAvailable,
    //             nextMaxId
    //     );
    // }

    // private List<FeedModel> parseItems(final JSONArray items) throws JSONException {
    //     if (items == null) {
    //         return Collections.emptyList();
    //     }
    //     final List<FeedModel> feedModels = new ArrayList<>();
    //     for (int i = 0; i < items.length(); i++) {
    //         final JSONObject itemJson = items.optJSONObject(i);
    //         if (itemJson == null) {
    //             continue;
    //         }
    //         final FeedModel feedModel = ResponseBodyUtils.parseItem(itemJson);
    //         if (feedModel != null) {
    //             feedModels.add(feedModel);
    //         }
    //     }
    //     return feedModels;
    // }
}
