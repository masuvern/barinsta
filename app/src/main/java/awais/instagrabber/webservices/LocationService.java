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
import awais.instagrabber.repositories.LocationRepository;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LocationService extends BaseService {
    private static final String TAG = "LocationService";

    private final LocationRepository repository, webRepository;

    private static LocationService instance;

    private LocationService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(LocationRepository.class);
        final Retrofit webRetrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com")
                .build();
        webRepository = webRetrofit.create(LocationRepository.class);
    }

    public static LocationService getInstance() {
        if (instance == null) {
            instance = new LocationService();
        }
        return instance;
    }

    public void fetchPosts(@NonNull final String locationId,
                           final String maxId,
                           final ServiceCallback<LocationPostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<String> request = repository.fetchPosts(locationId, builder.build());
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
                    final LocationPostsFetchResponse tagPostsFetchResponse = parseResponse(body);
                    callback.onSuccess(tagPostsFetchResponse);
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

    private LocationPostsFetchResponse parseResponse(@NonNull final String body) throws JSONException {
        final JSONObject root = new JSONObject(body);
        final boolean moreAvailable = root.optBoolean("more_available");
        final String nextMaxId = root.optString("next_max_id");
        final int numResults = root.optInt("num_results");
        final String status = root.optString("status");
        final JSONArray itemsJson = root.optJSONArray("items");
        final List<FeedModel> items = parseItems(itemsJson);
        return new LocationPostsFetchResponse(
                moreAvailable,
                nextMaxId,
                numResults,
                status,
                items
        );
    }

    private List<FeedModel> parseItems(final JSONArray items) throws JSONException {
        if (items == null) {
            return Collections.emptyList();
        }
        final List<FeedModel> feedModels = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            final JSONObject itemJson = items.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseItem(itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return feedModels;
    }

    public void fetchGraphQLPosts(@NonNull final String locationId,
                                  final String maxId,
                                  final ServiceCallback<LocationPostsFetchResponse> callback) {
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", "36bd0f2bf5911908de389b8ceaa3be6d");
        queryMap.put("variables", "{" +
                "\"id\":\"" + locationId + "\"," +
                "\"first\":25," +
                "\"after\":\"" + (maxId == null ? "" : maxId) + "\"" +
                "}");
        final Call<String> request = webRepository.fetchGraphQLPosts(queryMap);
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
                    final LocationPostsFetchResponse tagPostsFetchResponse = parseGraphQLResponse(body);
                    callback.onSuccess(tagPostsFetchResponse);
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

    private LocationPostsFetchResponse parseGraphQLResponse(@NonNull final String body) throws JSONException {
        final JSONObject rootroot = new JSONObject(body);
        final JSONObject root = rootroot.getJSONObject("data").getJSONObject("location").getJSONObject("edge_location_to_media");
        final boolean moreAvailable = root.getJSONObject("page_info").optBoolean("has_next_page");
        final String nextMaxId = root.getJSONObject("page_info").optString("end_cursor");
        final int numResults = root.optInt("count");
        final String status = rootroot.optString("status");
        final JSONArray itemsJson = root.optJSONArray("edges");
        final List<FeedModel> items = parseGraphQLItems(itemsJson);
        return new LocationPostsFetchResponse(
                moreAvailable,
                nextMaxId,
                numResults,
                status,
                items
        );
    }

    private List<FeedModel> parseGraphQLItems(final JSONArray items) throws JSONException {
        if (items == null) {
            return Collections.emptyList();
        }
        final List<FeedModel> feedModels = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            final JSONObject itemJson = items.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseGraphQLItem(itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return feedModels;
    }

    public static class LocationPostsFetchResponse {
        private boolean moreAvailable;
        private String nextMaxId;
        private int numResults;
        private String status;
        private List<FeedModel> items;

        public LocationPostsFetchResponse(final boolean moreAvailable,
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

        public LocationPostsFetchResponse setMoreAvailable(final boolean moreAvailable) {
            this.moreAvailable = moreAvailable;
            return this;
        }

        public String getNextMaxId() {
            return nextMaxId;
        }

        public LocationPostsFetchResponse setNextMaxId(final String nextMaxId) {
            this.nextMaxId = nextMaxId;
            return this;
        }

        public int getNumResults() {
            return numResults;
        }

        public LocationPostsFetchResponse setNumResults(final int numResults) {
            this.numResults = numResults;
            return this;
        }

        public String getStatus() {
            return status;
        }

        public LocationPostsFetchResponse setStatus(final String status) {
            this.status = status;
            return this;
        }

        public List<FeedModel> getItems() {
            return items;
        }

        public LocationPostsFetchResponse setItems(final List<FeedModel> items) {
            this.items = items;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final LocationPostsFetchResponse that = (LocationPostsFetchResponse) o;
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
            return "LocationPostsFetchResponse{" +
                    "moreAvailable=" + moreAvailable +
                    ", nextMaxId='" + nextMaxId + '\'' +
                    ", numResults=" + numResults +
                    ", status='" + status + '\'' +
                    ", items=" + items +
                    '}';
        }
    }
}
