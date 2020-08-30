package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

public class FriendshipRepoRestrictResponseFriendshipStatus extends FriendshipRepoChangeResponseFriendshipStatus {
    private boolean isRestricted;

    public FriendshipRepoRestrictResponseFriendshipStatus(final boolean following,
                                                          final boolean followedBy,
                                                          final boolean blocking,
                                                          final boolean muting,
                                                          final boolean isPrivate,
                                                          final boolean incomingRequest,
                                                          final boolean outgoingRequest,
                                                          final boolean isBestie,
                                                          final boolean isRestricted) {
        super(following, followedBy, blocking, muting, isPrivate, incomingRequest, outgoingRequest, isBestie);
        this.isRestricted = isRestricted;
    }

    public boolean isRestricted() {
        return isRestricted;
    }

    @NonNull
    @Override
    public String toString() {
        return "FriendshipRepoRestrictResponseFriendshipStatus{" +
                "following=" + isFollowing() +
                ", followedBy=" + isFollowedBy() +
                ", blocking=" + isBlocking() +
                ", muting=" + isMuting() +
                ", isPrivate=" + isPrivate() +
                ", incomingRequest=" + isIncomingRequest() +
                ", outgoingRequest=" + isOutgoingRequest() +
                ", isBestie=" + isBestie() +
                ", isRestricted=" + isRestricted() +
                '}';
    }
}
