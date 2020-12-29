package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.repositories.StoriesRepository;
import awais.instagrabber.repositories.responses.StoryStickerResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StoriesService extends BaseService {
    private static final String TAG = "StoriesService";
    private static final boolean loadFromMock = false;

    private final StoriesRepository repository;

    private static StoriesService instance;

    private StoriesService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(StoriesRepository.class);
    }

    public static StoriesService getInstance() {
        if (instance == null) {
            instance = new StoriesService();
        }
        return instance;
    }

    public void fetch(final String mediaId,
                      final ServiceCallback<StoryModel> callback) {
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
                    callback.onSuccess(ResponseBodyUtils.parseStoryItem(itemJson, false, false, null));
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

    public void getFeedStories(final String csrfToken, final ServiceCallback<List<FeedStoryModel>> callback) {
        final Map<String, Object> form = new HashMap<>(4);
        form.put("reason", "cold_start");
        form.put("_csrftoken", csrfToken);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("supported_capabilities_new", Constants.SUPPORTED_CAPABILITIES);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> response = repository.getFeedStories(Constants.I_USER_AGENT, signedForm);
        response.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "getFeedStories: body is empty");
                    return;
                }
                parseStoriesBody(body, callback);
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private void parseStoriesBody(final String body, final ServiceCallback<List<FeedStoryModel>> callback) {
        try {
            final List<FeedStoryModel> feedStoryModels = new ArrayList<>();
            final JSONArray feedStoriesReel = new JSONObject(body).getJSONArray("tray");
            for (int i = 0; i < feedStoriesReel.length(); ++i) {
                final JSONObject node = feedStoriesReel.getJSONObject(i);
                final JSONObject user = node.getJSONObject(node.has("user") ? "user" : "owner");
                final ProfileModel profileModel = new ProfileModel(false, false, false,
                                                                   user.getString("pk"),
                                                                   user.getString("username"),
                                                                   null, null, null,
                                                                   user.getString("profile_pic_url"),
                                                                   null, 0, 0, 0, false, false, false, false, false);
                final String id = node.getString("id");
                final long timestamp = node.getLong("latest_reel_media");
                final boolean fullyRead = !node.isNull("seen") && node.getLong("seen") == timestamp;
                final JSONObject itemJson = node.getJSONArray("items").getJSONObject(0);
                final StoryModel firstStoryModel = ResponseBodyUtils.parseStoryItem(itemJson, false, false, null);
                feedStoryModels.add(new FeedStoryModel(id, profileModel, fullyRead, timestamp, firstStoryModel));
            }
            callback.onSuccess(feedStoryModels);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing json", e);
        }
    }

    public void fetchHighlights(final String profileId,
                                final ServiceCallback<List<HighlightModel>> callback) {
        final Call<String> request = repository.fetchHighlights(profileId);
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
                    final JSONArray highlightsReel = new JSONObject(body).getJSONArray("tray");

                    final int length = highlightsReel.length();
                    final List<HighlightModel> highlightModels = new ArrayList<>();

                    for (int i = 0; i < length; ++i) {
                        final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                        highlightModels.add(new HighlightModel(
                                highlightNode.getString("title"),
                                highlightNode.getString(Constants.EXTRAS_ID),
                                highlightNode.getJSONObject("cover_media")
                                        .getJSONObject("cropped_image_version")
                                        .getString("url"),
                                highlightNode.getLong("latest_reel_media")
                        ));
                    }
                    callback.onSuccess(highlightModels);
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

    public void fetchArchive(final String maxId,
                             final ServiceCallback<ArchiveFetchResponse> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("include_suggested_highlights", "false");
        form.put("is_in_archive_home", "true");
        form.put("include_cover", "1");
        form.put("timezone_offset", String.valueOf(TimeZone.getDefault().getRawOffset() / 1000));
        if (!TextUtils.isEmpty(maxId)) {
            form.put("max_id", maxId); // NOT TESTED
        }
        final Call<String> request = repository.fetchArchive(form);
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
                    final JSONObject data = new JSONObject(body);
                    final JSONArray highlightsReel = data.getJSONArray("items");

                    final int length = highlightsReel.length();
                    final List<HighlightModel> highlightModels = new ArrayList<>();

                    for (int i = 0; i < length; ++i) {
                        final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                        highlightModels.add(new HighlightModel(
                                null,
                                highlightNode.getString(Constants.EXTRAS_ID),
                                highlightNode.getJSONObject("cover_image_version").getString("url"),
                                highlightNode.getLong("timestamp")
                        ));
                    }
                    callback.onSuccess(new ArchiveFetchResponse(highlightModels,
                                                                data.getBoolean("more_available"),
                                                                data.getString("max_id")));
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

    public void getUserStory(final String id,
                             final String username,
                             final boolean isLoc,
                             final boolean isHashtag,
                             final boolean highlight,
                             final ServiceCallback<List<StoryModel>> callback) {
        final String url = buildUrl(id, isLoc, isHashtag, highlight);
        final Call<String> userStoryCall = repository.getUserStory(Constants.I_USER_AGENT, url);
        userStoryCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                JSONObject data;
                String localUsername = username;
                try {
                    final String body = response.body();
                    if (body == null) {
                        Log.e(TAG, "body is null");
                        return;
                    }
                    data = new JSONObject(body);

                    if (!highlight)
                        data = data.optJSONObject((isLoc || isHashtag) ? "story" : "reel");
                    else if (highlight) data = data.getJSONObject("reels").optJSONObject(id);

                    if (data != null
                            && localUsername == null
                            && !isLoc
                            && !isHashtag)
                        localUsername = data.getJSONObject("user").getString("username");

                    JSONArray media;
                    if (data != null
                            && (media = data.optJSONArray("items")) != null
                            && media.length() > 0 && media.optJSONObject(0) != null) {

                        final int mediaLen = media.length();
                        final List<StoryModel> models = new ArrayList<>();
                        for (int i = 0; i < mediaLen; ++i) {
                            data = media.getJSONObject(i);
                            models.add(ResponseBodyUtils.parseStoryItem(data, isLoc, isHashtag, localUsername));
                        }
                        callback.onSuccess(models);
                    } else {
                        callback.onSuccess(null);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing string");
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private void respondToSticker(final String storyId,
                                  final String stickerId,
                                  final String action,
                                  final String arg1,
                                  final String arg2,
                                  final String userId,
                                  final String csrfToken,
                                  final ServiceCallback<StoryStickerResponse> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("mutation_token", UUID.randomUUID().toString());
        form.put("client_context", UUID.randomUUID().toString());
        form.put("radio_type", "wifi-none");
        form.put(arg1, arg2);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<StoryStickerResponse> request =
                repository.respondToSticker(Constants.I_USER_AGENT, storyId, stickerId, action, signedForm);
        request.enqueue(new Callback<StoryStickerResponse>() {
            @Override
            public void onResponse(@NonNull final Call<StoryStickerResponse> call,
                                   @NonNull final Response<StoryStickerResponse> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<StoryStickerResponse> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    // RespondAction.java
    public void respondToQuestion(final String storyId,
                                   final String stickerId,
                                   final String answer,
                                   final String userId,
                                   final String csrfToken,
                                   final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_question_response", "response", answer, userId, csrfToken, callback);
    }

    // QuizAction.java
    public void respondToQuiz(final String storyId,
                               final String stickerId,
                               final int answer,
                               final String userId,
                               final String csrfToken,
                               final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_quiz_answer", "answer", String.valueOf(answer), userId, csrfToken, callback);
    }

    // VoteAction.java
    public void respondToPoll(final String storyId,
                               final String stickerId,
                               final int answer,
                               final String userId,
                               final String csrfToken,
                               final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_poll_vote", "vote", String.valueOf(answer), userId, csrfToken, callback);
    }

    public void respondToSlider(final String storyId,
                              final String stickerId,
                              final double answer,
                              final String userId,
                              final String csrfToken,
                              final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_slider_vote", "vote", String.valueOf(answer), userId, csrfToken, callback);
    }

    private String buildUrl(final String id, final boolean isLoc, final boolean isHashtag, final boolean highlight) {
        final String userId = id.replace(":", "%3A");
        final StringBuilder builder = new StringBuilder();
        builder.append("https://i.instagram.com/api/v1/");
        if (isLoc) {
            builder.append("locations/");
        } else if (isHashtag) {
            builder.append("tags/");
        } else if (highlight) {
            builder.append("feed/reels_media/?user_ids=");
        } else {
            builder.append("feed/user/");
        }
        builder.append(userId);
        if (!highlight) {
            builder.append("/story/");
        }
        return builder.toString();
    }

    public class ArchiveFetchResponse {
        private final List<HighlightModel> archives;
        private final boolean hasNextPage;
        private final String nextCursor;

        public ArchiveFetchResponse(final List<HighlightModel> archives, final boolean hasNextPage, final String nextCursor) {
            this.archives = archives;
            this.hasNextPage = hasNextPage;
            this.nextCursor = nextCursor;
        }

        public List<HighlightModel> getArchives() {
            return archives;
        }

        public boolean hasNextPage() {
            return hasNextPage;
        }

        public String getNextCursor() {
            return nextCursor;
        }
    }

}
