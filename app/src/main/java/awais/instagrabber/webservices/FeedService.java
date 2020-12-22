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
import java.util.TimeZone;
import java.util.UUID;

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

    private final FeedRepository repository;

    private static FeedService instance;

    private FeedService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(FeedRepository.class);
    }

    public static FeedService getInstance() {
        if (instance == null) {
            instance = new FeedService();
        }
        return instance;
    }

    public void fetch(final String csrfToken,
                      final String cursor,
                      final ServiceCallback<PostsFetchResponse> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("_csrftoken", csrfToken);
        form.put("phone_id", UUID.randomUUID().toString());
        form.put("device_id", UUID.randomUUID().toString());
        form.put("client_session_id", UUID.randomUUID().toString());
        form.put("is_prefetch", "0");
        form.put("timezone_offset", String.valueOf(TimeZone.getDefault().getRawOffset() / 1000));
        if (!TextUtils.isEmpty(cursor)) {
            form.put("max_id", cursor);
            form.put("reason", "pagination");
        }
        else {
            form.put("is_pull_to_refresh", "1");
            form.put("reason", "pull_to_refresh");
        }
        final Call<String> request = repository.fetch(form);
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
        final JSONObject root = new JSONObject(body);
        final boolean moreAvailable = root.optBoolean("more_available");
        final String nextMaxId = root.optString("next_max_id");
        final JSONArray feedItems = root.optJSONArray("items");
        final List<FeedModel> feedModels = new ArrayList<>();
        for (int i = 0; i < feedItems.length(); ++i) {
            final JSONObject itemJson = feedItems.optJSONObject(i);
            if (itemJson == null || itemJson.has("injected")
            ) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseItem(itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return new PostsFetchResponse(feedModels, moreAvailable, nextMaxId);
    }
}