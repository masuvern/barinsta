package awais.instagrabber.repositories.responses.stories

import java.io.Serializable
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils

data class Story(
    // universal
    val id: String?,
    val latestReelMedia: Long?, // = timestamp
    var seen: Long?,
    val user: User?,
    // for stories
    val mediaCount: Int?,
    val muted: Boolean?,
    val hasBestiesMedia: Boolean?,
    val items: List<StoryMedia>?, // may be null
    // for highlights
    val coverMedia: CoverMedia?,
    val title: String?,
    // invented fields
    val broadcast: Broadcast? // does not naturally occur
) : Serializable {
    val dateTime: String
        get() = if (latestReelMedia != null) TextUtils.epochSecondToString(latestReelMedia) else ""
}