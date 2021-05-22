package awais.instagrabber.repositories.responses

import java.io.Serializable

data class FriendshipStatus(
    val isFollowing: Boolean,
    val isFollowedBy: Boolean,
    val isBlocking: Boolean,
    val isMuting: Boolean,
    val isPrivate: Boolean,
    val isIncomingRequest: Boolean,
    val isOutgoingRequest: Boolean,
    val isBestie: Boolean,
    val isRestricted: Boolean,
    val isMutingReel: Boolean
) : Serializable