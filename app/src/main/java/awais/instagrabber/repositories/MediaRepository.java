package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MediaRepository {

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/{action}/")
    Call<String> likeAction(@Header("User-Agent") final String userAgent,
                            @Path("action") final String action,
                            @Path("mediaId") final String mediaId,
                            @FieldMap final Map<String, String> signedForm);
}
