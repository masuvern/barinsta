package awais.instagrabber.db.entities;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

import awais.instagrabber.models.enums.FavoriteType;

@Entity(tableName = "favorites")
public class Favorite {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private final int id;

    @ColumnInfo(name = "query_text")
    private final String query;

    @ColumnInfo(name = "type")
    private final FavoriteType type;

    @ColumnInfo(name = "display_name")
    private final String displayName;

    @ColumnInfo(name = "pic_url")
    private final String picUrl;

    @ColumnInfo(name = "date_added")
    private final Date dateAdded;

    public Favorite(final int id,
                    final String query,
                    final FavoriteType type,
                    final String displayName,
                    final String picUrl,
                    final Date dateAdded) {
        this.id = id;
        this.query = query;
        this.type = type;
        this.displayName = displayName;
        this.picUrl = picUrl;
        this.dateAdded = dateAdded;
    }

    public int getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public FavoriteType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Favorite that = (Favorite) o;
        return id == that.id &&
                ObjectsCompat.equals(query, that.query) &&
                type == that.type &&
                ObjectsCompat.equals(displayName, that.displayName) &&
                ObjectsCompat.equals(picUrl, that.picUrl) &&
                ObjectsCompat.equals(dateAdded, that.dateAdded);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(id, query, type, displayName, picUrl, dateAdded);
    }

    @NonNull
    @Override
    public String toString() {
        return "FavoriteModel{" +
                "id=" + id +
                ", query='" + query + '\'' +
                ", type=" + type +
                ", displayName='" + displayName + '\'' +
                ", picUrl='" + picUrl + '\'' +
                ", dateAdded=" + dateAdded +
                '}';
    }
}
