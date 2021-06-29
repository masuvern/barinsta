package awais.instagrabber.webservices

import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.models.HighlightModel
import awais.instagrabber.models.StoryModel
import awais.instagrabber.repositories.StoriesService
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.stories.Story
import awais.instagrabber.repositories.responses.stories.StoryStickerResponse
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.ResponseBodyUtils
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

open class StoriesRepository(private val service: StoriesService) {

    suspend fun fetch(mediaId: Long): StoryModel {
        val response = service.fetch(mediaId)
        val itemJson = JSONObject(response).getJSONArray("items").getJSONObject(0)
        return ResponseBodyUtils.parseStoryItem(itemJson, false, null)
    }

    suspend fun getFeedStories(): List<Story> {
        val response = service.getFeedStories()
        val result = response.tray?.toMutableList() ?: mutableListOf()
        if (response.broadcasts != null) {
            val length = response.broadcasts.size
            for (i in 0 until length) {
                val broadcast = response.broadcasts.get(i)
                result.add(
                    Story(
                        broadcast.id,
                        broadcast.publishedTime,
                        0L,
                        broadcast.broadcastOwner,
                        broadcast.muted,
                        false, // unclear
                        1,
                        null,
                        broadcast
                    )
                )
            }
        }
        return sort(result.toList())
    }

    open suspend fun fetchHighlights(profileId: Long): List<HighlightModel> {
        val response = service.fetchHighlights(profileId)
        val highlightsReel = JSONObject(response).getJSONArray("tray")
        val length = highlightsReel.length()
        val highlightModels: MutableList<HighlightModel> = ArrayList()
        for (i in 0 until length) {
            val highlightNode = highlightsReel.getJSONObject(i)
            highlightModels.add(
                HighlightModel(
                    highlightNode.getString("title"),
                    highlightNode.getString(Constants.EXTRAS_ID),
                    highlightNode.getJSONObject("cover_media")
                        .getJSONObject("cropped_image_version")
                        .getString("url"),
                    highlightNode.getLong("latest_reel_media"),
                    highlightNode.getInt("media_count")
                )
            )
        }
        return highlightModels
    }

    suspend fun fetchArchive(maxId: String): ArchiveFetchResponse {
        val form = mutableMapOf(
            "include_suggested_highlights" to "false",
            "is_in_archive_home" to "true",
            "include_cover" to "1",
        )
        if (!isEmpty(maxId)) {
            form["max_id"] = maxId // NOT TESTED
        }
        val response = service.fetchArchive(form)
        val data = JSONObject(response)
        val highlightsReel = data.getJSONArray("items")
        val length = highlightsReel.length()
        val highlightModels: MutableList<HighlightModel> = ArrayList()
        for (i in 0 until length) {
            val highlightNode = highlightsReel.getJSONObject(i)
            highlightModels.add(
                HighlightModel(
                    null,
                    highlightNode.getString(Constants.EXTRAS_ID),
                    highlightNode.getJSONObject("cover_image_version").getString("url"),
                    highlightNode.getLong("latest_reel_media"),
                    highlightNode.getInt("media_count")
                )
            )
        }
        return ArchiveFetchResponse(highlightModels, data.getBoolean("more_available"), data.getString("max_id"))
    }

    open suspend fun getUserStory(options: StoryViewerOptions): List<StoryModel> {
        val url = buildUrl(options) ?: return emptyList()
        val response = service.getUserStory(url)
        val isLocOrHashtag = options.type == StoryViewerOptions.Type.LOCATION || options.type == StoryViewerOptions.Type.HASHTAG
        val isHighlight = options.type == StoryViewerOptions.Type.HIGHLIGHT || options.type == StoryViewerOptions.Type.STORY_ARCHIVE
        var data: JSONObject? = JSONObject(response)
        data = if (!isHighlight) {
            data?.optJSONObject(if (isLocOrHashtag) "story" else "reel")
        } else {
            data?.getJSONObject("reels")?.optJSONObject(options.name)
        }
        var username: String? = null
        if (data != null && !isLocOrHashtag) {
            username = data.getJSONObject("user").getString("username")
        }
        val media: JSONArray? = data?.optJSONArray("items")
        return if (media?.length() ?: 0 > 0 && media?.optJSONObject(0) != null) {
            val mediaLen = media.length()
            val models: MutableList<StoryModel> = ArrayList()
            for (i in 0 until mediaLen) {
                data = media.getJSONObject(i)
                models.add(ResponseBodyUtils.parseStoryItem(data, isLocOrHashtag, username))
            }
            models
        } else emptyList()
    }

    private suspend fun respondToSticker(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: String,
        stickerId: String,
        action: String,
        arg1: String,
        arg2: String,
    ): StoryStickerResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "mutation_token" to UUID.randomUUID().toString(),
            "client_context" to UUID.randomUUID().toString(),
            "radio_type" to "wifi-none",
            arg1 to arg2,
        )
        val signedForm = Utils.sign(form)
        return service.respondToSticker(storyId, stickerId, action, signedForm)
    }

    suspend fun respondToQuestion(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: String,
        stickerId: String,
        answer: String,
    ): StoryStickerResponse = respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_question_response", "response", answer)

    suspend fun respondToQuiz(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: String,
        stickerId: String,
        answer: Int,
    ): StoryStickerResponse {
        return respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_quiz_answer", "answer", answer.toString())
    }

    suspend fun respondToPoll(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: String,
        stickerId: String,
        answer: Int,
    ): StoryStickerResponse = respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_poll_vote", "vote", answer.toString())

    suspend fun respondToSlider(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyId: String,
        stickerId: String,
        answer: Double,
    ): StoryStickerResponse = respondToSticker(csrfToken, userId, deviceUuid, storyId, stickerId, "story_slider_vote", "vote", answer.toString())

    suspend fun seen(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        storyMediaId: String,
        takenAt: Long,
        seenAt: Long,
    ): String {
        val reelsForm = mapOf(storyMediaId to listOf(takenAt.toString() + "_" + seenAt))
        val form = mutableMapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "container_module" to "feed_timeline",
            "reels" to reelsForm,
        )
        val signedForm = Utils.sign(form)
        val queryMap = mapOf(
            "reel" to "1",
            "live_vod" to "0",
        )
        return service.seen(queryMap, signedForm)
    }

    private fun buildUrl(options: StoryViewerOptions): String? {
        val builder = StringBuilder()
        builder.append("https://i.instagram.com/api/v1/")
        val type = options.type
        var id: String? = null
        when (type) {
            StoryViewerOptions.Type.HASHTAG -> {
                builder.append("tags/")
                id = options.name
            }
            StoryViewerOptions.Type.LOCATION -> {
                builder.append("locations/")
                id = options.id.toString()
            }
            StoryViewerOptions.Type.USER -> {
                builder.append("feed/user/")
                id = options.id.toString()
            }
            StoryViewerOptions.Type.HIGHLIGHT, StoryViewerOptions.Type.STORY_ARCHIVE -> {
                builder.append("feed/reels_media/?user_ids=")
                id = options.name
            }
            StoryViewerOptions.Type.STORY -> {
            }
            else -> {
            }
        }
        if (id == null) {
            return null
        }
        builder.append(id)
        if (type != StoryViewerOptions.Type.HIGHLIGHT && type != StoryViewerOptions.Type.STORY_ARCHIVE) {
            builder.append("/story/")
        }
        return builder.toString()
    }

    private fun sort(list: List<Story>): List<Story> {
        val listCopy = ArrayList(list)
        listCopy.sortWith { o1, o2 ->
            if (o1.latestReelMedia == null || o2.latestReelMedia == null) return@sortWith 0
            else when (Utils.settingsHelper.getString(PreferenceKeys.STORY_SORT)) {
                "1" -> return@sortWith o2.latestReelMedia.compareTo(o1.latestReelMedia)
                "2" -> return@sortWith o1.latestReelMedia.compareTo(o2.latestReelMedia)
                else -> return@sortWith 0
            }
        }
        return listCopy
    }

    class ArchiveFetchResponse(val result: List<HighlightModel>, val hasNextPage: Boolean, val nextCursor: String) {
        fun hasNextPage(): Boolean {
            return hasNextPage
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: StoriesRepository? = null

        fun getInstance(): StoriesRepository {
            return INSTANCE ?: synchronized(this) {
                val service: StoriesService = retrofit.create(StoriesService::class.java)
                StoriesRepository(service).also { INSTANCE = it }
            }
        }
    }
}