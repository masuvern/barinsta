package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import awais.instagrabber.repositories.FriendshipRepository;
import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoRestrictRootResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FriendshipService extends BaseService {
    private static final String TAG = "FriendshipService";

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
                       final String csrfToken,
                       final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("create", userId, targetUserId, csrfToken, callback);
    }

    public void unfollow(final String userId,
                         final String targetUserId,
                         final String csrfToken,
                         final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("destroy", userId, targetUserId, csrfToken, callback);
    }

    public void block(final String userId,
                      final String targetUserId,
                      final String csrfToken,
                      final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("block", userId, targetUserId, csrfToken, callback);
    }

    public void unblock(final String userId,
                        final String targetUserId,
                        final String csrfToken,
                        final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("unblock", userId, targetUserId, csrfToken, callback);
    }

    public void toggleRestrict(final String targetUserId,
                               final boolean restrict,
                               final String csrfToken,
                               final ServiceCallback<FriendshipRepoRestrictRootResponse> callback) {
        final Map<String, String> form = new HashMap<>(3);
        form.put("_csrftoken", csrfToken);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("target_user_id", targetUserId);
        final String action = restrict ? "restrict" : "unrestrict";
        final Call<FriendshipRepoRestrictRootResponse> request = repository.toggleRestrict(Constants.I_USER_AGENT, action, form);
        request.enqueue(new Callback<FriendshipRepoRestrictRootResponse>() {
            @Override
            public void onResponse(@NonNull final Call<FriendshipRepoRestrictRootResponse> call,
                                   @NonNull final Response<FriendshipRepoRestrictRootResponse> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<FriendshipRepoRestrictRootResponse> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public void approve(final String userId,
                       final String targetUserId,
                       final String csrfToken,
                       final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("approve", userId, targetUserId, csrfToken, callback);
    }

    public void ignore(final String userId,
                        final String targetUserId,
                        final String csrfToken,
                        final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("ignore", userId, targetUserId, csrfToken, callback);
    }

    private void change(final String action,
                        final String userId,
                        final String targetUserId,
                        final String csrfToken,
                        final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        final Map<String, Object> form = new HashMap<>(5);
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("radio_type", "wifi-none");
        form.put("user_id", targetUserId);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<FriendshipRepoChangeRootResponse> request = repository.change(Constants.I_USER_AGENT, action, targetUserId, signedForm);
        request.enqueue(new Callback<FriendshipRepoChangeRootResponse>() {
            @Override
            public void onResponse(@NonNull final Call<FriendshipRepoChangeRootResponse> call,
                                   @NonNull final Response<FriendshipRepoChangeRootResponse> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<FriendshipRepoChangeRootResponse> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }
}
