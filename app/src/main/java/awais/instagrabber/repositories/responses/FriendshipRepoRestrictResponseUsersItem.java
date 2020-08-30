package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

public class FriendshipRepoRestrictResponseUsersItem {
    private long pk;
    private String username;
    private String fullName;
    private boolean isPrivate;
    private String profilePicUrl;
    private FriendshipRepoRestrictResponseFriendshipStatus friendshipStatus;
    private boolean isVerified;

    public FriendshipRepoRestrictResponseUsersItem(final long pk, final String username, final String fullName, final boolean isPrivate, final String profilePicUrl, final FriendshipRepoRestrictResponseFriendshipStatus friendshipStatus, final boolean isVerified) {
        this.pk = pk;
        this.username = username;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
        this.profilePicUrl = profilePicUrl;
        this.friendshipStatus = friendshipStatus;
        this.isVerified = isVerified;
    }

    public long getPk() {
        return pk;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public FriendshipRepoRestrictResponseFriendshipStatus getFriendshipStatus() {
        return friendshipStatus;
    }

    public boolean isVerified() {
        return isVerified;
    }

    @NonNull
    @Override
    public String toString() {
        return "FriendshipRepoRestrictResponseUsersItem{" +
                "pk=" + pk +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", isPrivate=" + isPrivate +
                ", profilePicUrl='" + profilePicUrl + '\'' +
                ", friendshipStatus=" + friendshipStatus +
                ", isVerified=" + isVerified +
                '}';
    }
}
