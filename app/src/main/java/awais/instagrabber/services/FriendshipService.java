package awais.instagrabber.services;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import awais.instagrabber.repositories.FriendshipRepository;
import awais.instagrabber.repositories.responses.FriendshipRepositoryChangeResponseRootObject;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FriendshipService extends BaseService {
    private static final String TAG = "ProfileService";

    private final FriendshipRepository repository;

    private static FriendshipService instance;

    private FriendshipService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(FriendshipRepository.class);
    }

    public static FriendshipService getInstance() {
        if (instance == null) {
            instance = new FriendshipService();
        }
        return instance;
    }

    public void follow(final String userId,
                       final String targetUserId,
                       final String crsfToken,
                       final ServiceCallback<FriendshipRepositoryChangeResponseRootObject> callback) {
        change("create", userId, targetUserId, crsfToken, callback);
    }

    public void unfollow(final String userId,
                         final String targetUserId,
                         final String crsfToken,
                         final ServiceCallback<FriendshipRepositoryChangeResponseRootObject> callback) {
        change("destroy", userId, targetUserId, crsfToken, callback);
    }

    private void change(final String action,
                        final String userId,
                        final String targetUserId,
                        final String crsfToken,
                        final ServiceCallback<FriendshipRepositoryChangeResponseRootObject> callback) {
        final Map<String, Object> form = new HashMap<>(5);
        form.put("_csrftoken", crsfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("user_id", targetUserId);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<FriendshipRepositoryChangeResponseRootObject> request = repository.change(action, targetUserId, signedForm);
        request.enqueue(new Callback<FriendshipRepositoryChangeResponseRootObject>() {
            @Override
            public void onResponse(@NonNull final Call<FriendshipRepositoryChangeResponseRootObject> call,
                                   @NonNull final Response<FriendshipRepositoryChangeResponseRootObject> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<FriendshipRepositoryChangeResponseRootObject> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }
}
