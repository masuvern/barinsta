package awais.instagrabber.db;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import awais.instagrabber.models.enums.FavoriteType;

public class Converters {
    @TypeConverter
    public static FavoriteType fromFavoriteTypeString(String value) {
        try {
            return FavoriteType.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    @TypeConverter
    public static String favoriteTypeToString(FavoriteType favoriteType) {
        return favoriteType == null ? null : favoriteType.toString();
    }

    @TypeConverter
    public static LocalDateTime fromTimestampToLocalDateTime(Long value) {
        if (value == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.systemDefault());
    }

    @TypeConverter
    public static Long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
