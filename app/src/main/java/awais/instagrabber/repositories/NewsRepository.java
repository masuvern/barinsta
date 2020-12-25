package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.utils.Constants;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NewsRepository {

    @Headers("User-Agent: " + Constants.USER_AGENT)
    @GET("https://www.instagram.com/accounts/activity/?__a=1")
    Call<String> webInbox();

    @Headers("User-Agent: " + Constants.I_USER_AGENT)
    @GET("/api/v1/news/inbox/")
    Call<String> appInbox(@Query(value = "mark_as_seen", encoded = true) boolean markAsSeen);
}
