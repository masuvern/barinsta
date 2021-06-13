package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class PhotoBroadcastOptions(
    clientContext: String,
    threadIdOrUserIds: ThreadIdOrUserIds,
    val allowFullAspectRatio: Boolean,
    val uploadId: String
) : BroadcastOptions(
    clientContext,
    threadIdOrUserIds,
    BroadcastItemType.IMAGE
) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "allow_full_aspect_ratio" to allowFullAspectRatio.toString(),
            "upload_id" to uploadId
        )
}