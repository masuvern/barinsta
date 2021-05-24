package awais.instagrabber.models

import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils
import java.io.Serializable
import java.util.*

data class FeedStoryModel(
    val storyMediaId: String,
    val profileModel: User,
    var isFullyRead: Boolean,
    val timestamp: Long,
    val firstStoryModel: StoryModel?,
    val mediaCount: Int,
    val isLive: Boolean,
    val isBestie: Boolean
) : Serializable {
    val dateTime: String
        get() = TextUtils.epochSecondToString(timestamp)
}