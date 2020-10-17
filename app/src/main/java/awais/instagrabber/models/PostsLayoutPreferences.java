package awais.instagrabber.models;

import com.google.gson.Gson;

import java.util.Objects;

public final class PostsLayoutPreferences {
    private final PostsLayoutType type;
    private final int colCount;
    private final boolean isAvatarVisible;
    private final boolean isNameVisible;
    private final ProfilePicSize profilePicSize;
    private final boolean hasRoundedCorners;
    private final boolean hasGap;

    public static class Builder {
        private PostsLayoutType type = PostsLayoutType.GRID;
        private int colCount = 2;
        private boolean isAvatarVisible = false;
        private boolean isNameVisible = false;
        private ProfilePicSize profilePicSize = ProfilePicSize.REGULAR;
        private boolean hasRoundedCorners = true;
        private boolean hasGap = true;

        public Builder setType(final PostsLayoutType type) {
            this.type = type;
            return this;
        }

        public Builder setColCount(final int colCount) {
            this.colCount = (colCount <= 0 || colCount > 3) ? 1 : colCount;
            return this;
        }

        public Builder setAvatarVisible(final boolean avatarVisible) {
            this.isAvatarVisible = avatarVisible;
            return this;
        }

        public Builder setNameVisible(final boolean nameVisible) {
            this.isNameVisible = nameVisible;
            return this;
        }

        public Builder setProfilePicSize(final ProfilePicSize profilePicSize) {
            this.profilePicSize = profilePicSize;
            return this;
        }

        public Builder setHasRoundedCorners(final boolean hasRoundedCorners) {
            this.hasRoundedCorners = hasRoundedCorners;
            return this;
        }

        public Builder setHasGap(final boolean hasGap) {
            this.hasGap = hasGap;
            return this;
        }

        // Breaking builder pattern and adding getters to avoid too many object creations in PostsLayoutPreferencesDialogFragment
        public PostsLayoutType getType() {
            return type;
        }

        public int getColCount() {
            return colCount;
        }

        public boolean isAvatarVisible() {
            return isAvatarVisible;
        }

        public boolean isNameVisible() {
            return isNameVisible;
        }

        public ProfilePicSize getProfilePicSize() {
            return profilePicSize;
        }

        public boolean getHasRoundedCorners() {
            return hasRoundedCorners;
        }

        public boolean getHasGap() {
            return hasGap;
        }

        public Builder mergeFrom(final PostsLayoutPreferences preferences) {
            setColCount(preferences.getColCount());
            setAvatarVisible(preferences.isAvatarVisible());
            setNameVisible(preferences.isNameVisible());
            setType(preferences.getType());
            setProfilePicSize(preferences.getProfilePicSize());
            setHasRoundedCorners(preferences.getHasRoundedCorners());
            setHasGap(preferences.getHasGap());
            return this;
        }

        public PostsLayoutPreferences build() {
            return new PostsLayoutPreferences(type, colCount, isAvatarVisible, isNameVisible, profilePicSize, hasRoundedCorners, hasGap);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private PostsLayoutPreferences(final PostsLayoutType type,
                                   final int colCount,
                                   final boolean isAvatarVisible,
                                   final boolean isNameVisible,
                                   final ProfilePicSize profilePicSize,
                                   final boolean hasRoundedCorners,
                                   final boolean hasGap) {

        this.type = type;
        this.colCount = colCount;
        this.isAvatarVisible = isAvatarVisible;
        this.isNameVisible = isNameVisible;
        this.profilePicSize = profilePicSize;
        this.hasRoundedCorners = hasRoundedCorners;
        this.hasGap = hasGap;
    }

    public PostsLayoutType getType() {
        return type;
    }

    public int getColCount() {
        return colCount;
    }

    public boolean isAvatarVisible() {
        return isAvatarVisible;
    }

    public boolean isNameVisible() {
        return isNameVisible;
    }

    public ProfilePicSize getProfilePicSize() {
        return profilePicSize;
    }

    public boolean getHasRoundedCorners() {
        return hasRoundedCorners;
    }

    public boolean getHasGap() {
        return hasGap;
    }

    public String getJson() {
        return new Gson().toJson(this);
    }

    public static PostsLayoutPreferences fromJson(final String json) {
        if (json == null) return null;
        return new Gson().fromJson(json, PostsLayoutPreferences.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PostsLayoutPreferences that = (PostsLayoutPreferences) o;
        return colCount == that.colCount &&
                isAvatarVisible == that.isAvatarVisible &&
                isNameVisible == that.isNameVisible &&
                type == that.type &&
                profilePicSize == that.profilePicSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, colCount, isAvatarVisible, isNameVisible, profilePicSize);
    }

    @Override
    public String toString() {
        return "PostsLayoutPreferences{" +
                "type=" + type +
                ", colCount=" + colCount +
                ", isAvatarVisible=" + isAvatarVisible +
                ", isNameVisible=" + isNameVisible +
                ", profilePicSize=" + profilePicSize +
                ", hasRoundedCorners=" + hasRoundedCorners +
                ", hasGap=" + hasGap +
                '}';
    }

    public enum PostsLayoutType {
        GRID,
        STAGGERED_GRID,
        LINEAR
    }

    public enum ProfilePicSize {
        REGULAR,
        SMALL,
        TINY
    }
}
