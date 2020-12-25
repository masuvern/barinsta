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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.models.stickers.SliderModel;
import awais.instagrabber.models.stickers.SwipeUpModel;
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
                final boolean fullyRead = !node.isNull("seen") && node.getLong("seen") == node.getLong("latest_reel_media");
                feedStoryModels.add(new FeedStoryModel(id, profileModel, fullyRead));
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
                    // final String[] highlightIds = new String[length];
                    for (int i = 0; i < length; ++i) {
                        final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                        highlightModels.add(new HighlightModel(
                                highlightNode.getString("title"),
                                highlightNode.getString(Constants.EXTRAS_ID),
                                highlightNode.getJSONObject("cover_media")
                                        .getJSONObject("cropped_image_version")
                                        .getString("url")
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
                            final boolean isVideo = data.has("video_duration");
                            final StoryModel model = new StoryModel(data.getString("id"),
                                                                    data.getJSONObject("image_versions2").getJSONArray("candidates").getJSONObject(0)
                                                                        .getString("url"),
                                                                    isVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                                                    data.optLong("taken_at", 0),
                                                                    (isLoc || isHashtag)
                                                                    ? data.getJSONObject("user").getString("username")
                                                                    : localUsername,
                                                                    data.getJSONObject("user").getString("pk"),
                                                                    data.getBoolean("can_reply"));

                            final JSONArray videoResources = data.optJSONArray("video_versions");
                            if (isVideo && videoResources != null)
                                model.setVideoUrl(ResponseBodyUtils.getHighQualityPost(videoResources, true, true, false));

                            if (data.has("story_feed_media")) {
                                model.setTappableShortCode(data.getJSONArray("story_feed_media").getJSONObject(0).optString("media_code"));
                            }

                            // TODO: this may not be limited to spotify
                            if (!data.isNull("story_app_attribution"))
                                model.setSpotify(data.getJSONObject("story_app_attribution").optString("content_url").split("\\?")[0]);

                            if (data.has("story_polls")) {
                                final JSONArray storyPolls = data.optJSONArray("story_polls");
                                JSONObject tappableObject = null;
                                if (storyPolls != null) {
                                    tappableObject = storyPolls.getJSONObject(0).optJSONObject("poll_sticker");
                                }
                                if (tappableObject != null) model.setPoll(new PollModel(
                                        String.valueOf(tappableObject.getLong("poll_id")),
                                        tappableObject.getString("question"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(0).getString("text"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(0).getInt("count"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(1).getString("text"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(1).getInt("count"),
                                        tappableObject.optInt("viewer_vote", -1)
                                ));
                            }
                            if (data.has("story_questions")) {
                                final JSONObject tappableObject = data.getJSONArray("story_questions").getJSONObject(0)
                                                                      .optJSONObject("question_sticker");
                                if (tappableObject != null && !tappableObject.getString("question_type").equals("music"))
                                    model.setQuestion(new QuestionModel(
                                            String.valueOf(tappableObject.getLong("question_id")),
                                            tappableObject.getString("question")
                                    ));
                            }
                            if (data.has("story_quizs")) {
                                JSONObject tappableObject = data.getJSONArray("story_quizs").getJSONObject(0).optJSONObject("quiz_sticker");
                                if (tappableObject != null) {
                                    String[] choices = new String[tappableObject.getJSONArray("tallies").length()];
                                    Long[] counts = new Long[choices.length];
                                    for (int q = 0; q < choices.length; ++q) {
                                        JSONObject tempchoice = tappableObject.getJSONArray("tallies").getJSONObject(q);
                                        choices[q] = (q == tappableObject.getInt("correct_answer") ? "*** " : "")
                                                + tempchoice.getString("text");
                                        counts[q] = tempchoice.getLong("count");
                                    }
                                    model.setQuiz(new QuizModel(
                                            String.valueOf(tappableObject.getLong("quiz_id")),
                                            tappableObject.getString("question"),
                                            choices,
                                            counts,
                                            tappableObject.optInt("viewer_answer", -1)
                                    ));
                                }
                            }
                            if (data.has("story_cta") && data.has("link_text")) {
                                JSONObject tappableObject = data.getJSONArray("story_cta").getJSONObject(0).getJSONArray("links").getJSONObject(0);
                                String swipeUpUrl = tappableObject.getString("webUri");
                                if (swipeUpUrl.startsWith("http")) {
                                    model.setSwipeUp(new SwipeUpModel(swipeUpUrl, data.getString("link_text")));
                                }
                            }
                            if (data.has("story_sliders")) {
                                final JSONObject tappableObject = data.getJSONArray("story_sliders").getJSONObject(0)
                                        .optJSONObject("slider_sticker");
                                if (tappableObject != null)
                                    model.setSlider(new SliderModel(
                                            String.valueOf(tappableObject.getLong("slider_id")),
                                            tappableObject.getString("question"),
                                            tappableObject.getString("emoji"),
                                            tappableObject.getBoolean("viewer_can_vote"),
                                            tappableObject.getDouble("slider_vote_average"),
                                            tappableObject.getInt("slider_vote_count"),
                                            tappableObject.optDouble("viewer_vote")
                                    ));
                            }
                            JSONArray hashtags = data.optJSONArray("story_hashtags");
                            JSONArray locations = data.optJSONArray("story_locations");
                            JSONArray atmarks = data.optJSONArray("reel_mentions");
                            String[] mentions = new String[(hashtags == null ? 0 : hashtags.length())
                                    + (atmarks == null ? 0 : atmarks.length())
                                    + (locations == null ? 0 : locations.length())];
                            if (hashtags != null) {
                                for (int h = 0; h < hashtags.length(); ++h) {
                                    mentions[h] = "#" + hashtags.getJSONObject(h).getJSONObject("hashtag").getString("name");
                                }
                            }
                            if (atmarks != null) {
                                for (int h = 0; h < atmarks.length(); ++h) {
                                    mentions[h + (hashtags == null ? 0 : hashtags.length())] =
                                            "@" + atmarks.getJSONObject(h).getJSONObject("user").getString("username");
                                }
                            }
                            if (locations != null) {
                                for (int h = 0; h < locations.length(); ++h) {
                                    mentions[h + (hashtags == null ? 0 : hashtags.length()) + (atmarks == null ? 0 : atmarks.length())] =
                                            locations.getJSONObject(h).getJSONObject("location").getString("short_name")
                                                    + " (" + locations.getJSONObject(h).getJSONObject("location").getLong("pk") + ")";
                                }
                            }
                            if (mentions.length != 0) model.setMentions(mentions);
                            models.add(model);
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
}
