package awais.instagrabber.models

import awais.instagrabber.utils.TextUtils
import java.util.*

data class HighlightModel(
    val title: String?,
    val id: String,
    val thumbnailUrl: String,
    val timestamp: Long,
    val mediaCount: Int
) {
    val dateTime: String
        get() = TextUtils.epochSecondToString(timestamp)
}