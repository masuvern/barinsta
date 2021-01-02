package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.directmessages.DirectBadgeCount;
import awais.instagrabber.repositories.responses.directmessages.DirectInboxResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadFeedResponse;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface DirectMessagesRepository {

    @GET("/api/v1/direct_v2/inbox/")
    Call<DirectInboxResponse> fetchInbox(@QueryMap Map<String, Object> queryMap);

    @GET("/api/v1/direct_v2/threads/{threadId}/")
    Call<DirectThreadFeedResponse> fetchThread(@Path("threadId") String threadId,
                                               @QueryMap Map<String, Object> queryMap);

    @GET("/api/v1/direct_v2/get_badge_count/")
    Call<DirectBadgeCount> fetchUnseenCount();

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/broadcast/{item}/")
    Call<DirectThreadBroadcastResponse> broadcast(@Path("item") String item,
                                                  @FieldMap final Map<String, String> signedForm);
}
