package awais.instagrabber.models

import awais.instagrabber.utils.Utils
import java.util.*

data class HighlightModel(
    val title: String,
    val id: String,
    val thumbnailUrl: String,
    val timestamp: Long,
    val mediaCount: Int
) {
    val dateTime: String
        get() = Utils.datetimeParser.format(Date(timestamp * 1000L))
}