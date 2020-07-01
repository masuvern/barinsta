package awais.instagrabber.models.direct_messages;

import awais.instagrabber.models.enums.MediaItemType;

public final class InboxMediaModel {
    private final MediaItemType mediaType;
    private final String mediaId;
    private final String displayUrl;

    public InboxMediaModel(final MediaItemType mediaType, final String mediaId, final String displayUrl) {
        this.mediaType = mediaType;
        this.mediaId = mediaId;
        this.displayUrl = displayUrl;
    }

    public MediaItemType getMediaType() {
        return mediaType;
    }

    public String getMediaId() {
        return mediaId;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }
}