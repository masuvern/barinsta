package awais.instagrabber.repositories.thirdparty;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Url;

public interface AloRepository {

    @FormUrlEncoded
    @POST("myfile/show.php")
    Call<String> getUserStory(@Header("User-Agent") String userAgent,
                              @Field("storyonId") String id);
}
