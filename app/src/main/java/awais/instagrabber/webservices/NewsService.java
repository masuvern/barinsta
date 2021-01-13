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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.repositories.NewsRepository;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NewsService extends BaseService {
    private static final String TAG = "NewsService";

    private final NewsRepository repository;

    private static NewsService instance;
    private static String browserUa, appUa;

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
        appUa = Utils.settingsHelper.getString(Constants.APP_UA);
        browserUa = Utils.settingsHelper.getString(Constants.BROWSER_UA);
        return instance;
    }

    public void fetchAppInbox(final boolean markAsSeen,
                              final ServiceCallback<List<NotificationModel>> callback) {
        final List<NotificationModel> result = new ArrayList<>();
        final Call<String> request = repository.appInbox(appUa, markAsSeen);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final JSONArray oldStories = jsonObject.getJSONArray("old_stories"),
                            newStories = jsonObject.getJSONArray("new_stories");

                    for (int j = 0; j < newStories.length(); ++j) {
                        final NotificationModel newsItem = parseNewsItem(newStories.getJSONObject(j));
                        if (newsItem != null) result.add(newsItem);
                    }

                    for (int i = 0; i < oldStories.length(); ++i) {
                        final NotificationModel newsItem = parseNewsItem(oldStories.getJSONObject(i));
                        if (newsItem != null) result.add(newsItem);
                    }

                    callback.onSuccess(result);
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

    public void fetchWebInbox(final boolean markAsSeen,
                              final ServiceCallback<List<NotificationModel>> callback) {
        final Call<String> request = repository.webInbox(browserUa);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final List<NotificationModel> result = new ArrayList<>();
                    final JSONObject page = new JSONObject(body)
                            .getJSONObject("graphql")
                            .getJSONObject("user");
                    final JSONObject ewaf = page.getJSONObject("activity_feed")
                                                .optJSONObject("edge_web_activity_feed");
                    final JSONObject efr = page.optJSONObject("edge_follow_requests");
                    JSONObject data;
                    JSONArray media;
                    if (ewaf != null
                            && (media = ewaf.optJSONArray("edges")) != null
                            && media.length() > 0
                            && media.optJSONObject(0).optJSONObject("node") != null) {
                        for (int i = 0; i < media.length(); ++i) {
                            data = media.optJSONObject(i).optJSONObject("node");
                            if (data == null) continue;
                            final String type = data.getString("__typename");
                            final NotificationType notificationType = NotificationType.valueOfType(type);
                            if (notificationType == null) continue;
                            final JSONObject user = data.getJSONObject("user");
                            result.add(new NotificationModel(
                                    data.getString(Constants.EXTRAS_ID),
                                    data.optString("text"), // comments or mentions
                                    data.getLong("timestamp"),
                                    user.getLong("id"),
                                    user.getString("username"),
                                    user.getString("profile_pic_url"),
                                    data.has("media") ? data.getJSONObject("media").getLong("id") : 0,
                                    data.has("media") ? data.getJSONObject("media").getString("thumbnail_src") : null,
                                    notificationType));
                        }
                    }

                    if (efr != null
                            && (media = efr.optJSONArray("edges")) != null
                            && media.length() > 0
                            && media.optJSONObject(0).optJSONObject("node") != null) {
                        for (int i = 0; i < media.length(); ++i) {
                            data = media.optJSONObject(i).optJSONObject("node");
                            if (data == null) continue;
                            result.add(new NotificationModel(
                                    data.getString(Constants.EXTRAS_ID),
                                    data.optString("full_name"),
                                    0L,
                                    data.getLong(Constants.EXTRAS_ID),
                                    data.getString("username"),
                                    data.getString("profile_pic_url"),
                                    0,
                                    null, NotificationType.REQUEST));
                        }
                    }
                    callback.onSuccess(result);
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

    private NotificationModel parseNewsItem(final JSONObject itemJson) throws JSONException {
        if (itemJson == null) return null;
        final String type = itemJson.getString("story_type");
        final NotificationType notificationType = NotificationType.valueOfType(type);
        if (notificationType == null) {
            if (BuildConfig.DEBUG) Log.d("austin_debug", "unhandled news type: " + itemJson);
            return null;
        }
        final JSONObject data = itemJson.getJSONObject("args");
        return new NotificationModel(
                data.getString("tuuid"),
                data.has("text") ? data.getString("text") : cleanRichText(data.optString("rich_text", "")),
                data.getLong("timestamp"),
                data.getLong("profile_id"),
                data.getString("profile_name"),
                data.getString("profile_image"),
                !data.isNull("media") ? Long.valueOf(data.getJSONArray("media").getJSONObject(0).getString("id").split("_")[0]) : 0,
                !data.isNull("media") ? data.getJSONArray("media").getJSONObject(0).getString("image") : null,
                notificationType);
    }

    private String cleanRichText(final String raw) {
        final Matcher matcher = Pattern.compile("\\{[\\p{L}\\d._]+\\|000000\\|1\\|user\\?id=\\d+\\}").matcher(raw);
        String result = raw;
        while (matcher.find()) {
            final String richObject = raw.substring(matcher.start(), matcher.end());
            final String username = richObject.split("\\|")[0].substring(1);
            result = result.replace(richObject, username);
        }
        return result;
    }

    public void fetchSuggestions(final String csrfToken,
                                 final ServiceCallback<List<NotificationModel>> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("_csrftoken", csrfToken);
        form.put("phone_id", UUID.randomUUID().toString());
        form.put("device_id", UUID.randomUUID().toString());
        form.put("module", "discover_people");
        form.put("paginate", "false");
        final Call<String> request = repository.getAyml(appUa, form);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final List<NotificationModel> result = new ArrayList<>();
                    final JSONObject jsonObject = new JSONObject(body);
                    final JSONArray oldStories = jsonObject.getJSONObject("suggested_users").getJSONArray("suggestions"),
                            newStories = jsonObject.getJSONObject("new_suggested_users").getJSONArray("suggestions");

                    for (int j = 0; j < newStories.length(); ++j) {
                        final NotificationModel newsItem = parseAymlItem(newStories.getJSONObject(j));
                        if (newsItem != null) result.add(newsItem);
                    }

                    for (int i = 0; i < oldStories.length(); ++i) {
                        final NotificationModel newsItem = parseAymlItem(oldStories.getJSONObject(i));
                        if (newsItem != null) result.add(newsItem);
                    }

                    callback.onSuccess(result);
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

    private NotificationModel parseAymlItem(final JSONObject itemJson) throws JSONException {
        if (itemJson == null) return null;
        final JSONObject data = itemJson.getJSONObject("user");
        return new NotificationModel(
                itemJson.getString("uuid"),
                itemJson.getString("social_context"),
                0L,
                data.getLong("pk"),
                data.getString("username"),
                data.getString("profile_pic_url"),
                0,
                data.getString("full_name"), // just borrowing this field
                NotificationType.AYML);
    }
}
