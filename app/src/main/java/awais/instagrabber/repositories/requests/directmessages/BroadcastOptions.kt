package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

sealed class BroadcastOptions(
    val clientContext: String,
    private val threadIdOrUserIds: ThreadIdOrUserIds,
    val itemType: BroadcastItemType
) {
    var repliedToItemId: String? = null
    var repliedToClientContext: String? = null
    val threadId: String?
        get() = threadIdOrUserIds.threadId
    val userIds: List<String>?
        get() = threadIdOrUserIds.userIds

    abstract val formMap: Map<String, String>
}

// TODO convert to data class once usages are migrated to kotlin
class ThreadIdOrUserIds(val threadId: String? = null, val userIds: List<String>? = null) {

    companion object {
        @JvmStatic
        fun of(threadId: String?): ThreadIdOrUserIds {
            return ThreadIdOrUserIds(threadId, null)
        }

        @JvmStatic
        fun of(userIds: List<String>?): ThreadIdOrUserIds {
            return ThreadIdOrUserIds(null, userIds)
        }
    }
}