package awais.instagrabber.models

import awais.instagrabber.utils.TextUtils

data class HighlightModel(
    val title: String? = null,
    val id: String = "",
    val thumbnailUrl: String = "",
    val timestamp: Long = 0,
    val mediaCount: Int = 0,
) {
    val dateTime: String
        get() = TextUtils.epochSecondToString(timestamp)
}