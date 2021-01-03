package awais.instagrabber.repositories.responses.directmessages;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class DirectUserFriendshipStatus implements Serializable {
    private final boolean following;
    private final boolean blocking;
    private final boolean isPrivate;
    private final boolean incomingRequest;
    private final boolean outgoingRequest;
    private final boolean isBestie;
    private final boolean isRestricted;

    public DirectUserFriendshipStatus(final boolean following,
                                      final boolean blocking,
                                      final boolean isPrivate,
                                      final boolean incomingRequest,
                                      final boolean outgoingRequest,
                                      final boolean isBestie, final boolean isRestricted) {
        this.following = following;
        this.blocking = blocking;
        this.isPrivate = isPrivate;
        this.incomingRequest = incomingRequest;
        this.outgoingRequest = outgoingRequest;
        this.isBestie = isBestie;
        this.isRestricted = isRestricted;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isBlocking() {
        return blocking;
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

    @NonNull
    @Override
    public String toString() {
        return "DirectInboxFeedResponseFriendshipStatus{" +
                "following=" + following +
                ", blocking=" + blocking +
                ", isPrivate=" + isPrivate +
                ", incomingRequest=" + incomingRequest +
                ", outgoingRequest=" + outgoingRequest +
                ", isBestie=" + isBestie +
                ", isRestricted" + isRestricted +
                '}';
    }
}
