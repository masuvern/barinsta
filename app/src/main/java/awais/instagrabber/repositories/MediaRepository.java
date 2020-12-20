package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MediaRepository {
    @GET("/api/v1/media/{mediaId}/likers/")
    Call<String> fetchLikes(@Header("User-Agent") final String userAgent,
                            @Path("mediaId") final String mediaId);

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/{action}/")
    Call<String> action(@Header("User-Agent") final String userAgent,
                        @Path("action") final String action,
                        @Path("mediaId") final String mediaId,
                        @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/comment/")
    Call<String> comment(@Header("User-Agent") final String userAgent,
                         @Path("mediaId") final String mediaId,
                         @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/comment/bulk_delete/")
    Call<String> commentsBulkDelete(@Header("User-Agent") final String userAgent,
                                    @Path("mediaId") final String mediaId,
                                    @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{commentId}/comment_like/")
    Call<String> commentLike(@Header("User-Agent") final String userAgent,
                             @Path("commentId") final String commentId,
                             @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{commentId}/comment_unlike/")
    Call<String> commentUnlike(@Header("User-Agent") final String userAgent,
                               @Path("commentId") final String commentId,
                               @FieldMap final Map<String, String> signedForm);
}
