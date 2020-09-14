package awais.instagrabber.repositories.responses;

public class UserInfo {
    private final String pk;
    private final String username;
    private final String fullName;
    private final String profilePicUrl;

    public UserInfo(final String pk, final String username, final String fullName, final String profilePicUrl) {
        this.pk = pk;
        this.username = username;
        this.fullName = fullName;
        this.profilePicUrl = profilePicUrl;
    }

    public String getPk() {
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

    @Override
    public String toString() {
        return "UserInfo{" +
                "uid='" + pk + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", profilePicUrl='" + profilePicUrl + '\'' +
                '}';
    }
}
