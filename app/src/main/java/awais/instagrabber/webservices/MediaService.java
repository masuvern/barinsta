package awais.instagrabber.webservices;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        action(mediaId, userId, "like", csrfToken, callback);
    }

    public void unlike(final String mediaId,
                       final String userId,
                       final String csrfToken,
                       final ServiceCallback<Boolean> callback) {
        action(mediaId, userId, "unlike", csrfToken, callback);
    }

    public void save(final String mediaId,
                     final String userId,
                     final String csrfToken,
                     final ServiceCallback<Boolean> callback) {
        action(mediaId, userId, "save", csrfToken, callback);
    }

    public void unsave(final String mediaId,
                       final String userId,
                       final String csrfToken,
                       final ServiceCallback<Boolean> callback) {
        action(mediaId, userId, "unsave", csrfToken, callback);
    }

    private void action(final String mediaId,
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
        final Call<String> request = repository.action(Constants.I_USER_AGENT, action, mediaId, signedForm);
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
    }

    public void comment(@NonNull final String mediaId,
                        @NonNull final String comment,
                        @NonNull final String userId,
                        final String replyToCommentId,
                        final String csrfToken,
                        @NonNull final ServiceCallback<Boolean> callback) {
        final String module = "self_comments_v2";
        final Map<String, Object> form = new HashMap<>();
        // form.put("user_breadcrumb", userBreadcrumb(comment.length()));
        form.put("idempotence_token", UUID.randomUUID().toString());
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("comment_text", comment);
        form.put("containermodule", module);
        if (!awais.instagrabber.utils.TextUtils.isEmpty(replyToCommentId)) {
            form.put("replied_to_comment_id", replyToCommentId);
        }
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> commentRequest = repository.comment(Constants.I_USER_AGENT, mediaId, signedForm);
        commentRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while creating comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void deleteComment(final String mediaId,
                              final String userId,
                              final String commentId,
                              final String csrfToken,
                              @NonNull final ServiceCallback<Boolean> callback) {
        deleteComments(mediaId, userId, Collections.singletonList(commentId), csrfToken, callback);
    }

    public void deleteComments(final String mediaId,
                               final String userId,
                               final List<String> commentIds,
                               final String csrfToken,
                               @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("comment_ids_to_delete", TextUtils.join(",", commentIds));
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> bulkDeleteRequest = repository.commentsBulkDelete(Constants.USER_AGENT, mediaId, signedForm);
        bulkDeleteRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while deleting comments");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                // Log.e(TAG, "Error deleting comments", t);
                callback.onFailure(t);
            }
        });
    }

    public void commentLike(@NonNull final String commentId,
                            @NonNull final String csrfToken,
                            @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        // form.put("_uid", userId);
        // form.put("_uuid", UUID.randomUUID().toString());
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> commentLikeRequest = repository.commentLike(Constants.USER_AGENT, commentId, signedForm);
        commentLikeRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while liking comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error liking comment", t);
                callback.onFailure(t);
            }
        });
    }

    public void commentUnlike(final String commentId,
                              @NonNull final String csrfToken,
                              @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        // form.put("_uid", userId);
        // form.put("_uuid", UUID.randomUUID().toString());
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> commentUnlikeRequest = repository.commentUnlike(Constants.USER_AGENT, commentId, signedForm);
        commentUnlikeRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while unliking comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error unliking comment", t);
                callback.onFailure(t);
            }
        });
    }
}
