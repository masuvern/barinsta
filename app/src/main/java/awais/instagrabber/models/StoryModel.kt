package awais.instagrabber.models

import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.models.stickers.*
import java.io.Serializable

data class StoryModel(
    val storyMediaId: String? = null,
    val storyUrl: String? = null,
    var thumbnail: String? = null,
    val itemType: MediaItemType? = null,
    val timestamp: Long = 0,
    val username: String? = null,
    val userId: Long = 0,
    val canReply: Boolean = false,
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