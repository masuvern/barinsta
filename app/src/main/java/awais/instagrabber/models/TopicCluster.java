package awais.instagrabber.models;

import java.io.Serializable;

public class TopicCluster implements Serializable {
    private String id;
    private String title;
    private String type;
    private boolean canMute;
    private boolean isMuted;
    private int rankedPosition;
    private FeedModel coverMedia;

    public TopicCluster(final String id,
                        final String title,
                        final String type,
                        final boolean canMute,
                        final boolean isMuted,
                        final int rankedPosition,
                        final FeedModel coverMedia) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.canMute = canMute;
        this.isMuted = isMuted;
        this.rankedPosition = rankedPosition;
        this.coverMedia = coverMedia;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public boolean isCanMute() {
        return canMute;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public int getRankedPosition() {
        return rankedPosition;
    }

    public FeedModel getCoverMedia() {
        return coverMedia;
    }
}
