package awais.instagrabber.repositories.responses.saved;

import java.io.Serializable;
import java.util.List;

import awais.instagrabber.repositories.responses.Media;

public class SavedCollection implements Serializable {
    private final String collectionId;
    private final String collectionName;
    private final String collectionType;
    private final int collectionMediacount;
    private final List<Media> coverMediaList;

    public SavedCollection(final String collectionId,
                           final String collectionName,
                           final String collectionType,
                           final int collectionMediacount,
                           final List<Media> coverMediaList) {
        this.collectionId = collectionId;
        this.collectionName = collectionName;
        this.collectionType = collectionType;
        this.collectionMediacount = collectionMediacount;
        this.coverMediaList = coverMediaList;
    }

    public String getId() {
        return collectionId;
    }

    public String getTitle() {
        return collectionName;
    }

    public String getType() {
        return collectionType;
    }

    public int getMediaCount() {
        return collectionMediacount;
    }

    public List<Media> getCoverMedias() {
        return coverMediaList;
    }
}
