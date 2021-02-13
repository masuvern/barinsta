package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class FriendshipStatus implements Serializable {
    private final boolean following;
    private final boolean followedBy;
    private final boolean blocking;
    private final boolean muting;
    private final boolean isPrivate;
    private final boolean incomingRequest;
    private final boolean outgoingRequest;
    private final boolean isBestie;
    private final boolean isRestricted;
    private final boolean isMutingReel;

    public FriendshipStatus(final boolean following,
                            final boolean followedBy,
                            final boolean blocking,
                            final boolean muting,
                            final boolean isPrivate,
                            final boolean incomingRequest,
                            final boolean outgoingRequest,
                            final boolean isBestie,
                            final boolean isRestricted,
                            final boolean isMutingReel) {
        this.following = following;
        this.followedBy = followedBy;
        this.blocking = blocking;
        this.muting = muting;
        this.isPrivate = isPrivate;
        this.incomingRequest = incomingRequest;
        this.outgoingRequest = outgoingRequest;
        this.isBestie = isBestie;
        this.isRestricted = isRestricted;
        this.isMutingReel = isMutingReel;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isFollowedBy() {
        return followedBy;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean isMuting() {
        return muting;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isIncomingRequest() {
        return incomingRequest;
    }

    public boolean isOutgoingRequest() {
        return outgoingRequest;
    }

    public boolean isBestie() {
        return isBestie;
    }

    public boolean isRestricted() {
        return isRestricted;
    }

    public boolean isMutingReel() {
        return isMutingReel;
    }

    @NonNull
    @Override
    public String toString() {
        return "FriendshipStatus{" +
                "following=" + following +
                ", followedBy=" + followedBy +
                ", blocking=" + blocking +
                ", muting=" + muting +
                ", isPrivate=" + isPrivate +
                ", incomingRequest=" + incomingRequest +
                ", outgoingRequest=" + outgoingRequest +
                ", isBestie=" + isBestie +
                ", isRestricted=" + isRestricted +
                ", isMutingReel=" + isMutingReel +
                '}';
    }
}
