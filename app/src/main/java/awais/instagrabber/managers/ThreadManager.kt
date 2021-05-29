package awais.instagrabber.managers

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.Transformations.map
import awais.instagrabber.R
import awais.instagrabber.customviews.emoji.Emoji
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.repositories.requests.UploadFinishOptions
import awais.instagrabber.repositories.requests.VideoOptions
import awais.instagrabber.repositories.requests.directmessages.ThreadIdOrUserIds
import awais.instagrabber.repositories.requests.directmessages.ThreadIdOrUserIds.Companion.of
import awais.instagrabber.repositories.responses.FriendshipChangeResponse
import awais.instagrabber.repositories.responses.FriendshipRestrictResponse
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.*
import awais.instagrabber.utils.MediaUploader.MediaUploadResponse
import awais.instagrabber.utils.MediaUploader.OnMediaUploadCompleteListener
import awais.instagrabber.utils.MediaUploader.uploadPhoto
import awais.instagrabber.utils.MediaUploader.uploadVideo
import awais.instagrabber.utils.MediaUtils.OnInfoLoadListener
import awais.instagrabber.utils.MediaUtils.VideoInfo
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.webservices.DirectMessagesService
import awais.instagrabber.webservices.FriendshipService
import awais.instagrabber.webservices.MediaService
import awais.instagrabber.webservices.ServiceCallback
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class ThreadManager private constructor(
    private val threadId: String,
    pending: Boolean,
    currentUser: User,
    contentResolver: ContentResolver,
    viewerId: Long,
    csrfToken: String,
    deviceUuid: String,
) {
    private val fetching = MutableLiveData<Resource<Any?>>()
    private val replyToItem = MutableLiveData<DirectItem?>()
    private val pendingRequests = MutableLiveData<DirectThreadParticipantRequestsResponse?>(null)
    private val inboxManager: InboxManager = if (pending) DirectMessagesManager.pendingInboxManager else DirectMessagesManager.inboxManager
    private val viewerId: Long
    private val threadIdOrUserIds: ThreadIdOrUserIds = of(threadId)
    private val currentUser: User?
    private val contentResolver: ContentResolver
    private val service: DirectMessagesService
    private val mediaService: MediaService
    private val friendshipService: FriendshipService

    val thread: LiveData<DirectThread?> by lazy {
        distinctUntilChanged(map(inboxManager.getInbox()) { inboxResource: Resource<DirectInbox?>? ->
            if (inboxResource == null) return@map null
            val (threads) = inboxResource.data ?: return@map null
            if (threads.isNullOrEmpty()) return@map null
            val thread = threads.firstOrNull { it.threadId == threadId }
            thread?.also {
                cursor = thread.oldestCursor
                hasOlder = thread.hasOlder
            }
        })
    }
    val inputMode: LiveData<Int> by lazy { distinctUntilChanged(map(thread) { it?.inputMode ?: 1 }) }
    val threadTitle: LiveData<String?> by lazy { distinctUntilChanged(map(thread) { it?.threadTitle }) }
    val users: LiveData<List<User>> by lazy { distinctUntilChanged(map(thread) { it?.users ?: emptyList() }) }
    val usersWithCurrent: LiveData<List<User>> by lazy {
        distinctUntilChanged(map(thread) {
            if (it == null) return@map emptyList()
            getUsersWithCurrentUser(it)
        })
    }
    val leftUsers: LiveData<List<User>> by lazy { distinctUntilChanged(map(thread) { it?.leftUsers ?: emptyList() }) }
    val usersAndLeftUsers: LiveData<Pair<List<User>, List<User>>> by lazy {
        distinctUntilChanged(map(thread) {
            if (it == null) return@map Pair<List<User>, List<User>>(emptyList(), emptyList())
            val users = getUsersWithCurrentUser(it)
            val leftUsers = it.leftUsers
            Pair(users, leftUsers)
        })
    }
    val isPending: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.pending ?: true }) }
    val adminUserIds: LiveData<List<Long>> by lazy { distinctUntilChanged(map(thread) { it?.adminUserIds ?: emptyList() }) }
    val items: LiveData<List<DirectItem>> by lazy { distinctUntilChanged(map(thread) { it?.items ?: emptyList() }) }
    val isViewerAdmin: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.adminUserIds?.contains(viewerId) ?: false }) }
    val isGroup: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.isGroup ?: false }) }
    val isMuted: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.muted ?: false }) }
    val isApprovalRequiredToJoin: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.approvalRequiredForNewMembers ?: false }) }
    val isMentionsMuted: LiveData<Boolean> by lazy { distinctUntilChanged(map(thread) { it?.mentionsMuted ?: false }) }
    val pendingRequestsCount: LiveData<Int> by lazy { distinctUntilChanged(map(pendingRequests) { it?.totalParticipantRequests ?: 0 }) }
    val inviter: LiveData<User?> by lazy { distinctUntilChanged(map(thread) { it?.inviter }) }

    private var hasOlder = true
    private var cursor: String? = null
    private var chatsRequest: Call<DirectThreadFeedResponse?>? = null

    private fun getUsersWithCurrentUser(t: DirectThread): List<User> {
        val builder = ImmutableList.builder<User>()
        if (currentUser != null) {
            builder.add(currentUser)
        }
        val users: List<User>? = t.users
        if (users != null) {
            builder.addAll(users)
        }
        return builder.build()
    }

    fun isFetching(): LiveData<Resource<Any?>> {
        return fetching
    }

    fun getReplyToItem(): LiveData<DirectItem?> {
        return replyToItem
    }

    fun getPendingRequests(): LiveData<DirectThreadParticipantRequestsResponse?> {
        return pendingRequests
    }

    fun fetchChats() {
        val fetchingValue = fetching.value
        if (fetchingValue != null && fetchingValue.status === Resource.Status.LOADING || !hasOlder) return
        fetching.postValue(loading(null))
        chatsRequest = service.fetchThread(threadId, cursor)
        chatsRequest?.enqueue(object : Callback<DirectThreadFeedResponse?> {
            override fun onResponse(call: Call<DirectThreadFeedResponse?>, response: Response<DirectThreadFeedResponse?>) {
                val feedResponse = response.body()
                if (feedResponse == null) {
                    fetching.postValue(error(R.string.generic_null_response, null))
                    Log.e(TAG, "onResponse: response was null!")
                    return
                }
                if (feedResponse.status != null && feedResponse.status != "ok") {
                    fetching.postValue(error(R.string.generic_not_ok_response, null))
                    return
                }
                val thread = feedResponse.thread
                if (thread == null) {
                    fetching.postValue(error("thread is null!", null))
                    return
                }
                setThread(thread)
                fetching.postValue(success(Any()))
            }

            override fun onFailure(call: Call<DirectThreadFeedResponse?>, t: Throwable) {
                Log.e(TAG, "Failed fetching dm chats", t)
                fetching.postValue(error(t.message, null))
                hasOlder = false
            }
        })
        if (cursor == null) {
            fetchPendingRequests()
        }
    }

    fun fetchPendingRequests() {
        val isGroup = isGroup.value
        if (isGroup == null || !isGroup) return
        val request = service.participantRequests(threadId, 1, null)
        request.enqueue(object : Callback<DirectThreadParticipantRequestsResponse?> {
            override fun onResponse(
                call: Call<DirectThreadParticipantRequestsResponse?>,
                response: Response<DirectThreadParticipantRequestsResponse?>,
            ) {
                if (!response.isSuccessful) {
                    if (response.errorBody() != null) {
                        try {
                            val string = response.errorBody()?.string() ?: ""
                            val msg = String.format(Locale.US,
                                "onResponse: url: %s, responseCode: %d, errorBody: %s",
                                call.request().url().toString(),
                                response.code(),
                                string)
                            Log.e(TAG, msg)
                        } catch (e: IOException) {
                            Log.e(TAG, "onResponse: ", e)
                        }
                        return
                    }
                    Log.e(TAG, "onResponse: request was not successful and response error body was null")
                    return
                }
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "onResponse: response body was null")
                    return
                }
                pendingRequests.postValue(body)
            }

            override fun onFailure(call: Call<DirectThreadParticipantRequestsResponse?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
            }
        })
    }

    private fun setThread(thread: DirectThread, skipItems: Boolean) {
        // if (thread.getInputMode() != 1 && thread.isGroup() && viewerIsAdmin) {
        //     fetchPendingRequests();
        // }
        val items = thread.items
        if (skipItems) {
            val currentThread = this.thread.value
            if (currentThread != null) {
                thread.items = currentThread.items
            }
        }
        if (!skipItems && !cursor.isNullOrBlank()) {
            val currentThread = this.thread.value
            if (currentThread != null) {
                val currentItems = currentThread.items
                val list = if (currentItems == null) LinkedList() else LinkedList(currentItems)
                if (items != null) {
                    list.addAll(items)
                }
                thread.items = list
            }
        }
        inboxManager.setThread(threadId, thread)
    }

    private fun setThread(thread: DirectThread) {
        setThread(thread, false)
    }

    private fun setThreadUsers(users: List<User>?, leftUsers: List<User>?) {
        val currentThread = thread.value ?: return
        val thread: DirectThread = try {
            currentThread.clone() as DirectThread
        } catch (e: CloneNotSupportedException) {
            Log.e(TAG, "setThreadUsers: ", e)
            return
        }
        if (users != null) {
            thread.users = users
        }
        if (leftUsers != null) {
            thread.leftUsers = leftUsers
        }
        inboxManager.setThread(threadId, thread)
    }

    private fun addItems(index: Int, items: Collection<DirectItem>) {
        inboxManager.addItemsToThread(threadId, index, items)
    }

    private fun addReaction(item: DirectItem, emoji: Emoji) {
        if (currentUser == null) return
        val isLike = emoji.unicode == "❤️"
        var reactions = item.reactions
        reactions = if (reactions == null) {
            DirectItemReactions(null, null)
        } else {
            try {
                reactions.clone() as DirectItemReactions
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "addReaction: ", e)
                return
            }
        }
        if (isLike) {
            val likes = addEmoji(reactions.likes, null, false)
            reactions.likes = likes
        }
        val emojis = addEmoji(reactions.emojis, emoji.unicode, true)
        reactions.emojis = emojis
        val currentItems = items.value
        val items = if (currentItems == null) LinkedList() else LinkedList(currentItems)
        val index = getItemIndex(item, items)
        if (index >= 0) {
            try {
                val clone = items[index].clone() as DirectItem
                clone.reactions = reactions
                items[index] = clone
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "addReaction: error cloning", e)
            }
        }
        inboxManager.setItemsToThread(threadId, items)
    }

    private fun removeReaction(item: DirectItem) {
        try {
            val itemClone = item.clone() as DirectItem
            val reactions = itemClone.reactions
            var reactionsClone: DirectItemReactions? = null
            if (reactions != null) {
                reactionsClone = reactions.clone() as DirectItemReactions
            }
            var likes: List<DirectItemEmojiReaction>? = null
            if (reactionsClone != null) {
                likes = reactionsClone.likes
            }
            if (likes != null) {
                val updatedLikes = likes.stream()
                    .filter { (senderId) -> senderId != viewerId }
                    .collect(Collectors.toList())
                if (reactionsClone != null) {
                    reactionsClone.likes = updatedLikes
                }
            }
            var emojis: List<DirectItemEmojiReaction>? = null
            if (reactionsClone != null) {
                emojis = reactionsClone.emojis
            }
            if (emojis != null) {
                val updatedEmojis = emojis.stream()
                    .filter { (senderId) -> senderId != viewerId }
                    .collect(Collectors.toList())
                if (reactionsClone != null) {
                    reactionsClone.emojis = updatedEmojis
                }
            }
            itemClone.reactions = reactionsClone
            val items = items.value
            val list = if (items == null) LinkedList() else LinkedList(items)
            val index = getItemIndex(item, list)
            if (index >= 0) {
                list[index] = itemClone
            }
            inboxManager.setItemsToThread(threadId, list)
        } catch (e: Exception) {
            Log.e(TAG, "removeReaction: ", e)
        }
    }

    private fun removeItem(item: DirectItem): Int {
        val items = items.value
        val list = if (items == null) LinkedList() else LinkedList(items)
        val index = getItemIndex(item, list)
        if (index >= 0) {
            list.removeAt(index)
            inboxManager.setItemsToThread(threadId, list)
        }
        return index
    }

    private fun addEmoji(
        reactionList: List<DirectItemEmojiReaction>?,
        emoji: String?,
        shouldReplaceIfAlreadyReacted: Boolean,
    ): List<DirectItemEmojiReaction>? {
        if (currentUser == null) return reactionList
        val temp: MutableList<DirectItemEmojiReaction> = if (reactionList == null) ArrayList() else ArrayList(reactionList)
        var index = -1
        for (i in temp.indices) {
            val (senderId) = temp[i]
            if (senderId == currentUser.pk) {
                index = i
                break
            }
        }
        val reaction = DirectItemEmojiReaction(
            currentUser.pk,
            System.currentTimeMillis() * 1000,
            emoji,
            "none"
        )
        if (index < 0) {
            temp.add(0, reaction)
        } else if (shouldReplaceIfAlreadyReacted) {
            temp[index] = reaction
        }
        return temp
    }

    fun sendText(text: String): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val userId = getCurrentUserId(data) ?: return data
        val clientContext = UUID.randomUUID().toString()
        val replyToItemValue = replyToItem.value
        val directItem = createText(userId, clientContext, text, replyToItemValue)
        // Log.d(TAG, "sendText: sending: itemId: " + directItem.getItemId());
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val repliedToItemId = replyToItemValue?.itemId
        val repliedToClientContext = replyToItemValue?.clientContext
        val request = service.broadcastText(
            clientContext,
            threadIdOrUserIds,
            text,
            repliedToItemId,
            repliedToClientContext
        )
        enqueueRequest(request, data, directItem)
        return data
    }

    fun sendUri(entry: MediaController.MediaEntry): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val uri = Uri.fromFile(File(entry.path))
        if (!entry.isVideo) {
            sendPhoto(data, uri, entry.width, entry.height)
            return data
        }
        sendVideo(data, uri, entry.size, entry.duration, entry.width, entry.height)
        return data
    }

    fun sendUri(uri: Uri): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val mimeType = Utils.getMimeType(uri, contentResolver)
        if (isEmpty(mimeType)) {
            data.postValue(error("Unknown MediaType", null))
            return data
        }
        val isPhoto = mimeType != null && mimeType.startsWith("image")
        if (isPhoto) {
            sendPhoto(data, uri)
            return data
        }
        if (mimeType != null && mimeType.startsWith("video")) {
            sendVideo(data, uri)
        }
        return data
    }

    fun sendAnimatedMedia(giphyGif: GiphyGif): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val userId = getCurrentUserId(data) ?: return data
        val clientContext = UUID.randomUUID().toString()
        val directItem = createAnimatedMedia(userId, clientContext, giphyGif)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val request = service.broadcastAnimatedMedia(
            clientContext,
            threadIdOrUserIds,
            giphyGif
        )
        enqueueRequest(request, data, directItem)
        return data
    }

    fun sendVoice(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        waveform: List<Float>,
        samplingFreq: Int,
        duration: Long,
        byteLength: Long,
    ) {
        if (duration > 60000) {
            // instagram does not allow uploading audio longer than 60 secs for Direct messages
            data.postValue(error(R.string.dms_ERROR_AUDIO_TOO_LONG, null))
            return
        }
        val userId = getCurrentUserId(data) ?: return
        val clientContext = UUID.randomUUID().toString()
        val directItem = createVoice(userId, clientContext, uri, duration, waveform, samplingFreq)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val uploadDmVoiceOptions = createUploadDmVoiceOptions(byteLength, duration)
        uploadVideo(uri, contentResolver, uploadDmVoiceOptions, object : OnMediaUploadCompleteListener {
            override fun onUploadComplete(response: MediaUploadResponse) {
                // Log.d(TAG, "onUploadComplete: " + response);
                if (handleInvalidResponse(data, response)) return
                val uploadFinishOptions = UploadFinishOptions(
                    uploadDmVoiceOptions.uploadId,
                    "4",
                    null
                )
                val uploadFinishRequest = mediaService.uploadFinish(uploadFinishOptions)
                uploadFinishRequest.enqueue(object : Callback<String?> {
                    override fun onResponse(call: Call<String?>, response: Response<String?>) {
                        if (response.isSuccessful) {
                            val request = service.broadcastVoice(
                                clientContext,
                                threadIdOrUserIds,
                                uploadDmVoiceOptions.uploadId,
                                waveform,
                                samplingFreq
                            )
                            enqueueRequest(request, data, directItem)
                            return
                        }
                        if (response.errorBody() != null) {
                            handleErrorBody(call, response, data)
                            return
                        }
                        data.postValue(error("uploadFinishRequest was not successful and response error body was null", directItem))
                        Log.e(TAG, "uploadFinishRequest was not successful and response error body was null")
                    }

                    override fun onFailure(call: Call<String?>, t: Throwable) {
                        data.postValue(error(t.message, directItem))
                        Log.e(TAG, "onFailure: ", t)
                    }
                })
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, directItem))
                Log.e(TAG, "onFailure: ", t)
            }
        })
    }

    fun sendReaction(
        item: DirectItem,
        emoji: Emoji,
    ): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val userId = getCurrentUserId(data)
        if (userId == null) {
            data.postValue(error("userId is null", null))
            return data
        }
        val clientContext = UUID.randomUUID().toString()
        // Log.d(TAG, "sendText: sending: itemId: " + directItem.getItemId());
        data.postValue(loading(item))
        addReaction(item, emoji)
        var emojiUnicode: String? = null
        if (emoji.unicode != "❤️") {
            emojiUnicode = emoji.unicode
        }
        val itemId = item.itemId
        if (itemId == null) {
            data.postValue(error("itemId is null", null))
            return data
        }
        val request = service.broadcastReaction(
            clientContext,
            threadIdOrUserIds,
            itemId,
            emojiUnicode,
            false
        )
        handleBroadcastReactionRequest(data, item, request)
        return data
    }

    fun sendDeleteReaction(itemId: String): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val item = getItem(itemId)
        if (item == null) {
            data.postValue(error("Invalid item", null))
            return data
        }
        val reactions = item.reactions
        if (reactions == null) {
            // already removed?
            data.postValue(success(item))
            return data
        }
        removeReaction(item)
        val clientContext = UUID.randomUUID().toString()
        val itemId1 = item.itemId
        if (itemId1 == null) {
            data.postValue(error("itemId is null", null))
            return data
        }
        val request = service.broadcastReaction(clientContext, threadIdOrUserIds, itemId1, null, true)
        handleBroadcastReactionRequest(data, item, request)
        return data
    }

    fun unsend(item: DirectItem): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val index = removeItem(item)
        val itemId = item.itemId
        if (itemId == null) {
            data.postValue(error("itemId is null", null))
            return data
        }
        val request = service.deleteItem(threadId, itemId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (response.isSuccessful) {
                    // Log.d(TAG, "onResponse: " + response.body());
                    return
                }
                // add the item back if unsuccessful
                addItems(index, listOf(item))
                if (response.errorBody() != null) {
                    handleErrorBody(call, response, data)
                    return
                }
                data.postValue(error(R.string.generic_failed_request, item))
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                data.postValue(error(t.message, item))
                Log.e(TAG, "enqueueRequest: onFailure: ", t)
            }
        })
        return data
    }

    fun forward(recipients: Set<RankedRecipient>, itemToForward: DirectItem) {
        for (recipient in recipients) {
            forward(recipient, itemToForward)
        }
    }

    fun forward(recipient: RankedRecipient, itemToForward: DirectItem) {
        if (recipient.thread == null && recipient.user != null) {
            // create thread and forward
            DirectMessagesManager.createThread(recipient.user.pk) { forward(it, itemToForward) }
        }
        if (recipient.thread != null) {
            // just forward
            val thread = recipient.thread
            forward(thread, itemToForward)
        }
    }

    fun setReplyToItem(item: DirectItem?) {
        // Log.d(TAG, "setReplyToItem: " + item);
        replyToItem.postValue(item)
    }

    private fun forward(thread: DirectThread, itemToForward: DirectItem): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val forwardItemId = itemToForward.itemId
        if (forwardItemId == null) {
            data.postValue(error("item id is null", null))
            return data
        }
        val itemType = itemToForward.itemType
        if (itemType == null) {
            data.postValue(error("item type is null", null))
            return data
        }
        val itemTypeName = itemType.getName()
        if (itemTypeName == null) {
            Log.e(TAG, "forward: itemTypeName was null!")
            data.postValue(error("itemTypeName is null", null))
            return data
        }
        data.postValue(loading(null))
        if (thread.threadId == null) {
            Log.e(TAG, "forward: threadId was null!")
            data.postValue(error("threadId is null", null))
            return data
        }
        val request = service.forward(thread.threadId,
            itemTypeName,
            threadId,
            forwardItemId)
        request.enqueue(object : Callback<DirectThreadBroadcastResponse?> {
            override fun onResponse(
                call: Call<DirectThreadBroadcastResponse?>,
                response: Response<DirectThreadBroadcastResponse?>,
            ) {
                if (response.isSuccessful) {
                    data.postValue(success(Any()))
                    return
                }
                val errorBody = response.errorBody()
                if (errorBody != null) {
                    try {
                        val string = errorBody.string()
                        val msg = String.format(Locale.US,
                            "onResponse: url: %s, responseCode: %d, errorBody: %s",
                            call.request().url().toString(),
                            response.code(),
                            string)
                        Log.e(TAG, msg)
                        data.postValue(error(msg, null))
                    } catch (e: IOException) {
                        Log.e(TAG, "onResponse: ", e)
                        data.postValue(error(e.message, null))
                    }
                    return
                }
                val msg = "onResponse: request was not successful and response error body was null"
                Log.e(TAG, msg)
                data.postValue(error(msg, null))
            }

            override fun onFailure(call: Call<DirectThreadBroadcastResponse?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun acceptRequest(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val request = service.approveRequest(threadId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(
                call: Call<String?>,
                response: Response<String?>,
            ) {
                if (!response.isSuccessful) {
                    try {
                        val string = response.errorBody()?.string() ?: ""
                        val msg = String.format(Locale.US,
                            "onResponse: url: %s, responseCode: %d, errorBody: %s",
                            call.request().url().toString(),
                            response.code(),
                            string)
                        Log.e(TAG, msg)
                        data.postValue(error(msg, null))
                        return
                    } catch (e: IOException) {
                        Log.e(TAG, "onResponse: ", e)
                    }
                    return
                }
                data.postValue(success(Any()))
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun declineRequest(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val request = service.declineRequest(threadId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(
                call: Call<String?>,
                response: Response<String?>,
            ) {
                if (!response.isSuccessful) {
                    try {
                        val string = response.errorBody()?.string() ?: ""
                        val msg = String.format(Locale.US,
                            "onResponse: url: %s, responseCode: %d, errorBody: %s",
                            call.request().url().toString(),
                            response.code(),
                            string)
                        Log.e(TAG, msg)
                        data.postValue(error(msg, null))
                        return
                    } catch (e: IOException) {
                        Log.e(TAG, "onResponse: ", e)
                    }
                    return
                }
                data.postValue(success(Any()))
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun refreshChats() {
        val isFetching = fetching.value
        if (isFetching != null && isFetching.status === Resource.Status.LOADING) {
            stopCurrentRequest()
        }
        cursor = null
        hasOlder = true
        fetchChats()
    }

    private fun sendPhoto(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
    ) {
        try {
            val dimensions = BitmapUtils.decodeDimensions(contentResolver, uri)
            if (dimensions == null) {
                data.postValue(error("Decoding dimensions failed", null))
                return
            }
            sendPhoto(data, uri, dimensions.first, dimensions.second)
        } catch (e: IOException) {
            data.postValue(error(e.message, null))
            Log.e(TAG, "sendPhoto: ", e)
        }
    }

    private fun sendPhoto(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        width: Int,
        height: Int,
    ) {
        val userId = getCurrentUserId(data) ?: return
        val clientContext = UUID.randomUUID().toString()
        val directItem = createImageOrVideo(userId, clientContext, uri, width, height, false)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        uploadPhoto(uri, contentResolver, object : OnMediaUploadCompleteListener {
            override fun onUploadComplete(response: MediaUploadResponse) {
                if (handleInvalidResponse(data, response)) return
                val response1 = response.response ?: return
                val uploadId = response1.optString("upload_id")
                val request = service.broadcastPhoto(clientContext, threadIdOrUserIds, uploadId)
                enqueueRequest(request, data, directItem)
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, directItem))
                Log.e(TAG, "onFailure: ", t)
            }
        })
    }

    private fun sendVideo(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
    ) {
        MediaUtils.getVideoInfo(contentResolver, uri, object : OnInfoLoadListener<VideoInfo?> {
            override fun onLoad(info: VideoInfo?) {
                if (info == null) {
                    data.postValue(error("Could not get the video info", null))
                    return
                }
                sendVideo(data, uri, info.size, info.duration, info.width, info.height)
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, null))
            }
        })
    }

    private fun sendVideo(
        data: MutableLiveData<Resource<Any?>>,
        uri: Uri,
        byteLength: Long,
        duration: Long,
        width: Int,
        height: Int,
    ) {
        if (duration > 60000) {
            // instagram does not allow uploading videos longer than 60 secs for Direct messages
            data.postValue(error(R.string.dms_ERROR_VIDEO_TOO_LONG, null))
            return
        }
        val userId = getCurrentUserId(data) ?: return
        val clientContext = UUID.randomUUID().toString()
        val directItem = createImageOrVideo(userId, clientContext, uri, width, height, true)
        directItem.isPending = true
        addItems(0, listOf(directItem))
        data.postValue(loading(directItem))
        val uploadDmVideoOptions = createUploadDmVideoOptions(byteLength, duration, width, height)
        uploadVideo(uri, contentResolver, uploadDmVideoOptions, object : OnMediaUploadCompleteListener {
            override fun onUploadComplete(response: MediaUploadResponse) {
                // Log.d(TAG, "onUploadComplete: " + response);
                if (handleInvalidResponse(data, response)) return
                val uploadFinishOptions = UploadFinishOptions(
                    uploadDmVideoOptions.uploadId,
                    "2",
                    VideoOptions(duration / 1000f, emptyList(), 0, false)
                )
                val uploadFinishRequest = mediaService.uploadFinish(uploadFinishOptions)
                uploadFinishRequest.enqueue(object : Callback<String?> {
                    override fun onResponse(call: Call<String?>, response: Response<String?>) {
                        if (response.isSuccessful) {
                            val request = service.broadcastVideo(
                                clientContext,
                                threadIdOrUserIds,
                                uploadDmVideoOptions.uploadId,
                                "",
                                true
                            )
                            enqueueRequest(request, data, directItem)
                            return
                        }
                        if (response.errorBody() != null) {
                            handleErrorBody(call, response, data)
                            return
                        }
                        data.postValue(error("uploadFinishRequest was not successful and response error body was null", directItem))
                        Log.e(TAG, "uploadFinishRequest was not successful and response error body was null")
                    }

                    override fun onFailure(call: Call<String?>, t: Throwable) {
                        data.postValue(error(t.message, directItem))
                        Log.e(TAG, "onFailure: ", t)
                    }
                })
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, directItem))
                Log.e(TAG, "onFailure: ", t)
            }
        })
    }

    private fun enqueueRequest(
        request: Call<DirectThreadBroadcastResponse?>,
        data: MutableLiveData<Resource<Any?>>,
        directItem: DirectItem,
    ) {
        request.enqueue(object : Callback<DirectThreadBroadcastResponse?> {
            override fun onResponse(
                call: Call<DirectThreadBroadcastResponse?>,
                response: Response<DirectThreadBroadcastResponse?>,
            ) {
                if (response.isSuccessful) {
                    val broadcastResponse = response.body()
                    if (broadcastResponse == null) {
                        data.postValue(error(R.string.generic_null_response, directItem))
                        Log.e(TAG, "enqueueRequest: onResponse: response body is null")
                        return
                    }
                    val payloadClientContext: String?
                    val timestamp: Long
                    val itemId: String?
                    val payload = broadcastResponse.payload
                    if (payload == null) {
                        val messageMetadata = broadcastResponse.messageMetadata
                        if (messageMetadata == null || messageMetadata.isEmpty()) {
                            data.postValue(success(directItem))
                            return
                        }
                        val (clientContext, itemId1, timestamp1) = messageMetadata[0]
                        payloadClientContext = clientContext
                        itemId = itemId1
                        timestamp = timestamp1
                    } else {
                        payloadClientContext = payload.clientContext
                        timestamp = payload.timestamp
                        itemId = payload.itemId
                    }
                    if (payloadClientContext == null) {
                        data.postValue(error("clientContext in response was null", null))
                        return
                    }
                    updateItemSent(payloadClientContext, timestamp, itemId)
                    data.postValue(success(directItem))
                    return
                }
                if (response.errorBody() != null) {
                    handleErrorBody(call, response, data)
                }
                data.postValue(error(R.string.generic_failed_request, directItem))
            }

            override fun onFailure(
                call: Call<DirectThreadBroadcastResponse?>,
                t: Throwable,
            ) {
                data.postValue(error(t.message, directItem))
                Log.e(TAG, "enqueueRequest: onFailure: ", t)
            }
        })
    }

    private fun updateItemSent(
        clientContext: String,
        timestamp: Long,
        itemId: String?,
    ) {
        val items = items.value
        val list = if (items == null) LinkedList() else LinkedList(items)
        val index = list.indexOfFirst { it?.clientContext == clientContext }
        if (index < 0) return
        val directItem = list[index]
        try {
            val itemClone = directItem.clone() as DirectItem
            itemClone.itemId = itemId
            itemClone.isPending = false
            itemClone.setTimestamp(timestamp)
            list[index] = itemClone
            inboxManager.setItemsToThread(threadId, list)
        } catch (e: CloneNotSupportedException) {
            Log.e(TAG, "updateItemSent: ", e)
        }
    }

    private fun handleErrorBody(
        call: Call<*>,
        response: Response<*>,
        data: MutableLiveData<Resource<Any?>>?,
    ) {
        try {
            val string = response.errorBody()?.string() ?: ""
            val msg = String.format(Locale.US,
                "onResponse: url: %s, responseCode: %d, errorBody: %s",
                call.request().url().toString(),
                response.code(),
                string)
            data?.postValue(error(msg, null))
            Log.e(TAG, msg)
        } catch (e: IOException) {
            data?.postValue(error(e.message, null))
            Log.e(TAG, "onResponse: ", e)
        }
    }

    private fun handleInvalidResponse(
        data: MutableLiveData<Resource<Any?>>,
        response: MediaUploadResponse,
    ): Boolean {
        val responseJson = response.response
        if (responseJson == null || response.responseCode != HttpURLConnection.HTTP_OK) {
            data.postValue(error(R.string.generic_not_ok_response, null))
            return true
        }
        val status = responseJson.optString("status")
        if (isEmpty(status) || status != "ok") {
            data.postValue(error(R.string.generic_not_ok_response, null))
            return true
        }
        return false
    }

    private fun getItemIndex(item: DirectItem, list: List<DirectItem?>): Int {
        return Iterables.indexOf(list) { i: DirectItem? -> i != null && i.itemId == item.itemId }
    }

    private fun getItem(itemId: String): DirectItem? {
        val items = items.value ?: return null
        return items.asSequence()
            .filter { it.itemId == itemId }
            .firstOrNull()
    }

    private fun handleBroadcastReactionRequest(
        data: MutableLiveData<Resource<Any?>>,
        item: DirectItem,
        request: Call<DirectThreadBroadcastResponse?>,
    ) {
        request.enqueue(object : Callback<DirectThreadBroadcastResponse?> {
            override fun onResponse(
                call: Call<DirectThreadBroadcastResponse?>,
                response: Response<DirectThreadBroadcastResponse?>,
            ) {
                if (!response.isSuccessful) {
                    if (response.errorBody() != null) {
                        handleErrorBody(call, response, data)
                        return
                    }
                    data.postValue(error(R.string.generic_failed_request, item))
                    return
                }
                val body = response.body()
                if (body == null) {
                    data.postValue(error(R.string.generic_null_response, item))
                }
                // otherwise nothing to do? maybe update the timestamp in the emoji?
            }

            override fun onFailure(call: Call<DirectThreadBroadcastResponse?>, t: Throwable) {
                data.postValue(error(t.message, item))
                Log.e(TAG, "enqueueRequest: onFailure: ", t)
            }
        })
    }

    private fun stopCurrentRequest() {
        chatsRequest?.let {
            if (it.isExecuted || it.isCanceled) return
            it.cancel()
        }
        fetching.postValue(success(Any()))
    }

    private fun getCurrentUserId(data: MutableLiveData<Resource<Any?>>): Long? {
        if (currentUser == null || currentUser.pk <= 0) {
            data.postValue(error(R.string.dms_ERROR_INVALID_USER, null))
            return null
        }
        return currentUser.pk
    }

    fun removeThread() {
        val pendingValue = isPending.value
        val threadInPending = pendingValue != null && pendingValue
        inboxManager.removeThread(threadId)
        if (threadInPending) {
            val totalValue = inboxManager.getPendingRequestsTotal().value ?: return
            inboxManager.setPendingRequestsTotal(totalValue - 1)
        }
    }

    fun updateTitle(newTitle: String): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val addUsersRequest = service.updateTitle(threadId, newTitle.trim { it <= ' ' })
        handleDetailsChangeRequest(data, addUsersRequest)
        return data
    }

    fun addMembers(users: Set<User>): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val addUsersRequest = service.addUsers(
            threadId,
            users.stream()
                .filter { obj: User? -> Objects.nonNull(obj) }
                .map { obj: User -> obj.pk }
                .collect(Collectors.toList())
        )
        handleDetailsChangeRequest(data, addUsersRequest)
        return data
    }

    fun removeMember(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        val request = service.removeUsers(threadId, setOf(user.pk))
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                data.postValue(success(Any()))
                var activeUsers = users.value
                var leftUsersValue = leftUsers.value
                if (activeUsers == null) {
                    activeUsers = emptyList()
                }
                if (leftUsersValue == null) {
                    leftUsersValue = emptyList()
                }
                val updatedActiveUsers = activeUsers.stream()
                    .filter { obj: User? -> Objects.nonNull(obj) }
                    .filter { u: User -> u.pk != user.pk }
                    .collect(Collectors.toList())
                val updatedLeftUsersBuilder = ImmutableList.builder<User>().addAll(leftUsersValue)
                if (!leftUsersValue.contains(user)) {
                    updatedLeftUsersBuilder.add(user)
                }
                val updatedLeftUsers = updatedLeftUsersBuilder.build()
                setThreadUsers(updatedActiveUsers, updatedLeftUsers)
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun isAdmin(user: User): Boolean {
        val adminUserIdsValue = adminUserIds.value
        return adminUserIdsValue != null && adminUserIdsValue.contains(user.pk)
    }

    fun makeAdmin(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        if (isAdmin(user)) return data
        val request = service.addAdmins(threadId, setOf(user.pk))
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                val currentAdminIds = adminUserIds.value
                val updatedAdminIds = ImmutableList.builder<Long>()
                    .addAll(currentAdminIds ?: emptyList())
                    .add(user.pk)
                    .build()
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.adminUserIds = updatedAdminIds
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun removeAdmin(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        if (!isAdmin(user)) return data
        val request = service.removeAdmins(threadId, setOf(user.pk))
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                val currentAdmins = adminUserIds.value ?: return
                val updatedAdminUserIds = currentAdmins.stream()
                    .filter { obj: Long? -> Objects.nonNull(obj) }
                    .filter { userId1: Long -> userId1 != user.pk }
                    .collect(Collectors.toList())
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.adminUserIds = updatedAdminUserIds
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun mute(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val muted = isMuted.value
        if (muted != null && muted) {
            data.postValue(success(Any()))
            return data
        }
        val request = service.mute(threadId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.muted = true
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun unmute(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val muted = isMuted.value
        if (muted != null && !muted) {
            data.postValue(success(Any()))
            return data
        }
        val request = service.unmute(threadId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.muted = false
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun muteMentions(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val mentionsMuted = isMentionsMuted.value
        if (mentionsMuted != null && mentionsMuted) {
            data.postValue(success(Any()))
            return data
        }
        val request = service.muteMentions(threadId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.mentionsMuted = true
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun unmuteMentions(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val mentionsMuted = isMentionsMuted.value
        if (mentionsMuted != null && !mentionsMuted) {
            data.postValue(success(Any()))
            return data
        }
        val request = service.unmuteMentions(threadId)
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                data.postValue(success(Any()))
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.mentionsMuted = false
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun blockUser(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        friendshipService.changeBlock(false, user.pk, object : ServiceCallback<FriendshipChangeResponse?> {
            override fun onSuccess(result: FriendshipChangeResponse?) {
                refreshChats()
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun unblockUser(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        friendshipService.changeBlock(true, user.pk, object : ServiceCallback<FriendshipChangeResponse?> {
            override fun onSuccess(result: FriendshipChangeResponse?) {
                refreshChats()
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun restrictUser(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        friendshipService.toggleRestrict(user.pk, true, object : ServiceCallback<FriendshipRestrictResponse?> {
            override fun onSuccess(result: FriendshipRestrictResponse?) {
                refreshChats()
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun unRestrictUser(user: User): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        friendshipService.toggleRestrict(user.pk, false, object : ServiceCallback<FriendshipRestrictResponse?> {
            override fun onSuccess(result: FriendshipRestrictResponse?) {
                refreshChats()
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun approveUsers(users: List<User>): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val approveUsersRequest = service.approveParticipantRequests(
            threadId,
            users.stream()
                .filter { obj: User? -> Objects.nonNull(obj) }
                .map { obj: User -> obj.pk }
                .collect(Collectors.toList())
        )
        handleDetailsChangeRequest(data, approveUsersRequest, object : OnSuccessAction {
            override fun onSuccess() {
                pendingUserApproveDenySuccessAction(users)
            }
        })
        return data
    }

    fun denyUsers(users: List<User>): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val approveUsersRequest = service.declineParticipantRequests(
            threadId,
            users.stream()
                .map { obj: User -> obj.pk }
                .collect(Collectors.toList())
        )
        handleDetailsChangeRequest(data, approveUsersRequest, object : OnSuccessAction {
            override fun onSuccess() {
                pendingUserApproveDenySuccessAction(users)
            }
        })
        return data
    }

    private fun pendingUserApproveDenySuccessAction(users: List<User>) {
        val pendingRequestsValue = pendingRequests.value ?: return
        val pendingUsers = pendingRequestsValue.users
        if (pendingUsers == null || pendingUsers.isEmpty()) return
        val filtered = pendingUsers.stream()
            .filter { o: User -> !users.contains(o) }
            .collect(Collectors.toList())
        try {
            val clone = pendingRequestsValue.clone() as DirectThreadParticipantRequestsResponse
            clone.users = filtered
            val totalParticipantRequests = clone.totalParticipantRequests
            clone.totalParticipantRequests = if (totalParticipantRequests > 0) totalParticipantRequests - 1 else 0
            pendingRequests.postValue(clone)
        } catch (e: CloneNotSupportedException) {
            Log.e(TAG, "pendingUserApproveDenySuccessAction: ", e)
        }
    }

    fun approvalRequired(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val approvalRequiredToJoin = isApprovalRequiredToJoin.value
        if (approvalRequiredToJoin != null && approvalRequiredToJoin) {
            data.postValue(success(Any()))
            return data
        }
        val request = service.approvalRequired(threadId)
        handleDetailsChangeRequest(data, request, object : OnSuccessAction {
            override fun onSuccess() {
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.approvalRequiredForNewMembers = true
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }
        })
        return data
    }

    fun approvalNotRequired(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val approvalRequiredToJoin = isApprovalRequiredToJoin.value
        if (approvalRequiredToJoin != null && !approvalRequiredToJoin) {
            data.postValue(success(Any()))
            return data
        }
        val request = service.approvalNotRequired(threadId)
        handleDetailsChangeRequest(data, request, object : OnSuccessAction {
            override fun onSuccess() {
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.approvalRequiredForNewMembers = false
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }
        })
        return data
    }

    fun leave(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val request = service.leave(threadId)
        handleDetailsChangeRequest(data, request)
        return data
    }

    fun end(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val request = service.end(threadId)
        handleDetailsChangeRequest(data, request, object : OnSuccessAction {
            override fun onSuccess() {
                val currentThread = thread.value ?: return
                try {
                    val thread = currentThread.clone() as DirectThread
                    thread.inputMode = 1
                    inboxManager.setThread(threadId, thread)
                } catch (e: CloneNotSupportedException) {
                    Log.e(TAG, "onResponse: ", e)
                }
            }
        })
        return data
    }

    private fun handleDetailsChangeRequest(
        data: MutableLiveData<Resource<Any?>>,
        request: Call<DirectThreadDetailsChangeResponse?>,
        action: OnSuccessAction? = null,
    ) {
        request.enqueue(object : Callback<DirectThreadDetailsChangeResponse?> {
            override fun onResponse(
                call: Call<DirectThreadDetailsChangeResponse?>,
                response: Response<DirectThreadDetailsChangeResponse?>,
            ) {
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                val changeResponse = response.body()
                if (changeResponse == null) {
                    data.postValue(error(R.string.generic_null_response, null))
                    return
                }
                data.postValue(success(Any()))
                val thread = changeResponse.thread
                if (thread != null) {
                    setThread(thread, true)
                }
                action?.onSuccess()
            }

            override fun onFailure(call: Call<DirectThreadDetailsChangeResponse?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
    }

    fun markAsSeen(directItem: DirectItem): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val request = service.markAsSeen(threadId, directItem)
        if (request == null) {
            data.postValue(error("request was null", null))
            return data
        }
        request.enqueue(object : Callback<DirectItemSeenResponse?> {
            override fun onResponse(
                call: Call<DirectItemSeenResponse?>,
                response: Response<DirectItemSeenResponse?>,
            ) {
                if (currentUser == null) return
                if (!response.isSuccessful) {
                    handleErrorBody(call, response, data)
                    return
                }
                val seenResponse = response.body()
                if (seenResponse == null) {
                    data.postValue(error(R.string.generic_null_response, null))
                    return
                }
                inboxManager.fetchUnseenCount()
                val payload = seenResponse.payload ?: return
                val timestamp = payload.timestamp
                val thread = thread.value ?: return
                val currentLastSeenAt = thread.lastSeenAt
                val lastSeenAt = if (currentLastSeenAt == null) HashMap() else HashMap(currentLastSeenAt)
                lastSeenAt[currentUser.pk] = DirectThreadLastSeenAt(timestamp, directItem.itemId)
                thread.lastSeenAt = lastSeenAt
                setThread(thread, true)
                data.postValue(success(Any()))
            }

            override fun onFailure(
                call: Call<DirectItemSeenResponse?>,
                t: Throwable,
            ) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    private interface OnSuccessAction {
        fun onSuccess()
    }

    companion object {
        private val TAG = ThreadManager::class.java.simpleName
        private val LOCK = Any()
        private val INSTANCE_MAP: MutableMap<String, ThreadManager> = ConcurrentHashMap()

        @JvmStatic
        fun getInstance(
            threadId: String,
            pending: Boolean,
            currentUser: User,
            contentResolver: ContentResolver,
            viewerId: Long,
            csrfToken: String,
            deviceUuid: String,
        ): ThreadManager {
            var instance = INSTANCE_MAP[threadId]
            if (instance == null) {
                synchronized(LOCK) {
                    instance = INSTANCE_MAP[threadId]
                    if (instance == null) {
                        instance = ThreadManager(threadId, pending, currentUser, contentResolver, viewerId, csrfToken, deviceUuid)
                        INSTANCE_MAP[threadId] = instance!!
                    }
                }
            }
            return instance!!
        }
    }

    init {
        this.currentUser = currentUser
        this.contentResolver = contentResolver
        this.viewerId = viewerId
        service = DirectMessagesService.getInstance(csrfToken, viewerId, deviceUuid)
        mediaService = MediaService.getInstance(deviceUuid, csrfToken, viewerId)
        friendshipService = FriendshipService.getInstance(deviceUuid, csrfToken, viewerId)
        // fetchChats();
    }
}