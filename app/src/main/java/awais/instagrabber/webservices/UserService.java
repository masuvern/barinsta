package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import java.util.TimeZone;

import awais.instagrabber.repositories.UserRepository;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.UserSearchResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserService extends BaseService {
    private static final String TAG = UserService.class.getSimpleName();

    private final UserRepository repository;

    private static UserService instance;

    private UserService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(UserRepository.class);
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public void getUserInfo(final long uid, final ServiceCallback<User> callback) {
        final Call<User> request = repository.getUserInfo(uid);
        request.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull final Call<User> call, @NonNull final Response<User> response) {
                final User user = response.body();
                if (user == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(user);
            }

            @Override
            public void onFailure(@NonNull final Call<User> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public Call<UserSearchResponse> search(final String query) {
        final float timezoneOffset = (float) TimeZone.getDefault().getRawOffset() / 1000;
        return repository.search(timezoneOffset, query);
    }
}
