package awais.instagrabber.models;

import androidx.annotation.NonNull;

import awais.instagrabber.models.enums.SuggestionType;

public final class SuggestionModel implements Comparable<SuggestionModel> {
    private final int position;
    private final boolean isVerified;
    private final String username, name, profilePic;
    private final SuggestionType suggestionType;

    public SuggestionModel(final boolean isVerified, final String username, final String name, final String profilePic,
                           final SuggestionType suggestionType, final int position) {
        this.isVerified = isVerified;
        this.username = username;
        this.name = name;
        this.profilePic = profilePic;
        this.suggestionType = suggestionType;
        this.position = position;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public SuggestionType getSuggestionType() {
        return suggestionType;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public int compareTo(@NonNull final SuggestionModel model) {
        return Integer.compare(getPosition(), model.getPosition());
    }
}
