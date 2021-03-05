package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class User implements Serializable {
    private final long pk;
    private final String username;
    private final String fullName;
    private final boolean isPrivate;
    private final String profilePicUrl;
    private final String profilePicId;
    private FriendshipStatus friendshipStatus;
    private final boolean isVerified;
    private final boolean hasAnonymousProfilePicture;
    private final boolean isUnpublished;
    private final boolean isFavorite;
    private final boolean isDirectappInstalled;
    private final String reelAutoArchive;
    private final String allowedCommenterType;
    private final long mediaCount;
    private final long followerCount;
    private final long followingCount;
    private final long followingTagCount;
    private final String biography;
    private final String externalUrl;
    private final long usertagsCount;
    private final String publicEmail;
    private final HdProfilePicUrlInfo hdProfilePicUrlInfo;
    private final String profileContext;
    private final List<UserProfileContextLink> profileContextLinksWithUserIds;

    public User(final long pk,
                final String username,
                final String fullName,
                final boolean isPrivate,
                final String profilePicUrl,
                final String profilePicId,
                final FriendshipStatus friendshipStatus,
                final boolean isVerified,
                final boolean hasAnonymousProfilePicture,
                final boolean isUnpublished,
                final boolean isFavorite,
                final boolean isDirectappInstalled,
                final String reelAutoArchive,
                final String allowedCommenterType,
                final long mediaCount,
                final long followerCount,
                final long followingCount,
                final long followingTagCount,
                final String biography,
                final String externalUrl,
                final long usertagsCount,
                final String publicEmail,
                final HdProfilePicUrlInfo hdProfilePicUrlInfo,
                final String profileContext,
                final List<UserProfileContextLink> profileContextLinksWithUserIds) {
        this.pk = pk;
        this.username = username;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
        this.profilePicUrl = profilePicUrl;
        this.profilePicId = profilePicId;
        this.friendshipStatus = friendshipStatus;
        this.isVerified = isVerified;
        this.hasAnonymousProfilePicture = hasAnonymousProfilePicture;
        this.isUnpublished = isUnpublished;
        this.isFavorite = isFavorite;
        this.isDirectappInstalled = isDirectappInstalled;
        this.reelAutoArchive = reelAutoArchive;
        this.allowedCommenterType = allowedCommenterType;
        this.mediaCount = mediaCount;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.followingTagCount = followingTagCount;
        this.biography = biography;
        this.externalUrl = externalUrl;
        this.usertagsCount = usertagsCount;
        this.publicEmail = publicEmail;
        this.hdProfilePicUrlInfo = hdProfilePicUrlInfo;
        this.profileContext = profileContext;
        this.profileContextLinksWithUserIds = profileContextLinksWithUserIds;
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

    public String getHDProfilePicUrl() {
        return hdProfilePicUrlInfo.getUrl();
    }

    public String getProfilePicId() {
        return profilePicId;
    }

    public FriendshipStatus getFriendshipStatus() {
        return friendshipStatus;
    }

    public void setFriendshipStatus(final FriendshipStatus friendshipStatus) {
        this.friendshipStatus = friendshipStatus;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean hasAnonymousProfilePicture() {
        return hasAnonymousProfilePicture;
    }

    public boolean isUnpublished() {
        return isUnpublished;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public boolean isDirectappInstalled() {
        return isDirectappInstalled;
    }

    public String getReelAutoArchive() {
        return reelAutoArchive;
    }

    public String getAllowedCommenterType() {
        return allowedCommenterType;
    }

    public long getMediaCount() {
        return mediaCount;
    }

    public long getFollowerCount() {
        return followerCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }

    public long getFollowingTagCount() {
        return followingTagCount;
    }

    public String getBiography() {
        return biography;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public long getUsertagsCount() {
        return usertagsCount;
    }

    public String getPublicEmail() {
        return publicEmail;
    }

    public String getProfileContext() {
        return profileContext;
    }

    public List<UserProfileContextLink> getProfileContextLinks() {
        return profileContextLinksWithUserIds;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final User that = (User) o;
        return pk == that.pk &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, username);
    }
}
