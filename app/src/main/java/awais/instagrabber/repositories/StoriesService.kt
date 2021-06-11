package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.StoryStickerResponse
import retrofit2.http.*

interface StoriesService {
    // this one is the same as MediaRepository.fetch BUT you need to make sure it's a story
    @GET("/api/v1/media/{mediaId}/info/")
    suspend fun fetch(@Path("mediaId") mediaId: Long): String

    @GET("/api/v1/feed/reels_tray/")
    suspend fun getFeedStories(): String

    @GET("/api/v1/highlights/{uid}/highlights_tray/")
    suspend fun fetchHighlights(@Path("uid") uid: Long): String

    @GET("/api/v1/archive/reel/day_shells/")
    suspend fun fetchArchive(@QueryMap queryParams: Map<String, String>): String

    @GET
    suspend fun getUserStory(@Url url: String): String

    @FormUrlEncoded
    @POST("/api/v1/media/{storyId}/{stickerId}/{action}/")
    suspend fun respondToSticker(
        @Path("storyId") storyId: String,
        @Path("stickerId") stickerId: String,
        @Path("action") action: String,  // story_poll_vote, story_question_response, story_slider_vote, story_quiz_answer
        @FieldMap form: Map<String, String>,
    ): StoryStickerResponse

    @FormUrlEncoded
    @POST("/api/v2/media/seen/")
    suspend fun seen(
        @QueryMap queryParams: Map<String, String>,
        @FieldMap form: Map<String, String>,
    ): String
}