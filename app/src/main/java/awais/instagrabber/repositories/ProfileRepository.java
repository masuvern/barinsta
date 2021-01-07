package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.UserFeedResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ProfileRepository {

    @GET("/api/v1/feed/user/{uid}/")
    Call<UserFeedResponse> fetch(@Path("uid") final long uid, @QueryMap Map<String, String> queryParams);

    @GET("/api/v1/feed/saved/")
    Call<UserFeedResponse> fetchSaved(@QueryMap Map<String, String> queryParams);

    @GET("/api/v1/feed/liked/")
    Call<UserFeedResponse> fetchLiked(@QueryMap Map<String, String> queryParams);

    @GET("/api/v1/usertags/{profileId}/feed/")
    Call<UserFeedResponse> fetchTagged(@Path("profileId") final long profileId, @QueryMap Map<String, String> queryParams);
}
