package awais.instagrabber.repositories.responses.stories

import java.io.Serializable
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils

data class Story(
    val id: String?,
    val latestReelMedia: Long?, // = timestamp
    var seen: Long?,
    val user: User?,
    val muted: Boolean?,
    val hasBestiesMedia: Boolean?,
    val mediaCount: Int?,
    val items: List<StoryMedia>?, // may be null
    val broadcast: Broadcast? // does not naturally occur
) : Serializable {
    val dateTime: String
        get() = if (latestReelMedia != null) TextUtils.epochSecondToString(latestReelMedia) else ""
}