package awais.instagrabber.webservices

import awais.instagrabber.repositories.DirectMessagesRepository
import awais.instagrabber.repositories.requests.directmessages.*
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.TextUtils.extractUrls
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import org.json.JSONArray
import java.util.*

class DirectMessagesService private constructor(
    val csrfToken: String,
    val userId: Long,
    val deviceUuid: String,
) : BaseService() {
    private val repository: DirectMessagesRepository = RetrofitFactory.retrofit.create(DirectMessagesRepository::class.java)

    suspend fun fetchInbox(
        cursor: String?,
        seqId: Long,
    ): DirectInboxResponse {
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

    suspend fun fetchThread(
        threadId: String,
        cursor: String?,
    ): DirectThreadFeedResponse {
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

    suspend fun fetchUnseenCount(): DirectBadgeCount = repository.fetchUnseenCount()

    suspend fun broadcastText(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        text: String,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): DirectThreadBroadcastResponse {
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

    private suspend fun broadcastLink(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        linkText: String,
        urls: List<String>,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): DirectThreadBroadcastResponse {
        val broadcastOptions = LinkBroadcastOptions(clientContext, threadIdOrUserIds, linkText, urls)
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            broadcastOptions.repliedToItemId = repliedToItemId
            broadcastOptions.repliedToClientContext = repliedToClientContext
        }
        return broadcast(broadcastOptions)
    }

    suspend fun broadcastPhoto(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
    ): DirectThreadBroadcastResponse {
        return broadcast(PhotoBroadcastOptions(clientContext, threadIdOrUserIds, true, uploadId))
    }

    suspend fun broadcastVideo(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
        videoResult: String,
        sampled: Boolean,
    ): DirectThreadBroadcastResponse {
        return broadcast(VideoBroadcastOptions(clientContext, threadIdOrUserIds, videoResult, uploadId, sampled))
    }

    suspend fun broadcastVoice(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
        waveform: List<Float>,
        samplingFreq: Int,
    ): DirectThreadBroadcastResponse {
        return broadcast(VoiceBroadcastOptions(clientContext, threadIdOrUserIds, uploadId, waveform, samplingFreq))
    }

    suspend fun broadcastStoryReply(
        threadIdOrUserIds: ThreadIdOrUserIds,
        text: String,
        mediaId: String,
        reelId: String,
    ): DirectThreadBroadcastResponse {
        return broadcast(StoryReplyBroadcastOptions(UUID.randomUUID().toString(), threadIdOrUserIds, text, mediaId, reelId))
    }

    suspend fun broadcastReaction(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        itemId: String,
        emoji: String?,
        delete: Boolean,
    ): DirectThreadBroadcastResponse {
        return broadcast(ReactionBroadcastOptions(clientContext, threadIdOrUserIds, itemId, emoji, delete))
    }

    suspend fun broadcastAnimatedMedia(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        giphyGif: GiphyGif,
    ): DirectThreadBroadcastResponse {
        return broadcast(AnimatedMediaBroadcastOptions(clientContext, threadIdOrUserIds, giphyGif))
    }

    suspend fun broadcastMediaShare(
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        mediaId: String,
    ): DirectThreadBroadcastResponse {
        return broadcast(MediaShareBroadcastOptions(clientContext, threadIdOrUserIds, mediaId))
    }

    private suspend fun broadcast(broadcastOptions: BroadcastOptions): DirectThreadBroadcastResponse {
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

    suspend fun addUsers(
        threadId: String,
        userIds: Collection<Long>,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.addUsers(threadId, form)
    }

    suspend fun removeUsers(
        threadId: String,
        userIds: Collection<Long>,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.removeUsers(threadId, form)
    }

    suspend fun updateTitle(
        threadId: String,
        title: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "title" to title,
        )
        return repository.updateTitle(threadId, form)
    }

    suspend fun addAdmins(
        threadId: String,
        userIds: Collection<Long>,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.addAdmins(threadId, form)
    }

    suspend fun removeAdmins(
        threadId: String,
        userIds: Collection<Long>,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.removeAdmins(threadId, form)
    }

    suspend fun deleteItem(
        threadId: String,
        itemId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.deleteItem(threadId, itemId, form)
    }

    suspend fun rankedRecipients(
        mode: String?,
        showThreads: Boolean?,
        query: String?,
    ): RankedRecipientsResponse {
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

    suspend fun forward(
        toThreadId: String,
        itemType: String,
        fromThreadId: String,
        itemId: String,
    ): DirectThreadBroadcastResponse {
        val form = mapOf(
            "action" to "forward_item",
            "thread_id" to toThreadId,
            "item_type" to itemType,
            "forwarded_from_thread_id" to fromThreadId,
            "forwarded_from_thread_item_id" to itemId,
        )
        return repository.forward(form)
    }

    suspend fun createThread(
        userIds: List<Long>,
        threadTitle: String?,
    ): DirectThread {
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

    suspend fun mute(threadId: String): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid
        )
        return repository.mute(threadId, form)
    }

    suspend fun unmute(threadId: String): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.unmute(threadId, form)
    }

    suspend fun muteMentions(threadId: String): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.muteMentions(threadId, form)
    }

    suspend fun unmuteMentions(threadId: String): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.unmuteMentions(threadId, form)
    }

    suspend fun participantRequests(
        threadId: String,
        pageSize: Int,
        cursor: String? = null,
    ): DirectThreadParticipantRequestsResponse {
        return repository.participantRequests(threadId, pageSize, cursor)
    }

    suspend fun approveParticipantRequests(
        threadId: String,
        userIds: List<Long>,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
            // "share_join_chat_story" to String.valueOf(true)
        )
        return repository.approveParticipantRequests(threadId, form)
    }

    suspend fun declineParticipantRequests(
        threadId: String,
        userIds: List<Long>,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return repository.declineParticipantRequests(threadId, form)
    }

    suspend fun approvalRequired(threadId: String): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approvalRequired(threadId, form)
    }

    suspend fun approvalNotRequired(threadId: String): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approvalNotRequired(threadId, form)
    }

    suspend fun leave(threadId: String): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.leave(threadId, form)
    }

    suspend fun end(threadId: String): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.end(threadId, form)
    }

    suspend fun fetchPendingInbox(cursor: String?, seqId: Long): DirectInboxResponse {
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

    suspend fun approveRequest(threadId: String): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approveRequest(threadId, form)
    }

    suspend fun declineRequest(threadId: String): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.declineRequest(threadId, form)
    }

    suspend fun markAsSeen(
        threadId: String,
        directItem: DirectItem,
    ): DirectItemSeenResponse? {
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