package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ProfileRepository {

    @GET("api/v1/users/{uid}/info/")
    Call<String> getUserInfo(@Path("uid") final String uid);

    @GET("/graphql/query/")
    Call<String> fetch(@QueryMap Map<String, String> queryMap);
}
