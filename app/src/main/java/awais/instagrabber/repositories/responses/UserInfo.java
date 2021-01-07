package awais.instagrabber.repositories.responses;

public class UserInfo {
    private final long pk;
    private final String username, fullName, profilePicUrl, hdProfilePicUrl;

    public UserInfo(final long pk,
                    final String username,
                    final String fullName,
                    final String profilePicUrl,
                    final String hdProfilePicUrl) {
        this.pk = pk;
        this.username = username;
        this.fullName = fullName;
        this.profilePicUrl = profilePicUrl;
        this.hdProfilePicUrl = hdProfilePicUrl;
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

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public String getHDProfilePicUrl() {
        return hdProfilePicUrl;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "uid='" + pk + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", profilePicUrl='" + profilePicUrl + '\'' +
                ", hdProfilePicUrl='" + hdProfilePicUrl + '\'' +
                '}';
    }
}
