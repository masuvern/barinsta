package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.TagFeedResponse;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface TagsRepository {
    @FormUrlEncoded
    @POST("/api/v1/tags/follow/{tag}/")
    Call<String> follow(@FieldMap final Map<String, String> signedForm,
                        @Path("tag") String tag);

    @FormUrlEncoded
    @POST("/api/v1/tags/unfollow/{tag}/")
    Call<String> unfollow(@FieldMap final Map<String, String> signedForm,
                          @Path("tag") String tag);

    @GET("/api/v1/feed/tag/{tag}/")
    Call<TagFeedResponse> fetchPosts(@Path("tag") final String tag,
                                     @QueryMap Map<String, String> queryParams);
}
