package awais.instagrabber.repositories.thirdparty;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface InstadpRepository {

    @GET("stories/{username}")
    Call<String> getUserStory(@Header("User-Agent") String userAgent,
                              @QueryMap(encoded = true) Map<String, String> variables);
}
