package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class MediaShareBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val mediaId: String
) : BroadcastOptions(
    clientContext,
    threadIdsOrUserIds,
    BroadcastItemType.MEDIA_SHARE
) {
    override val formMap: Map<String, String>
        get() = mapOf("media_id" to mediaId)
}