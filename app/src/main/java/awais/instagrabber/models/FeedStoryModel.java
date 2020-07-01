package awais.instagrabber.models;

import java.io.Serializable;

public final class FeedStoryModel implements Serializable {
    private final String storyMediaId;
    private final ProfileModel profileModel;
    private StoryModel[] storyModels;

    public FeedStoryModel(final String storyMediaId, final ProfileModel profileModel) {
        this.storyMediaId = storyMediaId;
        this.profileModel = profileModel;
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
}