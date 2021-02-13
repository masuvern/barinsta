package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

import awais.instagrabber.models.enums.RavenMediaViewMode;
import awais.instagrabber.repositories.responses.Media;

public class DirectItemVisualMedia {
    private final long urlExpireAtSecs;
    private final int playbackDurationSecs;
    private final List<Long> seenUserIds;
    private final RavenMediaViewMode viewMode;
    private final int seenCount;
    private final long replayExpiringAtUs;
    private final RavenExpiringMediaActionSummary expiringMediaActionSummary;
    private final Media media;

    public DirectItemVisualMedia(final long urlExpireAtSecs,
                                 final int playbackDurationSecs,
                                 final List<Long> seenUserIds,
                                 final RavenMediaViewMode viewMode,
                                 final int seenCount,
                                 final long replayExpiringAtUs,
                                 final RavenExpiringMediaActionSummary expiringMediaActionSummary,
                                 final Media media) {
        this.urlExpireAtSecs = urlExpireAtSecs;
        this.playbackDurationSecs = playbackDurationSecs;
        this.seenUserIds = seenUserIds;
        this.viewMode = viewMode;
        this.seenCount = seenCount;
        this.replayExpiringAtUs = replayExpiringAtUs;
        this.expiringMediaActionSummary = expiringMediaActionSummary;
        this.media = media;
    }

    public long getUrlExpireAtSecs() {
        return urlExpireAtSecs;
    }

    public int getPlaybackDurationSecs() {
        return playbackDurationSecs;
    }

    public List<Long> getSeenUserIds() {
        return seenUserIds;
    }

    public RavenMediaViewMode getViewMode() {
        return viewMode;
    }

    public int getSeenCount() {
        return seenCount;
    }

    public long getReplayExpiringAtUs() {
        return replayExpiringAtUs;
    }

    public RavenExpiringMediaActionSummary getExpiringMediaActionSummary() {
        return expiringMediaActionSummary;
    }

    public Media getMedia() {
        return media;
    }
}
