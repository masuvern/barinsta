package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import awais.instagrabber.repositories.NewsRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NewsService extends BaseService {
    private static final String TAG = "NewsService";

    private final NewsRepository repository;

    private static NewsService instance;

    private NewsService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(NewsRepository.class);
    }

    public static NewsService getInstance() {
        if (instance == null) {
            instance = new NewsService();
        }
        return instance;
    }

    public void markChecked(final String timestamp,
                            final String csrfToken,
                            final ServiceCallback<Boolean> callback) {
        final Map<String, String> map = new HashMap<>();
        map.put("timestamp", timestamp);
        final Call<String> request = repository.markChecked(csrfToken, map);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));

                } catch (JSONException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }
}
