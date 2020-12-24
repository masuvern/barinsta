package awais.instagrabber.models;

import android.util.Log;

import java.io.Serializable;

public final class FeedStoryModel implements Serializable {
    private final String storyMediaId;
    private final ProfileModel profileModel;
    private StoryModel[] storyModels;
    private Boolean fullyRead;

    public FeedStoryModel(final String storyMediaId, final ProfileModel profileModel, final boolean fullyRead) {
        this.storyMediaId = storyMediaId;
        this.profileModel = profileModel;
        this.fullyRead = fullyRead;
    }

    public String getStoryMediaId() {
        return storyMediaId;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public void setStoryModels(final StoryModel[] storyModels) {
        this.storyModels = storyModels;
    }

    public StoryModel[] getStoryModels() {
        return storyModels;
    }

    public Boolean isFullyRead() {
        return fullyRead;
    }

    public void setFullyRead(final boolean fullyRead) {
        this.fullyRead = fullyRead;
    }
}