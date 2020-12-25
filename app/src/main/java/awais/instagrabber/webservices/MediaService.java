package awais.instagrabber.webservices;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.repositories.MediaRepository;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
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

    public void fetch(final String mediaId,
                      final ServiceCallback<FeedModel> callback) {
        final Call<String> request = repository.fetch(mediaId);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call,
                                   @NonNull final Response<String> response) {
                if (callback == null) return;
                final String body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject itemJson = new JSONObject(body).getJSONArray("items").getJSONObject(0);
                    callback.onSuccess(ResponseBodyUtils.parseItem(itemJson));
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
        final Call<String> request = repository.action(action, mediaId, signedForm);
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
        final Call<String> commentRequest = repository.comment(mediaId, signedForm);
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
        final Call<String> bulkDeleteRequest = repository.commentsBulkDelete(mediaId, signedForm);
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
        final Call<String> commentLikeRequest = repository.commentLike(commentId, signedForm);
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
        final Call<String> commentUnlikeRequest = repository.commentUnlike(commentId, signedForm);
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

    public void editCaption(final String postId,
                            final String userId,
                            final String newCaption,
                            @NonNull final String csrfToken,
                            @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("igtv_feed_preview", "false");
        form.put("media_id", postId);
        form.put("caption_text", newCaption);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> request = repository.editCaption(postId, signedForm);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while editing caption");
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
                Log.e(TAG, "Error editing caption", t);
                callback.onFailure(t);
            }
        });
    }

    public void fetchLikes(final String mediaId,
                           @NonNull final ServiceCallback<List<ProfileModel>> callback) {
        final Call<String> likesRequest = repository.fetchLikes(mediaId);
        likesRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while fetching likes of "+mediaId);
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject data = new JSONObject(body);
                    final JSONArray users = data.getJSONArray("users");
                    final int usersLen = users.length();
                    final List<ProfileModel> userModels = new ArrayList<>();
                    for (int j = 0; j < usersLen; ++j) {
                        final JSONObject userObject = users.getJSONObject(j);
                        userModels.add(new ProfileModel(userObject.optBoolean("is_private"),
                                false,
                                userObject.optBoolean("is_verified"),
                                String.valueOf(userObject.get("pk")),
                                userObject.getString("username"),
                                userObject.optString("full_name"),
                                null, null,
                                userObject.getString("profile_pic_url"),
                                null, 0, 0, 0, false, false, false, false, false));
                    }
                    callback.onSuccess(userModels);
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error getting likes", t);
                callback.onFailure(t);
            }
        });
    }

    public void translate(final String id,
                          final String type, // 1 caption 2 comment 3 bio
                          @NonNull final ServiceCallback<String> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("id", id);
        form.put("type", type);
        final Call<String> request = repository.translate(form);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while translating");
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String translation = jsonObject.optString("translation");
                    callback.onSuccess(translation);
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error translating", t);
                callback.onFailure(t);
            }
        });
    }
}
