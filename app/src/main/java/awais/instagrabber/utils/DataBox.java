package awais.instagrabber.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.models.enums.FavoriteType;

public final class DataBox extends SQLiteOpenHelper {
    private static final String TAG = "DataBox";

    private static DataBox sInstance;

    private final static int VERSION = 3;
    public final static String TABLE_COOKIES = "cookies";
    public final static String TABLE_FAVORITES = "favorites";

    public final static String KEY_ID = "id";
    public final static String KEY_USERNAME = Constants.EXTRAS_USERNAME;
    public final static String KEY_COOKIE = "cookie";
    public final static String KEY_UID = "uid";
    public final static String KEY_FULL_NAME = "full_name";
    public final static String KEY_PROFILE_PIC = "profile_pic";

    public final static String FAV_COL_ID = "id";
    public final static String FAV_COL_QUERY = "query_text";
    public final static String FAV_COL_TYPE = "type";
    public final static String FAV_COL_DISPLAY_NAME = "display_name";
    public final static String FAV_COL_PIC_URL = "pic_url";
    public final static String FAV_COL_DATE_ADDED = "date_added";

    public static synchronized DataBox getInstance(final Context context) {
        if (sInstance == null) sInstance = new DataBox(context.getApplicationContext());
        return sInstance;
    }

    private DataBox(@Nullable final Context context) {
        super(context, "cookiebox.db", null, VERSION);
    }

    @Override
    public void onCreate(@NonNull final SQLiteDatabase db) {
        Log.i(TAG, "Creating tables...");
        db.execSQL("CREATE TABLE " + TABLE_COOKIES + " ("
                           + KEY_ID + " INTEGER PRIMARY KEY,"
                           + KEY_UID + " TEXT,"
                           + KEY_USERNAME + " TEXT,"
                           + KEY_COOKIE + " TEXT,"
                           + KEY_FULL_NAME + " TEXT,"
                           + KEY_PROFILE_PIC + " TEXT)");
        // db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY, query_text TEXT, date_added INTEGER, query_display TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " ("
                           + FAV_COL_ID + " INTEGER PRIMARY KEY,"
                           + FAV_COL_QUERY + " TEXT,"
                           + FAV_COL_TYPE + " TEXT,"
                           + FAV_COL_DISPLAY_NAME + " TEXT,"
                           + FAV_COL_PIC_URL + " TEXT,"
                           + FAV_COL_DATE_ADDED + " INTEGER)");
        Log.i(TAG, "Tables created!");
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Log.i(TAG, String.format("Updating DB from v%d to v%d", oldVersion, newVersion));
        // switch without break, so that all migrations from a previous version to new are run
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE " + TABLE_COOKIES + " ADD " + KEY_FULL_NAME + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_COOKIES + " ADD " + KEY_PROFILE_PIC + " TEXT");
            case 2:
                final List<Favorite> oldFavorites = backupOldFavorites(db);
                // recreate with new columns (as there will be no doubt about the `query_display` column being present or not in the future versions)
                db.execSQL("DROP TABLE " + TABLE_FAVORITES);
                db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " ("
                                   + FAV_COL_ID + " INTEGER PRIMARY KEY,"
                                   + FAV_COL_QUERY + " TEXT,"
                                   + FAV_COL_TYPE + " TEXT,"
                                   + FAV_COL_DISPLAY_NAME + " TEXT,"
                                   + FAV_COL_PIC_URL + " TEXT,"
                                   + FAV_COL_DATE_ADDED + " INTEGER)");
                // add the old favorites back
                for (final Favorite oldFavorite : oldFavorites) {
                    insertOrUpdateFavorite(db, oldFavorite);
                }
        }
        Log.i(TAG, String.format("DB update from v%d to v%d completed!", oldVersion, newVersion));
    }

    public synchronized void insertOrUpdateFavorite(@NonNull final SQLiteDatabase db, @NonNull final Favorite model) {
        final ContentValues values = new ContentValues();
        values.put(FAV_COL_QUERY, model.getQuery());
        values.put(FAV_COL_TYPE, model.getType().toString());
        values.put(FAV_COL_DISPLAY_NAME, model.getDisplayName());
        values.put(FAV_COL_PIC_URL, model.getPicUrl());
        values.put(FAV_COL_DATE_ADDED, model.getDateAdded().getTime());
        int rows;
        if (model.getId() >= 1) {
            rows = db.update(TABLE_FAVORITES, values, FAV_COL_ID + "=?", new String[]{String.valueOf(model.getId())});
        } else {
            rows = db.update(TABLE_FAVORITES,
                             values,
                             FAV_COL_QUERY + "=?" +
                                     " AND " + FAV_COL_TYPE + "=?",
                             new String[]{model.getQuery(), model.getType().toString()});
        }
        if (rows != 1) {
            db.insert(TABLE_FAVORITES, null, values);
        }
    }

    @NonNull
    private List<Favorite> backupOldFavorites(@NonNull final SQLiteDatabase db) {
        // check if old favorites table had the column query_display
        final boolean queryDisplayExists = checkColumnExists(db, TABLE_FAVORITES, "query_display");
        Log.d(TAG, "backupOldFavorites: queryDisplayExists: " + queryDisplayExists);
        final List<Favorite> oldModels = new ArrayList<>();
        final String sql = "SELECT "
                + "query_text,"
                + "date_added"
                + (queryDisplayExists ? ",query_display" : "")
                + " FROM " + TABLE_FAVORITES;
        try (final Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        final String queryText = cursor.getString(cursor.getColumnIndex("query_text"));
                        final Pair<FavoriteType, String> favoriteTypeQueryPair = Utils.migrateOldFavQuery(queryText);
                        if (favoriteTypeQueryPair == null) continue;
                        final FavoriteType type = favoriteTypeQueryPair.first;
                        final String query = favoriteTypeQueryPair.second;
                        oldModels.add(new Favorite(
                                -1,
                                query,
                                type,
                                queryDisplayExists ? cursor.getString(cursor.getColumnIndex("query_display"))
                                                   : null,
                                null,
                                new Date(cursor.getLong(cursor.getColumnIndex("date_added")))
                        ));
                    } catch (Exception e) {
                        Log.e(TAG, "onUpgrade", e);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "onUpgrade", e);
        }
        Log.d(TAG, "backupOldFavorites: oldModels:" + oldModels);
        return oldModels;
    }

    public boolean checkColumnExists(@NonNull final SQLiteDatabase db,
                                     @NonNull final String tableName,
                                     @NonNull final String columnName) {
        boolean exists = false;
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            if (cursor.moveToFirst()) {
                do {
                    final String currentColumn = cursor.getString(cursor.getColumnIndex("name"));
                    if (currentColumn.equals(columnName)) {
                        exists = true;
                    }
                } while (cursor.moveToNext());

            }
        } catch (Exception ex) {
            Log.e(TAG, "checkColumnExists", ex);
        }
        return exists;
    }
}