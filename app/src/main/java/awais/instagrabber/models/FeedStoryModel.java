package awais.instagrabber.models;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

import awais.instagrabber.utils.Utils;

public final class FeedStoryModel implements Serializable {
    private final String storyMediaId;
    private final ProfileModel profileModel;
    private final StoryModel firstStoryModel;
    private Boolean fullyRead;
    private final long timestamp;
    private final int mediaCount;

    public FeedStoryModel(final String storyMediaId, final ProfileModel profileModel, final boolean fullyRead,
                          final long timestamp, final StoryModel firstStoryModel, final int mediaCount) {
        this.storyMediaId = storyMediaId;
        this.profileModel = profileModel;
        this.fullyRead = fullyRead;
        this.timestamp = timestamp;
        this.firstStoryModel = firstStoryModel;
        this.mediaCount = mediaCount;
    }

    public String getStoryMediaId() {
        return storyMediaId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getDateTime() {
        return Utils.datetimeParser.format(new Date(timestamp * 1000L));
    }

    public int getMediaCount() {
        return mediaCount;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

//    public void setFirstStoryModel(final StoryModel firstStoryModel) {
//        this.firstStoryModel = firstStoryModel;
//    }

    public StoryModel getFirstStoryModel() {
        return firstStoryModel;
    }

    public Boolean isFullyRead() {
        return fullyRead;
    }

    public void setFullyRead(final boolean fullyRead) {
        this.fullyRead = fullyRead;
    }
}