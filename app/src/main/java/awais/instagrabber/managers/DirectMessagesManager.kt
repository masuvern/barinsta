package awais.instagrabber.managers

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import awais.instagrabber.managers.ThreadManager.Companion.getInstance
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.repositories.requests.directmessages.ThreadIdOrUserIds.Companion.of
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.DirectThread
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.getCsrfTokenFromCookie
import awais.instagrabber.utils.getUserIdFromCookie
import awais.instagrabber.webservices.DirectMessagesService
import awais.instagrabber.webservices.DirectMessagesService.Companion.getInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object DirectMessagesManager {
    val inboxManager: InboxManager by lazy { InboxManager.getInstance(false) }
    val pendingInboxManager: InboxManager by lazy { InboxManager.getInstance(true) }

    private val TAG = DirectMessagesManager::class.java.simpleName
    private val viewerId: Long
    private val deviceUuid: String
    private val csrfToken: String
    private val service: DirectMessagesService

    fun moveThreadFromPending(threadId: String) {
        val pendingThreads = pendingInboxManager.threads.value ?: return
        val index = pendingThreads.indexOfFirst { it.threadId == threadId }
        if (index < 0) return
        val thread = pendingThreads[index]
        val threadFirstDirectItem = thread.firstDirectItem ?: return
        val threads = inboxManager.threads.value
        var insertIndex = 0
        if (threads != null) {
            for (tempThread in threads) {
                val firstDirectItem = tempThread.firstDirectItem ?: continue
                val timestamp = firstDirectItem.getTimestamp()
                if (timestamp < threadFirstDirectItem.getTimestamp()) {
                    break
                }
                insertIndex++
            }
        }
        thread.pending = false
        inboxManager.addThread(thread, insertIndex)
        pendingInboxManager.removeThread(threadId)
        val currentTotal = inboxManager.getPendingRequestsTotal().value ?: return
        inboxManager.setPendingRequestsTotal(currentTotal - 1)
    }

    fun getThreadManager(
        threadId: String,
        pending: Boolean,
        currentUser: User,
        contentResolver: ContentResolver,
    ): ThreadManager {
        return getInstance(threadId, pending, currentUser, contentResolver, viewerId, csrfToken, deviceUuid)
    }

    suspend fun createThread(userPk: Long): DirectThread = service.createThread(listOf(userPk), null)

    fun sendMedia(recipients: Set<RankedRecipient>, mediaId: String, scope: CoroutineScope) {
        val resultsCount = intArrayOf(0)
        val callback: () -> Unit = {
            resultsCount[0]++
            if (resultsCount[0] == recipients.size) {
                inboxManager.refresh(scope)
            }
        }
        for (recipient in recipients) {
            sendMedia(recipient, mediaId, false, callback, scope)
        }
    }

    fun sendMedia(recipient: RankedRecipient, mediaId: String, scope: CoroutineScope) {
        sendMedia(recipient, mediaId, true, null, scope)
    }

    private fun sendMedia(
        recipient: RankedRecipient,
        mediaId: String,
        refreshInbox: Boolean,
        callback: (() -> Unit)?,
        scope: CoroutineScope,
    ) {
        if (recipient.thread == null && recipient.user != null) {
            // create thread and forward
            scope.launch(Dispatchers.IO) {
                try {
                    val (threadId) = createThread(recipient.user.pk)
                    val threadIdTemp = threadId ?: return@launch
                    sendMedia(threadIdTemp, mediaId, scope) {
                        if (refreshInbox) {
                            inboxManager.refresh(scope)
                        }
                        callback?.invoke()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "sendMedia: ", e)
                    callback?.invoke()
                }
            }
        }
        if (recipient.thread == null) return
        // just forward
        val thread = recipient.thread
        val threadId = thread.threadId ?: return
        sendMedia(threadId, mediaId, scope) {
            if (refreshInbox) {
                inboxManager.refresh(scope)
            }
            callback?.invoke()
        }
    }

    private fun sendMedia(
        threadId: String,
        mediaId: String,
        scope: CoroutineScope,
        callback: (() -> Unit)?,
    ): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        scope.launch(Dispatchers.IO) {
            try {
                service.broadcastMediaShare(
                    UUID.randomUUID().toString(),
                    of(threadId),
                    mediaId
                )
                data.postValue(success(Any()))
                callback?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "sendMedia: ", e)
                data.postValue(error(e.message, null))
                callback?.invoke()
            }
        }
        return data
    }

    init {
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        viewerId = getUserIdFromCookie(cookie)
        deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        val csrfToken = getCsrfTokenFromCookie(cookie)
        require(!csrfToken.isNullOrBlank() && viewerId != 0L && deviceUuid.isNotBlank()) { "User is not logged in!" }
        this.csrfToken = csrfToken
        service = getInstance(csrfToken, viewerId, deviceUuid)
    }
}