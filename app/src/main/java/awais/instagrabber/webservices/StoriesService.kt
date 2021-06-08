package awais.instagrabber.webservices

import android.util.Log
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.models.FeedStoryModel
import awais.instagrabber.models.HighlightModel
import awais.instagrabber.models.StoryModel
import awais.instagrabber.repositories.StoriesRepository
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.StoryStickerResponse
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.ResponseBodyUtils
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object StoriesService {
    private val repository: StoriesRepository = retrofit.create(StoriesRepository::class.java)

    suspend fun fetch(mediaId: Long): StoryModel {
        val response = repository.fetch(mediaId)
        val itemJson = JSONObject(response).getJSONArray("items").getJSONObject(0)
        return ResponseBodyUtils.parseStoryItem(itemJson, false, null)
    }

    suspend fun getFeedStories(): List<FeedStoryModel> {
        val response = repository.getFeedStories()
        return parseStoriesBody(response)
    }

    private fun parseStoriesBody(body: String): List<FeedStoryModel> {
        val feedStoryModels: MutableList<FeedStoryModel> = ArrayList()
        val feedStoriesReel = JSONObject(body).getJSONArray("tray")
        for (i in 0 until feedStoriesReel.length()) {
            val node = feedStoriesReel.getJSONObject(i)
            if (node.optBoolean("hide_from_feed_unit") && Utils.settingsHelper.getBoolean(PreferenceKeys.HIDE_MUTED_REELS)) continue
            val userJson = node.getJSONObject(if (node.has("user")) "user" else "owner")
            try {
                val user = User(userJson.getLong("pk"),
                    userJson.getString("username"),
                    userJson.optString("full_name"),
                    userJson.optBoolean("is_private"),
                    userJson.getString("profile_pic_url"),
                    userJson.optBoolean("is_verified")
                )
                val timestamp = node.getLong("latest_reel_media")
                val fullyRead = !node.isNull("seen") && node.getLong("seen") == timestamp
                val itemJson = if (node.has("items")) node.getJSONArray("items").optJSONObject(0) else null
                var firstStoryModel: StoryModel? = null
                if (itemJson != null) {
                    firstStoryModel = ResponseBodyUtils.parseStoryItem(itemJson, false, null)
                }
                feedStoryModels.add(FeedStoryModel(
                    node.getString("id"),
                    user,
                    fullyRead,
                    timestamp,
                    firstStoryModel,
                    node.getInt("media_count"),
                    false,
                    node.optBoolean("has_besties_media")))
            } catch (e: Exception) {
                Log.e(TAG, "parseStoriesBody: ", e)
            } // to cover promotional reels with non-long user pk's
        }
        val broadcasts = JSONObject(body).getJSONArray("broadcasts")
        for (i in 0 until broadcasts.length()) {
            val node = broadcasts.getJSONObject(i)
            val userJson = node.getJSONObject("broadcast_owner")
            val user = User(userJson.getLong("pk"),
                userJson.getString("username"),
                userJson.optString("full_name"),
                userJson.optBoolean("is_private"),
                userJson.getString("profile_pic_url"),
                userJson.optBoolean("is_verified")
            )
            feedStoryModels.add(FeedStoryModel(
                node.getString("id"),
                user,
                false,
                node.getLong("published_time"),
                ResponseBodyUtils.parseBroadcastItem(node),
                1,
                isLive = true,
                isBestie = false
            ))
        }
        return sort(feedStoryModels)
    }

    suspend fun fetchHighlights(profileId: Long): List<HighlightModel> {
        val response = repository.fetchHighlights(profileId)
        val highlightsReel = JSONObject(response).getJSONArray("tray")
        val length = highlightsReel.length()
        val highlightModels: MutableList<HighlightModel> = ArrayList()
        for (i in 0 until length) {
            val highlightNode = highlightsReel.getJSONObject(i)
            highlightModels.add(HighlightModel(
                highlightNode.getString("title"),
                highlightNode.getString(Constants.EXTRAS_ID),
                highlightNode.getJSONObject("cover_media")
                    .getJSONObject("cropped_image_version")
                    .getString("url"),
                highlightNode.getLong("latest_reel_media"),
                highlightNode.getInt("media_count")
            ))
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
        val response = repository.fetchArchive(form)
        val data = JSONObject(response)
        val highlightsReel = data.getJSONArray("items")
        val length = highlightsReel.length()
        val highlightModels: MutableList<HighlightModel> = ArrayList()
        for (i in 0 until length) {
            val highlightNode = highlightsReel.getJSONObject(i)
            highlightModels.add(HighlightModel(
                null,
                highlightNode.getString(Constants.EXTRAS_ID),
                highlightNode.getJSONObject("cover_image_version").getString("url"),
                highlightNode.getLong("latest_reel_media"),
                highlightNode.getInt("media_count")
            ))
        }
        return ArchiveFetchResponse(highlightModels, data.getBoolean("more_available"), data.getString("max_id"))
    }

    suspend fun getUserStory(options: StoryViewerOptions): List<StoryModel> {
        val url = buildUrl(options) ?: return emptyList()
        val response = repository.getUserStory(url)
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
        return repository.respondToSticker(storyId, stickerId, action, signedForm)
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
        return repository.seen(queryMap, signedForm)
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

    private fun sort(list: List<FeedStoryModel>): List<FeedStoryModel> {
        val listCopy = ArrayList(list)
        listCopy.sortWith { o1, o2 ->
            when (Utils.settingsHelper.getString(PreferenceKeys.STORY_SORT)) {
                "1" -> return@sortWith o2.timestamp.compareTo(o1.timestamp)
                "2" -> return@sortWith o1.timestamp.compareTo(o2.timestamp)
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
}