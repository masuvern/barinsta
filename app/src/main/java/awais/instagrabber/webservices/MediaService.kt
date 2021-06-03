package awais.instagrabber.webservices

import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.MediaRepository
import awais.instagrabber.repositories.requests.Clip
import awais.instagrabber.repositories.requests.UploadFinishOptions
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.DateUtils
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.retryContextString
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import org.json.JSONObject

class MediaService private constructor(
    val deviceUuid: String,
    val csrfToken: String,
    val userId: Long,
) : BaseService() {
    private val repository: MediaRepository = retrofit.create(MediaRepository::class.java)

    suspend fun fetch(
        mediaId: Long,
    ): Media? {
        val response = repository.fetch(mediaId)
        return if (response.items.isNullOrEmpty()) {
            null
        } else response.items[0]
    }

    suspend fun like(mediaId: String): Boolean = action(mediaId, "like", null)

    suspend fun unlike(mediaId: String): Boolean = action(mediaId, "unlike", null)

    suspend fun save(mediaId: String, collection: String?): Boolean = action(mediaId, "save", collection)

    suspend fun unsave(mediaId: String): Boolean = action(mediaId, "unsave", null)

    private suspend fun action(
        mediaId: String,
        action: String,
        collection: String?,
    ): Boolean {
        val form: MutableMap<String, Any> = mutableMapOf(
            "media_id" to mediaId,
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
        )
        // form.put("radio_type", "wifi-none");
        if (action == "save" && !collection.isNullOrBlank()) {
            form["added_collection_ids"] = "[$collection]"
        }
        // there also exists "removed_collection_ids" which can be used with "save" and "unsave"
        val signedForm = Utils.sign(form)
        val response = repository.action(action, mediaId, signedForm)
        val jsonObject = JSONObject(response)
        val status = jsonObject.optString("status")
        return status == "ok"
    }

    suspend fun editCaption(
        postId: String,
        newCaption: String,
    ): Boolean {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "igtv_feed_preview" to "false",
            "media_id" to postId,
            "caption_text" to newCaption,
        )
        val signedForm = Utils.sign(form)
        val response = repository.editCaption(postId, signedForm)
        val jsonObject = JSONObject(response)
        val status = jsonObject.optString("status")
        return status == "ok"
    }

    suspend fun fetchLikes(
        mediaId: String,
        isComment: Boolean,
    ): List<User> {
        val response = repository.fetchLikes(mediaId, if (isComment) "comment_likers" else "likers")
        return response.users
    }

    suspend fun translate(
        id: String,
        type: String,  // 1 caption 2 comment 3 bio
    ): String {
        val form = mapOf(
            "id" to id,
            "type" to type,
        )
        val response = repository.translate(form)
        val jsonObject = JSONObject(response)
        return jsonObject.optString("translation")
    }

    suspend fun uploadFinish(options: UploadFinishOptions): String {
        if (options.videoOptions != null) {
            val videoOptions = options.videoOptions
            if (videoOptions.clips.isEmpty()) {
                videoOptions.clips = listOf(Clip(videoOptions.length, options.sourceType))
            }
        }
        val timezoneOffset = DateUtils.getTimezoneOffset().toString()
        val form = mutableMapOf<String, Any>(
            "timezone_offset" to timezoneOffset,
            "_csrftoken" to csrfToken,
            "source_type" to options.sourceType,
            "_uid" to userId.toString(),
            "_uuid" to deviceUuid,
            "upload_id" to options.uploadId,
        )
        if (options.videoOptions != null) {
            form.putAll(options.videoOptions.map)
        }
        val queryMap = if (options.videoOptions != null) mapOf("video" to "1") else emptyMap()
        val signedForm = Utils.sign(form)
        return repository.uploadFinish(retryContextString, queryMap, signedForm)
    }

    suspend fun delete(
        postId: String,
        type: MediaItemType,
    ): String? {
        if (!DELETABLE_ITEMS_TYPES.contains(type)) return null
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "igtv_feed_preview" to "false",
            "media_id" to postId,
        )
        val signedForm = Utils.sign(form)
        val mediaType: String = when (type) {
            MediaItemType.MEDIA_TYPE_IMAGE -> "PHOTO"
            MediaItemType.MEDIA_TYPE_VIDEO -> "VIDEO"
            MediaItemType.MEDIA_TYPE_SLIDER -> "CAROUSEL"
            else -> return null
        }
        return repository.delete(postId, mediaType, signedForm)
    }

    companion object {
        private val DELETABLE_ITEMS_TYPES = listOf(
            MediaItemType.MEDIA_TYPE_IMAGE,
            MediaItemType.MEDIA_TYPE_VIDEO,
            MediaItemType.MEDIA_TYPE_SLIDER
        )
        private lateinit var instance: MediaService

        @JvmStatic
        fun getInstance(deviceUuid: String, csrfToken: String, userId: Long): MediaService {
            if (!this::instance.isInitialized
                || instance.csrfToken != csrfToken
                || instance.deviceUuid != deviceUuid
                || instance.userId != userId
            ) {
                instance = MediaService(deviceUuid, csrfToken, userId)
            }
            return instance
        }
    }

}