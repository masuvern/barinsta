package awais.instagrabber.repositories.responses;

public class FriendshipRepositoryChangeResponseFriendshipStatus {
    private boolean following;
    private boolean followedBy;
    private boolean blocking;
    private boolean muting;
    private boolean isPrivate;
    private boolean incomingRequest;
    private boolean outgoingRequest;
    private boolean isBestie;

    public FriendshipRepositoryChangeResponseFriendshipStatus(final boolean following,
                                                              final boolean followedBy,
                                                              final boolean blocking,
                                                              final boolean muting,
                                                              final boolean isPrivate,
                                                              final boolean incomingRequest,
                                                              final boolean outgoingRequest,
                                                              final boolean isBestie) {
        this.following = following;
        this.followedBy = followedBy;
        this.blocking = blocking;
        this.muting = muting;
        this.isPrivate = isPrivate;
        this.incomingRequest = incomingRequest;
        this.outgoingRequest = outgoingRequest;
        this.isBestie = isBestie;
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

    @Override
    public String toString() {
        return "FriendshipRepositoryChangeResponseFriendshipStatus{" +
                "following=" + following +
                ", followedBy=" + followedBy +
                ", blocking=" + blocking +
                ", muting=" + muting +
                ", isPrivate=" + isPrivate +
                ", incomingRequest=" + incomingRequest +
                ", outgoingRequest=" + outgoingRequest +
                ", isBestie=" + isBestie +
                '}';
    }
}
