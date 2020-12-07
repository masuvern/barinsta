package awais.instagrabber.db.entities;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import awais.instagrabber.utils.TextUtils;

@Entity(tableName = "cookies")
public class Account {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private final int id;

    @ColumnInfo(name = "uid")
    private final String uid;

    @ColumnInfo(name = "username")
    private final String username;

    @ColumnInfo(name = "cookie")
    private final String cookie;

    @ColumnInfo(name = "full_name")
    private final String fullName;

    @ColumnInfo(name = "profile_pic")
    private final String profilePic;

    private boolean selected;

    public Account(final int id,
                   final String uid,
                   final String username,
                   final String cookie,
                   final String fullName,
                   final String profilePic) {
        this.id = id;
        this.uid = uid;
        this.username = username;
        this.cookie = cookie;
        this.fullName = fullName;
        this.profilePic = profilePic;
    }

    public int getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getCookie() {
        return cookie;
    }

    public String getFullName() {
        return fullName;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(uid)
                && !TextUtils.isEmpty(username)
                && !TextUtils.isEmpty(cookie);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Account that = (Account) o;
        return ObjectsCompat.equals(uid, that.uid) &&
                ObjectsCompat.equals(username, that.username) &&
                ObjectsCompat.equals(cookie, that.cookie);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(uid, username, cookie);
    }

    @NonNull
    @Override
    public String toString() {
        return "Account{" +
                "uid='" + uid + '\'' +
                ", username='" + username + '\'' +
                ", cookie='" + cookie + '\'' +
                ", fullName='" + fullName + '\'' +
                ", profilePic='" + profilePic + '\'' +
                ", selected=" + selected +
                '}';
    }
}
