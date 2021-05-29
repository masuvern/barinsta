package awais.instagrabber.managers

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import awais.instagrabber.R
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.getCsrfTokenFromCookie
import awais.instagrabber.utils.getUserIdFromCookie
import awais.instagrabber.webservices.DirectMessagesService
import awais.instagrabber.webservices.DirectMessagesService.Companion.getInstance
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.TimeUnit

class InboxManager private constructor(private val pending: Boolean) {
    private val inbox = MutableLiveData<Resource<DirectInbox?>>()
    private val unseenCount = MutableLiveData<Resource<Int?>>()
    private val pendingRequestsTotal = MutableLiveData(0)
    val threads: LiveData<List<DirectThread>>
    private val service: DirectMessagesService
    private var inboxRequest: Call<DirectInboxResponse?>? = null
    private var unseenCountRequest: Call<DirectBadgeCount?>? = null
    private var seqId: Long = 0
    private var cursor: String? = null
    private var hasOlder = true
    var viewer: User? = null
        private set

    fun getInbox(): LiveData<Resource<DirectInbox?>> {
        return Transformations.distinctUntilChanged(inbox)
    }

    fun getUnseenCount(): LiveData<Resource<Int?>> {
        return Transformations.distinctUntilChanged(unseenCount)
    }

    fun getPendingRequestsTotal(): LiveData<Int> {
        return Transformations.distinctUntilChanged(pendingRequestsTotal)
    }

    fun fetchInbox() {
        val inboxResource = inbox.value
        if (inboxResource != null && inboxResource.status === Resource.Status.LOADING || !hasOlder) return
        stopCurrentInboxRequest()
        inbox.postValue(loading(currentDirectInbox))
        inboxRequest = if (pending) service.fetchPendingInbox(cursor, seqId) else service.fetchInbox(cursor, seqId)
        inboxRequest?.enqueue(object : Callback<DirectInboxResponse?> {
            override fun onResponse(call: Call<DirectInboxResponse?>, response: Response<DirectInboxResponse?>) {
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "parseInboxResponse: Response is null")
                    inbox.postValue(error(R.string.generic_null_response, currentDirectInbox))
                    hasOlder = false
                    return
                }
                parseInboxResponse(body)
            }

            override fun onFailure(call: Call<DirectInboxResponse?>, t: Throwable) {
                Log.e(TAG, "Failed fetching dm inbox", t)
                inbox.postValue(error(t.message, currentDirectInbox))
                hasOlder = false
            }
        })
    }

    fun fetchUnseenCount() {
        val unseenCountResource = unseenCount.value
        if (unseenCountResource != null && unseenCountResource.status === Resource.Status.LOADING) return
        stopCurrentUnseenCountRequest()
        unseenCount.postValue(loading(currentUnseenCount))
        unseenCountRequest = service.fetchUnseenCount()
        unseenCountRequest?.enqueue(object : Callback<DirectBadgeCount?> {
            override fun onResponse(call: Call<DirectBadgeCount?>, response: Response<DirectBadgeCount?>) {
                val directBadgeCount = response.body()
                if (directBadgeCount == null) {
                    Log.e(TAG, "onResponse: directBadgeCount Response is null")
                    unseenCount.postValue(error(R.string.dms_inbox_error_null_count, currentUnseenCount))
                    return
                }
                unseenCount.postValue(success(directBadgeCount.badgeCount))
            }

            override fun onFailure(call: Call<DirectBadgeCount?>, t: Throwable) {
                Log.e(TAG, "Failed fetching unseen count", t)
                unseenCount.postValue(error(t.message, currentUnseenCount))
            }
        })
    }

    fun refresh() {
        cursor = null
        seqId = 0
        hasOlder = true
        fetchInbox()
        if (!pending) {
            fetchUnseenCount()
        }
    }

    private val currentDirectInbox: DirectInbox?
        get() {
            val inboxResource = inbox.value
            return inboxResource?.data
        }

    private fun parseInboxResponse(response: DirectInboxResponse) {
        if (response.status != "ok") {
            Log.e(TAG, "DM inbox fetch response: status not ok")
            inbox.postValue(error(R.string.generic_not_ok_response, currentDirectInbox))
            hasOlder = false
            return
        }
        seqId = response.seqId
        if (viewer == null) {
            viewer = response.viewer
        }
        val inbox = response.inbox ?: return
        if (!cursor.isNullOrBlank()) {
            val currentDirectInbox = currentDirectInbox
            currentDirectInbox?.let {
                val threads = it.threads
                val threadsCopy = if (threads == null) LinkedList() else LinkedList(threads)
                threadsCopy.addAll(inbox.threads ?: emptyList())
                inbox.threads = threads
            }
        }
        this.inbox.postValue(success(inbox))
        cursor = inbox.oldestCursor
        hasOlder = inbox.hasOlder
        pendingRequestsTotal.postValue(response.pendingRequestsTotal)
    }

    fun setThread(
        threadId: String,
        thread: DirectThread,
    ) {
        val inbox = currentDirectInbox ?: return
        val index = getThreadIndex(threadId, inbox)
        setThread(inbox, index, thread)
    }

    private fun setThread(
        inbox: DirectInbox,
        index: Int,
        thread: DirectThread,
    ) {
        if (index < 0) return
        synchronized(this.inbox) {
            val threads = inbox.threads
            val threadsCopy = if (threads == null) LinkedList() else LinkedList(threads)
            threadsCopy[index] = thread
            try {
                val clone = inbox.clone() as DirectInbox
                clone.threads = threadsCopy
                this.inbox.postValue(success(clone))
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "setThread: ", e)
            }
        }
    }

    fun addItemsToThread(
        threadId: String,
        insertIndex: Int,
        items: Collection<DirectItem>,
    ) {
        val inbox = currentDirectInbox ?: return
        synchronized(THREAD_LOCKS.getUnchecked(threadId)) {
            val index = getThreadIndex(threadId, inbox)
            if (index < 0) return
            val threads = inbox.threads ?: return
            val thread = threads[index]
            val threadItems = thread.items
            val list = if (threadItems == null) LinkedList() else LinkedList(threadItems)
            if (insertIndex >= 0) {
                list.addAll(insertIndex, items)
            } else {
                list.addAll(items)
            }
            try {
                val threadClone = thread.clone() as DirectThread
                threadClone.items = list
                setThread(inbox, index, threadClone)
            } catch (e: Exception) {
                Log.e(TAG, "addItemsToThread: ", e)
            }
        }
    }

    fun setItemsToThread(
        threadId: String,
        updatedItems: List<DirectItem>,
    ) {
        val inbox = currentDirectInbox ?: return
        synchronized(THREAD_LOCKS.getUnchecked(threadId)) {
            val index = getThreadIndex(threadId, inbox)
            if (index < 0) return
            val threads = inbox.threads ?: return
            val thread = threads[index]
            try {
                val threadClone = thread.clone() as DirectThread
                threadClone.items = updatedItems
                setThread(inbox, index, threadClone)
            } catch (e: Exception) {
                Log.e(TAG, "setItemsToThread: ", e)
            }
        }
    }

    private fun getThreadIndex(
        threadId: String,
        inbox: DirectInbox,
    ): Int {
        val threads = inbox.threads
        return if (threads == null || threads.isEmpty()) {
            -1
        } else threads.indexOfFirst { it.threadId == threadId }
    }

    private val currentUnseenCount: Int?
        get() {
            val unseenCountResource = unseenCount.value
            return unseenCountResource?.data
        }

    private fun stopCurrentInboxRequest() {
        inboxRequest?.let {
            if (it.isCanceled || it.isExecuted) return
            it.cancel()
        }
        inboxRequest = null
    }

    private fun stopCurrentUnseenCountRequest() {
        unseenCountRequest?.let {
            if (it.isCanceled || it.isExecuted) return
            it.cancel()
        }
        unseenCountRequest = null
    }

    fun onDestroy() {
        stopCurrentInboxRequest()
        stopCurrentUnseenCountRequest()
    }

    fun addThread(thread: DirectThread, insertIndex: Int) {
        if (insertIndex < 0) return
        synchronized(inbox) {
            val currentDirectInbox = currentDirectInbox ?: return
            val threads = currentDirectInbox.threads
            val threadsCopy = if (threads == null) LinkedList() else LinkedList(threads)
            threadsCopy.add(insertIndex, thread)
            try {
                val clone = currentDirectInbox.clone() as DirectInbox
                clone.threads = threadsCopy
                inbox.setValue(success(clone))
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "setThread: ", e)
            }
        }
    }

    fun removeThread(threadId: String) {
        synchronized(inbox) {
            val currentDirectInbox = currentDirectInbox ?: return
            val threads = currentDirectInbox.threads ?: return
            val threadsCopy = threads.asSequence().filter { it.threadId != threadId }.toList()
            try {
                val clone = currentDirectInbox.clone() as DirectInbox
                clone.threads = threadsCopy
                inbox.postValue(success(clone))
            } catch (e: CloneNotSupportedException) {
                Log.e(TAG, "setThread: ", e)
            }
        }
    }

    fun setPendingRequestsTotal(total: Int) {
        pendingRequestsTotal.postValue(total)
    }

    fun containsThread(threadId: String?): Boolean {
        if (threadId == null) return false
        synchronized(inbox) {
            val currentDirectInbox = currentDirectInbox ?: return false
            val threads = currentDirectInbox.threads ?: return false
            return threads.any { it.threadId == threadId }
        }
    }

    companion object {
        private val TAG = InboxManager::class.java.simpleName
        private val THREAD_LOCKS = CacheBuilder
            .newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES) // max lock time ever expected
            .build<String, Any>(CacheLoader.from<Any> { Object() })
        private val THREAD_COMPARATOR = Comparator { t1: DirectThread, t2: DirectThread ->
            val t1FirstDirectItem = t1.firstDirectItem
            val t2FirstDirectItem = t2.firstDirectItem
            if (t1FirstDirectItem == null && t2FirstDirectItem == null) return@Comparator 0
            if (t1FirstDirectItem == null) return@Comparator 1
            if (t2FirstDirectItem == null) return@Comparator -1
            t2FirstDirectItem.getTimestamp().compareTo(t1FirstDirectItem.getTimestamp())
        }

        fun getInstance(pending: Boolean): InboxManager {
            return InboxManager(pending)
        }
    }

    init {
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        val viewerId = getUserIdFromCookie(cookie)
        val deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        val csrfToken = getCsrfTokenFromCookie(cookie)
        require(!csrfToken.isNullOrBlank() && viewerId != 0L && deviceUuid.isNotBlank()) { "User is not logged in!" }
        service = getInstance(csrfToken, viewerId, deviceUuid)

        // Transformations
        threads = Transformations.distinctUntilChanged(Transformations.map(inbox) { inboxResource: Resource<DirectInbox?>? ->
            if (inboxResource == null) {
                return@map emptyList()
            }
            val inbox = inboxResource.data
            val threads = inbox?.threads ?: emptyList()
            ImmutableList.sortedCopyOf(THREAD_COMPARATOR, threads)
        })
        fetchInbox()
        if (!pending) {
            fetchUnseenCount()
        }
    }
}