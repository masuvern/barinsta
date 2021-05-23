package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class VideoBroadcastOptions(
    clientContext: String,
    threadIdOrUserIds: ThreadIdOrUserIds,
    val videoResult: String,
    val uploadId: String,
    val sampled: Boolean
) : BroadcastOptions(
    clientContext,
    threadIdOrUserIds,
    BroadcastItemType.VIDEO
) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "video_result" to videoResult,
            "upload_id" to uploadId,
            "sampled" to sampled.toString()
        )
}