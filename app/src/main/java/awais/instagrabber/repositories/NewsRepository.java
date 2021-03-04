package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.AymlResponse;
import awais.instagrabber.repositories.responses.NewsInboxResponse;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NewsRepository {

    @GET("https://www.instagram.com/accounts/activity/?__a=1")
    Call<String> webInbox(@Header("User-Agent") String userAgent);

    @GET("/api/v1/news/inbox/")
    Call<NewsInboxResponse> appInbox(@Header("User-Agent") String userAgent, @Query(value = "mark_as_seen", encoded = true) boolean markAsSeen);

    @FormUrlEncoded
    @POST("/api/v1/discover/ayml/")
    Call<AymlResponse> getAyml(@Header("User-Agent") String userAgent, @FieldMap final Map<String, String> form);
}
