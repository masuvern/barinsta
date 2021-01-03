package awais.instagrabber.repositories;

import awais.instagrabber.repositories.responses.UserSearchResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserRepository {

    @GET("/api/v1/users/{uid}/info/")
    Call<String> getUserInfo(@Path("uid") final String uid);

    @GET("/api/v1/users/search/")
    Call<UserSearchResponse> search(@Query("timezone_offset") float timezoneOffset,
                                    @Query("q") String query);
}
