package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

import java.util.List;

public class FriendshipRepoRestrictRootResponse {
    private List<FriendshipRepoRestrictResponseUsersItem> users;
    private String status;

    public FriendshipRepoRestrictRootResponse(final List<FriendshipRepoRestrictResponseUsersItem> users, final String status) {
        this.users = users;
        this.status = status;
    }

    public List<FriendshipRepoRestrictResponseUsersItem> getUsers() {
        return users;
    }

    public String getStatus() {
        return status;
    }

    @NonNull
    @Override
    public String toString() {
        return "FriendshipRepoRestrictRootResponse{" +
                "users=" + users +
                ", status='" + status + '\'' +
                '}';
    }
}
