package awais.instagrabber.repositories;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ProfileRepository {

    @GET("api/v1/users/{uid}/info/")
    Call<String> getUserInfo(@Path("uid") final String uid);
}
