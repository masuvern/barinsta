package awais.instagrabber.repositories.responses.stories

import java.io.Serializable
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User

data class Story(
    val id: Long?,
    val latestReelMedia: Long?, // = timestamp
    val seen: Long?,
    val user: User?,
    val muted: Boolean?,
    val hasBestiesMedia: Boolean?,
    val mediaCount: Int?,
    val items: List<Media>? // may be null
) : Serializable