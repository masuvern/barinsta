package awais.instagrabber.db.datasources;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.TextUtils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.DataBox.FAV_COL_DATE_ADDED;
import static awais.instagrabber.utils.DataBox.FAV_COL_DISPLAY_NAME;
import static awais.instagrabber.utils.DataBox.FAV_COL_ID;
import static awais.instagrabber.utils.DataBox.FAV_COL_PIC_URL;
import static awais.instagrabber.utils.DataBox.FAV_COL_QUERY;
import static awais.instagrabber.utils.DataBox.FAV_COL_TYPE;
import static awais.instagrabber.utils.DataBox.TABLE_FAVORITES;
import static awais.instagrabber.utils.Utils.logCollector;

public class FavoriteDataSource {
    private static final String TAG = FavoriteDataSource.class.getSimpleName();

    private static FavoriteDataSource INSTANCE;

    private final DataBox dataBox;

    private FavoriteDataSource(@NonNull Context context) {
        dataBox = DataBox.getInstance(context);
    }

    public static synchronized FavoriteDataSource getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new FavoriteDataSource(context);
        }
        return INSTANCE;
    }

    @Nullable
    public final Favorite getFavorite(@NonNull final String query, @NonNull final FavoriteType type) {
        try (final SQLiteDatabase db = dataBox.getReadableDatabase();
             final Cursor cursor = db.query(TABLE_FAVORITES,
                                            new String[]{
                                                    FAV_COL_ID,
                                                    FAV_COL_QUERY,
                                                    FAV_COL_TYPE,
                                                    FAV_COL_DISPLAY_NAME,
                                                    FAV_COL_PIC_URL,
                                                    FAV_COL_DATE_ADDED
                                            },
                                            FAV_COL_QUERY + "=?" + " AND " + FAV_COL_TYPE + "=?",
                                            new String[]{
                                                    query,
                                                    type.toString()
                                            },
                                            null,
                                            null,
                                            null)) {
            if (cursor != null && cursor.moveToFirst()) {
                FavoriteType favoriteType = null;
                try {
                    favoriteType = FavoriteType.valueOf(cursor.getString(cursor.getColumnIndex(FAV_COL_TYPE)));
                } catch (IllegalArgumentException ignored) {}
                return new Favorite(
                        cursor.getInt(cursor.getColumnIndex(FAV_COL_ID)),
                        cursor.getString(cursor.getColumnIndex(FAV_COL_QUERY)),
                        favoriteType,
                        cursor.getString(cursor.getColumnIndex(FAV_COL_DISPLAY_NAME)),
                        cursor.getString(cursor.getColumnIndex(FAV_COL_PIC_URL)),
                        new Date(cursor.getLong(cursor.getColumnIndex(FAV_COL_DATE_ADDED)))
                );
            }
        }
        return null;
    }

    @NonNull
    public final List<Favorite> getAllFavorites() {
        final List<Favorite> favorites = new ArrayList<>();
        try (final SQLiteDatabase db = dataBox.getWritableDatabase()) {
            try (final Cursor cursor = db.query(TABLE_FAVORITES,
                                                new String[]{
                                                        FAV_COL_ID,
                                                        FAV_COL_QUERY,
                                                        FAV_COL_TYPE,
                                                        FAV_COL_DISPLAY_NAME,
                                                        FAV_COL_PIC_URL,
                                                        FAV_COL_DATE_ADDED
                                                },
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    db.beginTransaction();
                    Favorite tempFav;
                    do {
                        FavoriteType type = null;
                        try {
                            type = FavoriteType.valueOf(cursor.getString(cursor.getColumnIndex(FAV_COL_TYPE)));
                        } catch (IllegalArgumentException ignored) {}
                        tempFav = new Favorite(
                                cursor.getInt(cursor.getColumnIndex(FAV_COL_ID)),
                                cursor.getString(cursor.getColumnIndex(FAV_COL_QUERY)),
                                type,
                                cursor.getString(cursor.getColumnIndex(FAV_COL_DISPLAY_NAME)),
                                cursor.getString(cursor.getColumnIndex(FAV_COL_PIC_URL)),
                                new Date(cursor.getLong(cursor.getColumnIndex(FAV_COL_DATE_ADDED)))
                        );
                        favorites.add(tempFav);
                    } while (cursor.moveToNext());
                    db.endTransaction();
                }
            } catch (final Exception e) {
                Log.e(TAG, "", e);
            }
        }
        return favorites;
    }

    public final synchronized Favorite insertOrUpdateFavorite(@NonNull final Favorite model) {
        final String query = model.getQuery();
        if (!TextUtils.isEmpty(query)) {
            try (final SQLiteDatabase db = dataBox.getWritableDatabase()) {
                db.beginTransaction();
                try {
                    dataBox.insertOrUpdateFavorite(db, model);
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    if (logCollector != null) {
                        logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "insertOrUpdateFavorite");
                    }
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error adding/updating favorite", e);
                    }
                } finally {
                    db.endTransaction();
                }
            }
            return getFavorite(model.getQuery(), model.getType());
        }
        return null;
    }

    public final synchronized void deleteFavorite(@NonNull final String query, @NonNull final FavoriteType type) {
        if (!TextUtils.isEmpty(query)) {
            try (final SQLiteDatabase db = dataBox.getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final int rowsDeleted = db.delete(TABLE_FAVORITES,
                                                      FAV_COL_QUERY + "=?" +
                                                              " AND " + FAV_COL_TYPE + "=?",
                                                      new String[]{query, type.toString()});

                    if (rowsDeleted > 0) db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (logCollector != null) {
                        logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "deleteFavorite");
                    }
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error", e);
                    }
                } finally {
                    db.endTransaction();
                }
            }
        }
    }
}
