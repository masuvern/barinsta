package awais.instagrabber.webservices;

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
import java.util.stream.Collectors;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.repositories.NewsRepository;
import awais.instagrabber.repositories.responses.AymlResponse;
import awais.instagrabber.repositories.responses.AymlUser;
import awais.instagrabber.repositories.responses.NewsInboxResponse;
import awais.instagrabber.repositories.responses.Notification;
import awais.instagrabber.repositories.responses.NotificationArgs;
import awais.instagrabber.repositories.responses.NotificationImage;
import awais.instagrabber.repositories.responses.User;
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
                              final ServiceCallback<List<Notification>> callback) {
        final Call<NewsInboxResponse> request = repository.appInbox(appUa, markAsSeen);
        request.enqueue(new Callback<NewsInboxResponse>() {
            @Override
            public void onResponse(@NonNull final Call<NewsInboxResponse> call, @NonNull final Response<NewsInboxResponse> response) {
                final NewsInboxResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                final List<Notification> result = new ArrayList<>();
                result.addAll(body.getNewStories());
                result.addAll(body.getOldStories());
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(@NonNull final Call<NewsInboxResponse> call, @NonNull final Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    public void fetchWebInbox(final ServiceCallback<List<Notification>> callback) {
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
                    final List<Notification> result = new ArrayList<>();
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

                            result.add(new Notification(
                                    new NotificationArgs(
                                            data.optString("text"),
                                            null,
                                            user.getLong(Constants.EXTRAS_ID),
                                            user.getString("profile_pic_url"),
                                            data.isNull("media") ? null : Collections.singletonList(new NotificationImage(
                                                    data.getJSONObject("media").getString("id"),
                                                    data.getJSONObject("media").getString("thumbnail_src")
                                            )),
                                            data.getLong("timestamp"),
                                            user.getString("username"),
                                            null
                                    ),
                                    type,
                                    data.getString(Constants.EXTRAS_ID)
                            ));
                        }
                    }

                    if (efr != null
                            && (media = efr.optJSONArray("edges")) != null
                            && media.length() > 0
                            && media.optJSONObject(0).optJSONObject("node") != null) {
                        for (int i = 0; i < media.length(); ++i) {
                            data = media.optJSONObject(i).optJSONObject("node");
                            if (data == null) continue;
                            result.add(new Notification(
                                    new NotificationArgs(
                                            null,
                                            null,
                                            data.getLong(Constants.EXTRAS_ID),
                                            data.getString("profile_pic_url"),
                                            null,
                                            0L,
                                            data.getString("username"),
                                            data.optString("full_name")
                                    ),
                                    "REQUEST",
                                    data.getString(Constants.EXTRAS_ID)
                            ));
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

    public void fetchSuggestions(final String csrfToken,
                                 final ServiceCallback<List<Notification>> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("_csrftoken", csrfToken);
        form.put("phone_id", UUID.randomUUID().toString());
        form.put("device_id", UUID.randomUUID().toString());
        form.put("module", "discover_people");
        form.put("paginate", "false");
        final Call<AymlResponse> request = repository.getAyml(appUa, form);
        request.enqueue(new Callback<AymlResponse>() {
            @Override
            public void onResponse(@NonNull final Call<AymlResponse> call, @NonNull final Response<AymlResponse> response) {
                final AymlResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                final List<AymlUser> aymlUsers = new ArrayList<>();
                aymlUsers.addAll(body.getNewSuggestedUsers().getSuggestions());
                aymlUsers.addAll(body.getSuggestedUsers().getSuggestions());

                final List<Notification> newsItems = aymlUsers.stream()
                        .map(i -> {
                            final User u = i.getUser();
                            return new Notification(
                                    new NotificationArgs(
                                            i.getSocialContext(),
                                            i.getAlgorithm(),
                                            u.getPk(),
                                            u.getProfilePicUrl(),
                                            null,
                                            0L,
                                            u.getUsername(),
                                            u.getFullName()
                                    ),
                                    "AYML",
                                    i.getUuid()
                            );
                        })
                        .collect(Collectors.toList());
                callback.onSuccess(newsItems);
            }

            @Override
            public void onFailure(@NonNull final Call<AymlResponse> call, @NonNull final Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }
}
