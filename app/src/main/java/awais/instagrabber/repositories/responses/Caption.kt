package awais.instagrabber.repositories.responses

import java.io.Serializable

data class Caption(var pk: Long = 0, val userId: Long, var text: String) : Serializable