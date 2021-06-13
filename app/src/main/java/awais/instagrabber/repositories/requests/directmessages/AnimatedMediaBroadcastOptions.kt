package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType
import awais.instagrabber.repositories.responses.giphy.GiphyGif

class AnimatedMediaBroadcastOptions(
    clientContext: String,
    threadIdOrUserIds: ThreadIdOrUserIds,
    val giphyGif: GiphyGif
) : BroadcastOptions(
    clientContext,
    threadIdOrUserIds,
    BroadcastItemType.ANIMATED_MEDIA
) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "is_sticker" to giphyGif.isSticker.toString(),
            "id" to giphyGif.id
        )
}