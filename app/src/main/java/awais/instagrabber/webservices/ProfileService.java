package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import awais.instagrabber.repositories.ProfileRepository;
import awais.instagrabber.repositories.responses.UserInfo;
import awais.instagrabber.utils.Constants;
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
}
