package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.StoryStickerResponse;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public interface StoriesRepository {
    @GET("/api/v1/media/{mediaId}/info/")
    Call<String> fetch(@Path("mediaId") final String mediaId);
    // this one is the same as MediaRepository.fetch BUT you need to make sure it's a story

    @FormUrlEncoded
    @POST("/api/v1/feed/reels_tray/")
    Call<String> getFeedStories(@Header("User-Agent") String userAgent,
                                @FieldMap Map<String, String> form);

    @GET("/api/v1/highlights/{uid}/highlights_tray/")
    Call<String> fetchHighlights(@Path("uid") final String uid);

    @GET
    Call<String> getUserStory(@Header("User-Agent") String userAgent, @Url String url);

    @FormUrlEncoded
    @POST("/api/v1/media/{storyId}/{stickerId}/{action}/")
    Call<StoryStickerResponse> respondToSticker(@Header("User-Agent") String userAgent,
                                                @Path("storyId") String storyId,
                                                @Path("stickerId") String stickerId,
                                                @Path("action") String action,
                                                // story_poll_vote, story_question_response, story_slider_vote, story_quiz_answer
                                                @FieldMap Map<String, String> form);
}
