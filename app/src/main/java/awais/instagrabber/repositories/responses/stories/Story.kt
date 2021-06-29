package awais.instagrabber.repositories.responses.stories

import java.io.Serializable
import awais.instagrabber.repositories.responses.ImageUrl
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils

data class Story(
    // universal
    val id: String?,
    val latestReelMedia: Long?, // = timestamp
    val mediaCount: Int?,
    // for stories and highlights
    var seen: Long?,
    val user: User?,
    // for stories
    val muted: Boolean?,
    val hasBestiesMedia: Boolean?,
    val items: List<StoryMedia>?, // may be null
    // for highlights
    val coverMedia: CoverMedia?,
    val title: String?,
    // for archives
    val coverImageVersion: ImageUrl?,
    // invented fields
    val broadcast: Broadcast? // does not naturally occur
) : Serializable {
    val dateTime: String
        get() = if (latestReelMedia != null) TextUtils.epochSecondToString(latestReelMedia) else ""
    // note that archives have property "timestamp" but is ignored
}