package awais.instagrabber.webservices

import awais.instagrabber.repositories.DirectMessagesRepository
import awais.instagrabber.repositories.requests.directmessages.*
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.TextUtils.extractUrls
import awais.instagrabber.utils.Utils
import org.json.JSONArray
import java.util.*

object DirectMessagesService : BaseService() {
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
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        text: String,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): DirectThreadBroadcastResponse {
        val urls = extractUrls(text)
        if (urls.isNotEmpty()) {
            return broadcastLink(csrfToken, userId, deviceUuid, clientContext, threadIdOrUserIds, text, urls, repliedToItemId, repliedToClientContext)
        }
        val broadcastOptions = TextBroadcastOptions(clientContext, threadIdOrUserIds, text)
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            broadcastOptions.repliedToItemId = repliedToItemId
            broadcastOptions.repliedToClientContext = repliedToClientContext
        }
        return broadcast(csrfToken, userId, deviceUuid, broadcastOptions)
    }

    private suspend fun broadcastLink(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
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
        return broadcast(csrfToken, userId, deviceUuid, broadcastOptions)
    }

    suspend fun broadcastPhoto(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, PhotoBroadcastOptions(clientContext, threadIdOrUserIds, true, uploadId))

    suspend fun broadcastVideo(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
        videoResult: String,
        sampled: Boolean,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, VideoBroadcastOptions(clientContext, threadIdOrUserIds, videoResult, uploadId, sampled))

    suspend fun broadcastVoice(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        uploadId: String,
        waveform: List<Float>,
        samplingFreq: Int,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, VoiceBroadcastOptions(clientContext, threadIdOrUserIds, uploadId, waveform, samplingFreq))

    suspend fun broadcastStoryReply(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        text: String,
        mediaId: String,
        reelId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, StoryReplyBroadcastOptions(UUID.randomUUID().toString(), threadIdOrUserIds, text, mediaId, reelId))

    suspend fun broadcastReaction(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        itemId: String,
        emoji: String?,
        delete: Boolean,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, ReactionBroadcastOptions(clientContext, threadIdOrUserIds, itemId, emoji, delete))

    suspend fun broadcastAnimatedMedia(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        giphyGif: GiphyGif,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, AnimatedMediaBroadcastOptions(clientContext, threadIdOrUserIds, giphyGif))

    suspend fun broadcastMediaShare(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdOrUserIds: ThreadIdOrUserIds,
        mediaId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, MediaShareBroadcastOptions(clientContext, threadIdOrUserIds, mediaId))

    private suspend fun broadcast(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        broadcastOptions: BroadcastOptions,
    ): DirectThreadBroadcastResponse {
        require(broadcastOptions.clientContext.isNotBlank()) { "Broadcast requires a valid client context value" }
        val form = mutableMapOf<String, Any>(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "__uuid" to deviceUuid,
            "client_context" to broadcastOptions.clientContext,
            "mutation_token" to broadcastOptions.clientContext,
        )
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
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

    suspend fun mute(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid
        )
        return repository.mute(threadId, form)
    }

    suspend fun unmute(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.unmute(threadId, form)
    }

    suspend fun muteMentions(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.muteMentions(threadId, form)
    }

    suspend fun unmuteMentions(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
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
        csrfToken: String,
        deviceUuid: String,
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
        csrfToken: String,
        deviceUuid: String,
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

    suspend fun approvalRequired(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approvalRequired(threadId, form)
    }

    suspend fun approvalNotRequired(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approvalNotRequired(threadId, form)
    }

    suspend fun leave(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.leave(threadId, form)
    }

    suspend fun end(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
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

    suspend fun approveRequest(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.approveRequest(threadId, form)
    }

    suspend fun declineRequest(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return repository.declineRequest(threadId, form)
    }

    suspend fun markAsSeen(
        csrfToken: String,
        deviceUuid: String,
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
}