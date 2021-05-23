package awais.instagrabber.models

import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.models.stickers.*
import java.io.Serializable

data class StoryModel(
    val storyMediaId: String?,
    val storyUrl: String?,
    var thumbnail: String?,
    val itemType: MediaItemType?,
    val timestamp: Long,
    val username: String?,
    val userId: Long,
    val canReply: Boolean
) : Serializable {
    var videoUrl: String? = null
    var tappableShortCode: String? = null
    val tappableId: String? = null
    var spotify: String? = null
    var poll: PollModel? = null
    var question: QuestionModel? = null
    var slider: SliderModel? = null
    var quiz: QuizModel? = null
    var swipeUp: SwipeUpModel? = null
    var mentions: Array<String>? = null
    var position = 0
    var isCurrentSlide = false
}