package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class MediaShareBroadcastOptions(
    clientContext: String?,
    threadIdOrUserIds: ThreadIdOrUserIds,
    val mediaId: String
) : BroadcastOptions(
    clientContext,
    threadIdOrUserIds,
    BroadcastItemType.MEDIA_SHARE
) {

    override fun getFormMap(): Map<String, String> {
        return mapOf("media_id" to mediaId)
    }
}