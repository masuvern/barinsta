package awais.instagrabber.repositories.responses

import java.io.Serializable

data class Caption(val userId: Long, var text: String?) : Serializable {
    var pk: Long = 0
}