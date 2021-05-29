package awais.instagrabber.webservices

import awais.instagrabber.repositories.DirectMessagesRepository
import awais.instagrabber.repositories.requests.directmessages.*
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.TextUtils.extractUrls
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import org.json.JSONArray
import retrofit2.Call
import java.util.*

class DirectMessagesService private constructor(
    val csrfToken: String,
    val userId: Long,
    val deviceUuid: String,
) : BaseService() {
    private val repository: DirectMessagesRepository = RetrofitFactory.retrofit.create(DirectMessagesRepository::class.java)

    fun fetchInbox(
        cursor: String?,
        seqId: Long,
    ): Call<DirectInboxResponse?> {
        val queryMap = mutableMapOf(
            "visual_message_return_type" to "unseen",
            "thread_message_limit" to 10.toString(),
            "persistentBadging" to true.toString(),
            "limit" to 10.toString(),
        )
        if (!cursor.isNullOrBlank()) {
            queryMap["cursor"] = cursor
            queryMap["direction"] = "older"
        }
        if (seqId != 0L) {
            queryMap["seq_id"] = seqId.toString()
        }
        return repository.fetchInbox(queryMap)
    }

    fun fetchThread(
        threadId: String,
        cursor: String?,
    ): Call<DirectThreadFeedResponse?> {
        val queryMap = mutableMapOf(
            "visual_message_return_type" to "unseen",
            "limit" to 20.toString(),
            "direction" to "older",
        )
        if (!cursor.isNullOrBlank()) {
            queryMap["cursor"] = cursor
        }
        return repository.fetchThread(threadId, queryMap)
    }

    fun fetchUnseenCount(): Call<DirectBadgeCount?> {
        return repository.fetchUnseenCount()
    }

    fun broadcastText(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        text: String,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): Call<DirectThreadBroadcastResponse?> {
        val urls = extractUrls(text)
        if (urls.isNotEmpty()) {
            return broadcastLink(clientContext, threadIdOrUserIds, text, urls, repliedToItemId, repliedToClientContext)
        }
        val broadcastOptions = TextBroadcastOptions(clientContext, threadIdOrUserIds, text)
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            broadcastOptions.repliedToItemId = repliedToItemId
            broadcastOptions.repliedToClientContext = repliedToClientContext
        }
        return broadcast(broadcastOptions)
    }

    private fun broadcastLink(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        linkText: String,
        urls: List<String>,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): Call<DirectThreadBroadcastResponse?> {
        val broadcastOptions = LinkBroadcastOptions(clientContext, threadIdOrUserIds, linkText, urls)
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            broadcastOptions.repliedToItemId = repliedToItemId
            broadcastOptions.repliedToClientContext = repliedToClientContext
        }
        return broadcast(broadcastOptions)
    }

    fun broadcastPhoto(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(PhotoBroadcastOptions(clientContext, threadIdOrUserIds, true, uploadId))
    }

    fun broadcastVideo(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
        videoResult: String,
        sampled: Boolean,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(VideoBroadcastOptions(clientContext, threadIdOrUserIds, videoResult, uploadId, sampled))
    }

    fun broadcastVoice(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
        waveform: List<Float>,
        samplingFreq: Int,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(VoiceBroadcastOptions(clientContext, threadIdOrUserIds, uploadId, waveform, samplingFreq))
    }

    fun broadcastStoryReply(
        threadIdOrUserIds: ThreadIdOrUserIds,
        text: String,
        mediaId: String,
        reelId: String,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(StoryReplyBroadcastOptions(UUID.randomUUID().toString(), threadIdOrUserIds, text, mediaId, reelId))
    }

    fun broadcastReaction(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        itemId: String,
        emoji: String?,
        delete: Boolean,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(ReactionBroadcastOptions(clientContext, threadIdOrUserIds, itemId, emoji, delete))
    }

    fun broadcastAnimatedMedia(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        giphyGif: GiphyGif,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(AnimatedMediaBroadcastOptions(clientContext, threadIdOrUserIds, giphyGif))
    }

    fun broadcastMediaShare(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        mediaId: String,
    ): Call<DirectThreadBroadcastResponse?> {
        return broadcast(MediaShareBroadcastOptions(clientContext, threadIdOrUserIds, mediaId))
    }

    private fun broadcast(broadcastOptions: BroadcastOptions): Call<DirectThreadBroadcastResponse?> {
        require(!isEmpty(broadcastOptions.clientContext)) { "Broadcast requires a valid client context value" }
        val form = mutableMapOf<String, Any>()
        val threadId = broadcastOptions.threadId
        if (!threadId.isNullOrBlank()) {
            form["thread_id"] = threadId
        } else {
            val userIds = broadcastOptions.userIds
            require(!userIds.isNullOrEmpty()) {
                "Either provide a thread id or pass a list of user ids"
            }
            form["recipient_users"] = JSONArray(userIds).toString()
        }
        form["_csrftoken"] = csrfToken
        form["_uid"] = userId
        form["__uuid"] = deviceUuid
        form["client_context"] = broadcastOptions.clientContext
        form["mutation_token"] = broadcastOptions.clientContext
        val repliedToItemId = broadcastOptions.repliedToItemId
        val repliedToClientContext = broadcastOptions.repliedToClientContext
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            form["replied_to_item_id"] = repliedToItemId
            form["replied_to_client_context"] = repliedToClientContext
        }
        form.putAll(broadcastOptions.formMap)
        form["action"] = "send_item"
        val signedForm = Utils.sign(form)
        return repository.broadcast(broadcastOptions.itemType.value, signedForm)
    }

    fun addUsers(
        threadId: String,
        userIds: Collection<Long>,
    ): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.addUsers(threadId, form)
    }

    fun removeUsers(
        threadId: String,
        userIds: Collection<Long>,
    ): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.removeUsers(threadId, form)
    }

    fun updateTitle(
        threadId: String,
        title: String,
    ): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "title" to title,
        )
        return repository.updateTitle(threadId, form)
    }

    fun addAdmins(
        threadId: String,
        userIds: Collection<Long>,
    ): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.addAdmins(threadId, form)
    }

    fun removeAdmins(
        threadId: String,
        userIds: Collection<Long>,
    ): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.removeAdmins(threadId, form)
    }

    fun deleteItem(
        threadId: String,
        itemId: String,
    ): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.deleteItem(threadId, itemId, form)
    }

    fun rankedRecipients(
        mode: String?,
        showThreads: Boolean?,
        query: String?,
    ): Call<RankedRecipientsResponse?> {
        // String correctedMode = mode;
        // if (TextUtils.isEmpty(mode) || (!mode.equals("raven") && !mode.equals("reshare"))) {
        //     correctedMode = "raven";
        // }
        val queryMap = mutableMapOf<String, String>()
        if (!mode.isNullOrBlank()) {
            queryMap["mode"] = mode
        }
        if (!query.isNullOrBlank()) {
            queryMap["query"] = query
        }
        if (showThreads != null) {
            queryMap["showThreads"] = showThreads.toString()
        }
        return repository.rankedRecipients(queryMap)
    }

    fun forward(
        toThreadId: String,
        itemType: String,
        fromThreadId: String,
        itemId: String,
    ): Call<DirectThreadBroadcastResponse?> {
        val form = mapOf(
            "action" to "forward_item",
            "thread_id" to toThreadId,
            "item_type" to itemType,
            "forwarded_from_thread_id" to fromThreadId,
            "forwarded_from_thread_item_id" to itemId,
        )
        return repository.forward(form)
    }

    fun createThread(
        userIds: List<Long>,
        threadTitle: String?,
    ): Call<DirectThread?> {
        val userIdStringList = userIds.map { it.toString() }
        val form = mutableMapOf<String, Any>(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "_uid" to userId,
            "recipient_users" to JSONArray(userIdStringList).toString(),
        )
        if (!threadTitle.isNullOrBlank()) {
            form["thread_title"] = threadTitle
        }
        val signedForm = Utils.sign(form)
        return repository.createThread(signedForm)
    }

    fun mute(threadId: String): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid
        )
        return repository.mute(threadId, form)
    }

    fun unmute(threadId: String): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.unmute(threadId, form)
    }

    fun muteMentions(threadId: String): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.muteMentions(threadId, form)
    }

    fun unmuteMentions(threadId: String): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.unmuteMentions(threadId, form)
    }

    fun participantRequests(
        threadId: String,
        pageSize: Int,
        cursor: String?,
    ): Call<DirectThreadParticipantRequestsResponse?> {
        return repository.participantRequests(threadId, pageSize, cursor)
    }

    fun approveParticipantRequests(
        threadId: String,
        userIds: List<Long>,
    ): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
            // "share_join_chat_story" to String.valueOf(true)
        )
        return repository.approveParticipantRequests(threadId, form)
    }

    fun declineParticipantRequests(
        threadId: String,
        userIds: List<Long>,
    ): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.declineParticipantRequests(threadId, form)
    }

    fun approvalRequired(threadId: String): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approvalRequired(threadId, form)
    }

    fun approvalNotRequired(threadId: String): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approvalNotRequired(threadId, form)
    }

    fun leave(threadId: String): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.leave(threadId, form)
    }

    fun end(threadId: String): Call<DirectThreadDetailsChangeResponse?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.end(threadId, form)
    }

    fun fetchPendingInbox(cursor: String?, seqId: Long): Call<DirectInboxResponse?> {
        val queryMap = mutableMapOf(
            "visual_message_return_type" to "unseen",
            "thread_message_limit" to 20.toString(),
            "persistentBadging" to true.toString(),
            "limit" to 10.toString(),
        )
        if (!cursor.isNullOrBlank()) {
            queryMap["cursor"] = cursor
            queryMap["direction"] = "older"
        }
        if (seqId != 0L) {
            queryMap["seq_id"] = seqId.toString()
        }
        return repository.fetchPendingInbox(queryMap)
    }

    fun approveRequest(threadId: String): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approveRequest(threadId, form)
    }

    fun declineRequest(threadId: String): Call<String?> {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.declineRequest(threadId, form)
    }

    fun markAsSeen(
        threadId: String,
        directItem: DirectItem,
    ): Call<DirectItemSeenResponse?>? {
        val itemId = directItem.itemId ?: return null
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "use_unified_inbox" to "true",
            "action" to "mark_seen",
            "thread_id" to threadId,
            "item_id" to itemId,
        )
        return repository.markItemSeen(threadId, itemId, form)
    }

    companion object {
        private lateinit var instance: DirectMessagesService

        @JvmStatic
        fun getInstance(
            csrfToken: String,
            userId: Long,
            deviceUuid: String,
        ): DirectMessagesService {
            if (!this::instance.isInitialized
                || instance.csrfToken != csrfToken
                || instance.userId != userId
                || instance.deviceUuid != deviceUuid
            ) {
                instance = DirectMessagesService(csrfToken, userId, deviceUuid)
            }
            return instance
        }
    }

}