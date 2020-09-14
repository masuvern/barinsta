package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.thirdparty.AloRepository;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AloService extends BaseService {
    private static final String TAG = "AloService";

    private final AloRepository repository;

    private static AloService instance;

    private AloService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://aloinstagram.com")
                .build();
        repository = retrofit.create(AloRepository.class);
    }

    public static AloService getInstance() {
        if (instance == null) {
            instance = new AloService();
        }
        return instance;
    }

    public void getUserStory(final String id,
                             final String username,
                             final boolean highlight,
                             final ServiceCallback<List<StoryModel>> callback) {
        final Call<String> userStoryCall = repository.getUserStory(Constants.A_USER_AGENT, id);
        userStoryCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "body is null");
                    return;
                }
                final Document data = Jsoup.parse(body);
                final Elements media = data.select(".mySpan > a");

                Log.d("austin_debug", id+ ": "+body);

                if (data != null && media != null) {
                    final int mediaLen = media.size();
                    final List<StoryModel> models = new ArrayList<>();
                    for (Element story : media) {

                        final StoryModel model = new StoryModel(null,
                                story.absUrl("href"),
                                story.selectFirst("video") != null ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                -1, // doesn't exist, to handle
                                username,
                                id,
                                false);

                        models.add(model);
                    }
                    callback.onSuccess(models);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }
}
