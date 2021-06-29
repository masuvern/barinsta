package awais.instagrabber.repositories.responses

import java.io.Serializable

data class HdProfilePicUrlInfo(val url: String, private val width: Int, private val height: Int) : Serializable