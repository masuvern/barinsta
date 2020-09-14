package awais.instagrabber.repositories.responses;

public class FriendshipRepoChangeRootResponse {
    private FriendshipRepoChangeResponseFriendshipStatus friendshipStatus;
    private String status;

    public FriendshipRepoChangeRootResponse(final FriendshipRepoChangeResponseFriendshipStatus friendshipStatus,
                                            final String status) {
        this.friendshipStatus = friendshipStatus;
        this.status = status;
    }

    public FriendshipRepoChangeResponseFriendshipStatus getFriendshipStatus() {
        return friendshipStatus;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "FriendshipRepositoryChangeResponseRootObject{" +
                "friendshipStatus=" + friendshipStatus +
                ", status='" + status + '\'' +
                '}';
    }
}
