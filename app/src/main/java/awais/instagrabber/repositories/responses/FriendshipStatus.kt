package awais.instagrabber.repositories.responses

import java.io.Serializable

data class FriendshipStatus(
    val following: Boolean,
    val followedBy: Boolean,
    val blocking: Boolean,
    val muting: Boolean,
    val isPrivate: Boolean,
    val incomingRequest: Boolean,
    val outgoingRequest: Boolean,
    val isBestie: Boolean,
    val isRestricted: Boolean,
    val isMutingReel: Boolean
) : Serializable