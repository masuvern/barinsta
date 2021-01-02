package awais.instagrabber.repositories.responses.directmessages;

public class DirectUserFriendshipStatus {
    private final boolean following;
    private final boolean blocking;
    private final boolean isPrivate;
    private final boolean incomingRequest;
    private final boolean outgoingRequest;
    private final boolean isBestie;

    public DirectUserFriendshipStatus(final boolean following,
                                      final boolean blocking,
                                      final boolean isPrivate,
                                      final boolean incomingRequest,
                                      final boolean outgoingRequest,
                                      final boolean isBestie) {
        this.following = following;
        this.blocking = blocking;
        this.isPrivate = isPrivate;
        this.incomingRequest = incomingRequest;
        this.outgoingRequest = outgoingRequest;
        this.isBestie = isBestie;
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

    @Override
    public String toString() {
        return "DirectInboxFeedResponseFriendshipStatus{" +
                "following=" + following +
                ", blocking=" + blocking +
                ", is_private=" + isPrivate +
                ", incomingRequest=" + incomingRequest +
                ", outgoingRequest=" + outgoingRequest +
                ", isBestie=" + isBestie +
                '}';
    }
}
