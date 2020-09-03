package awais.instagrabber.services;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import awais.instagrabber.repositories.MediaRepository;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MediaService extends BaseService {
    private static final String TAG = "MediaService";

    private final MediaRepository repository;

    private static MediaService instance;

    private MediaService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(MediaRepository.class);
    }

    public static MediaService getInstance() {
        if (instance == null) {
            instance = new MediaService();
        }
        return instance;
    }

    public void like(final String mediaId,
                     final String userId,
                     final String csrfToken,
                     final ServiceCallback<Boolean> callback) {
        likeAction(mediaId, userId, "like", csrfToken, callback);
    }

    public void unlike(final String mediaId,
                       final String userId,
                       final String csrfToken,
                       final ServiceCallback<Boolean> callback) {
        likeAction(mediaId, userId, "unlike", csrfToken, callback);
    }

    public void save(final String mediaId,
                     final String userId,
                     final String csrfToken,
                     final ServiceCallback<Boolean> callback) {
        likeAction(mediaId, userId, "save", csrfToken, callback);
    }

    public void unsave(final String mediaId,
                       final String userId,
                       final String csrfToken,
                       final ServiceCallback<Boolean> callback) {
        likeAction(mediaId, userId, "unsave", csrfToken, callback);
    }

    private void likeAction(final String mediaId,
                            final String userId,
                            final String action,
                            final String csrfToken,
                            final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>(4);
        form.put("media_id", mediaId);
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        // form.put("radio_type", "wifi-none");
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> request = repository.likeAction(Constants.I_USER_AGENT, action, mediaId, signedForm);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call,
                                   @NonNull final Response<String> response) {
                if (callback == null) return;
                final String body = response.body();
                if (body == null) {
                    callback.onFailure(new RuntimeException("Returned body is null"));
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
            public void onFailure(@NonNull final Call<String> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
        // const signedFormData = this.client.request.sign({
        //             media_id: options.mediaId,
        //             _csrftoken: this.client.state.cookieCsrfToken,
        //             _uid: this.client.state.cookieUserId,
        //             _uuid: this.client.state.uuid,
        // });
        //
        // const { body } = await this.client.request.send({
        //                                                         url: `/api/v1/media/${options.mediaId}/${options.action}/`,
        //     method: 'POST',
        //             form: {
        //     ...signedFormData,
        //                 d: options.d,
        //     },
        // });
        //     return body;
    }


}
