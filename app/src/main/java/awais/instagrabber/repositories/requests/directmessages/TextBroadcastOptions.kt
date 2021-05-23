package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class TextBroadcastOptions(
    clientContext: String,
    threadIdOrUserIds: ThreadIdOrUserIds,
    val text: String
) : BroadcastOptions(
    clientContext,
    threadIdOrUserIds,
    BroadcastItemType.TEXT
) {
    override val formMap: Map<String, String>
        get() = mapOf("text" to text)
}