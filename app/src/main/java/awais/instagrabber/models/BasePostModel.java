package awais.instagrabber.models;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.io.Serializable;
import java.util.Date;

import awais.instagrabber.adapters.MultiSelectListAdapter.Selectable;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Utils;

public abstract class BasePostModel implements Serializable, Selectable {
    protected String postId;
    protected String displayUrl;
    protected String shortCode;
    protected CharSequence postCaption;
    protected MediaItemType itemType;
    protected boolean isSelected;
    protected boolean isDownloaded;
    protected long timestamp;
    protected int position;
    boolean liked;
    boolean saved;

    public boolean getLike() {
        return liked;
    }

    public boolean isSaved() {
        return saved;
    }

    public MediaItemType getItemType() {
        return itemType;
    }

    public final String getPostId() {
        return postId;
    }

    public final String getDisplayUrl() {
        return displayUrl;
    }

    public final CharSequence getPostCaption() {
        return postCaption;
    }

    public final String getShortCode() {
        return shortCode;
    }

    public final long getTimestamp() {
        return timestamp;
    }

    public int getPosition() {
        return this.position;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setItemType(final MediaItemType itemType) {
        this.itemType = itemType;
    }

    public void setPostId(final String postId) {
        this.postId = postId;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public void setSelected(final boolean selected) {
        this.isSelected = selected;
    }

    public void setDownloaded(final boolean downloaded) {
        isDownloaded = downloaded;
    }

    @NonNull
    public final String getPostDate() {
        return Utils.datetimeParser.format(new Date(timestamp * 1000L));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BasePostModel that = (BasePostModel) o;
        return ObjectsCompat.equals(postId, that.postId) && ObjectsCompat.equals(shortCode, that.shortCode);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(postId, shortCode);
    }
}