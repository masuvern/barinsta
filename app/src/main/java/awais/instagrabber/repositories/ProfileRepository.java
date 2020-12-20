package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ProfileRepository {

    @GET("/api/v1/users/{uid}/info/")
    Call<String> getUserInfo(@Path("uid") final String uid);

    @GET("/api/v1/feed/user/{uid}/")
    Call<String> fetch(@Path("uid") final String uid, @QueryMap Map<String, String> queryParams);

    @GET("/api/v1/feed/saved/")
    Call<String> fetchSaved(@QueryMap Map<String, String> queryParams);

    @GET("/api/v1/feed/liked/")
    Call<String> fetchLiked(@QueryMap Map<String, String> queryParams);

    @GET("/api/v1/usertags/{profileId}/feed/")
    Call<String> fetchTagged(@Path("profileId") final String profileId, @QueryMap Map<String, String> queryParams);
}
