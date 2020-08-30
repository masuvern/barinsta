package awais.instagrabber.repositories.responses;

public class FriendshipRepositoryChangeResponseRootObject {
    private FriendshipRepositoryChangeResponseFriendshipStatus friendshipStatus;
    private String status;

    public FriendshipRepositoryChangeResponseRootObject(final FriendshipRepositoryChangeResponseFriendshipStatus friendshipStatus, final String status) {
        this.friendshipStatus = friendshipStatus;
        this.status = status;
    }

    public FriendshipRepositoryChangeResponseFriendshipStatus getFriendshipStatus() {
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
