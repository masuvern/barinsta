package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.FriendshipRepositoryChangeResponseRootObject;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FriendshipRepository {

    @FormUrlEncoded
    @POST("/api/v1/friendships/{action}/{id}/")
    Call<FriendshipRepositoryChangeResponseRootObject> change(@Path("action") String action,
                                                              @Path("id") String id,
                                                              @FieldMap Map<String, String> form);
}
