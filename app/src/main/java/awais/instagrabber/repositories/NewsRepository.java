package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.utils.Constants;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface NewsRepository {

    Call<String> inbox();

    @FormUrlEncoded
    @Headers("User-Agent: " + Constants.USER_AGENT)
    @POST("https://www.instagram.com/web/activity/mark_checked/")
    Call<String> markChecked(@Header("x-csrftoken") String csrfToken, @FieldMap Map<String, String> map);
}
