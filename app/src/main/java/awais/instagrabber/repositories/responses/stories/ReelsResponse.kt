package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class ReelsResponse(
    val status: String?,
    val reel: Story?,
    val broadcast: Broadcast?
) : Serializable