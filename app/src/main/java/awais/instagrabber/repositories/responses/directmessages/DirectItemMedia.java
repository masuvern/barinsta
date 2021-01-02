package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

import awais.instagrabber.models.enums.MediaItemType;

public class DirectItemMedia {
    private final String pk;
    private final String id;
    private final String code;
    private final DirectUser user;
    private final ImageVersions2 imageVersions2;
    private final int originalWidth;
    private final int originalHeight;
    private final MediaItemType mediaType;
    private final boolean isReelMedia;
    private final List<VideoVersion> videoVersions;
    private final boolean hasAudio;
    private final Caption caption;
    private final Audio audio;
    private final String title;
    private final List<DirectItemMedia> carouselMedia;

    public DirectItemMedia(final String pk,
                           final String id,
                           final String code,
                           final DirectUser user,
                           final ImageVersions2 imageVersions2,
                           final int originalWidth,
                           final int originalHeight,
                           final MediaItemType mediaType,
                           final boolean isReelMedia,
                           final List<VideoVersion> videoVersions,
                           final boolean hasAudio,
                           final Caption caption,
                           final Audio audio,
                           final String title,
                           final List<DirectItemMedia> carouselMedia) {
        this.pk = pk;
        this.id = id;
        this.code = code;
        this.user = user;
        this.imageVersions2 = imageVersions2;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.mediaType = mediaType;
        this.isReelMedia = isReelMedia;
        this.videoVersions = videoVersions;
        this.hasAudio = hasAudio;
        this.caption = caption;
        this.audio = audio;
        this.title = title;
        this.carouselMedia = carouselMedia;
    }

    public String getPk() {
        return pk;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public DirectUser getUser() {
        return user;
    }

    public ImageVersions2 getImageVersions2() {
        return imageVersions2;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public MediaItemType getMediaType() {
        return mediaType;
    }

    public boolean isReelMedia() {
        return isReelMedia;
    }

    public List<VideoVersion> getVideoVersions() {
        return videoVersions;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }

    public Caption getCaption() {
        return caption;
    }

    public Audio getAudio() {
        return audio;
    }

    public String getTitle() {
        return title;
    }

    public List<DirectItemMedia> getCarouselMedia() {
        return carouselMedia;
    }
}
