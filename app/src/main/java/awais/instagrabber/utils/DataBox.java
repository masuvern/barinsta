package awais.instagrabber.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;

import awais.instagrabber.BuildConfig;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class DataBox extends SQLiteOpenHelper {
    private static final String TAG = "DataBox";

    private static DataBox sInstance;

    private final static int VERSION = 2;
    private final static String TABLE_COOKIES = "cookies";
    private final static String TABLE_FAVORITES = "favorites";
    private final static String KEY_DATE_ADDED = "date_added";
    private final static String KEY_QUERY_TEXT = "query_text";
    private final static String KEY_QUERY_DISPLAY = "query_display";

    private final static String KEY_ID = "id";
    private final static String KEY_USERNAME = Constants.EXTRAS_USERNAME;
    private final static String KEY_COOKIE = "cookie";
    private final static String KEY_UID = "uid";
    private final static String KEY_FULL_NAME = "full_name";
    private final static String KEY_PROFILE_PIC = "profile_pic";

    private final Context c;

    public static synchronized DataBox getInstance(final Context context) {
        if (sInstance == null) sInstance = new DataBox(context.getApplicationContext());
        return sInstance;
    }

    private DataBox(@Nullable final Context context) {
        super(context, "cookiebox.db", null, VERSION);
        c = context;
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
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY, query_text TEXT, date_added INTEGER, query_display TEXT)");
        Log.i(TAG, "Tables created!");
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Log.i(TAG, String.format("Updating DB from v%d to v%d", oldVersion, newVersion));
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE " + TABLE_COOKIES + " ADD " + KEY_FULL_NAME + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_COOKIES + " ADD " + KEY_PROFILE_PIC + " TEXT");
        }
        Log.i(TAG, String.format("DB update from v%d to v%d completed!", oldVersion, newVersion));
    }

    public final void addFavorite(@NonNull final FavoriteModel favoriteModel) {
        final String query = favoriteModel.getQuery();
        final String display = favoriteModel.getDisplayName();
        if (!Utils.isEmpty(query)) {
            try (final SQLiteDatabase db = getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final ContentValues values = new ContentValues();
                    values.put(KEY_DATE_ADDED, favoriteModel.getDate());
                    values.put(KEY_QUERY_TEXT, query);
                    values.put(KEY_QUERY_DISPLAY, display);

                    final int rows = db.update(TABLE_FAVORITES, values, KEY_QUERY_TEXT + "=?", new String[]{query});

                    if (rows != 1)
                        db.insertOrThrow(TABLE_FAVORITES, null, values);

                    db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (logCollector != null)
                        logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "addFavorite");
                    if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    public final synchronized void delFavorite(@NonNull final FavoriteModel favoriteModel) {
        final String query = favoriteModel.getQuery();
        if (!Utils.isEmpty(query)) {
            try (final SQLiteDatabase db = getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final int rowsDeleted = db.delete(TABLE_FAVORITES, "query_text=? AND date_added=?",
                                                      new String[]{query, Long.toString(favoriteModel.getDate())});

                    final int rowsDeletedTwo = db.delete(TABLE_FAVORITES, "query_text=? AND date_added=?",
                                                         new String[]{query.replaceAll("@", ""), Long.toString(favoriteModel.getDate())});

                    if (rowsDeleted > 0 || rowsDeletedTwo > 0) db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (logCollector != null)
                        logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "delFavorite");
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error", e);
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    @Nullable
    public final ArrayList<FavoriteModel> getAllFavorites() {
        ArrayList<FavoriteModel> favorites = null;
        FavoriteModel tempFav;
        final SQLiteDatabase db = getWritableDatabase();

        try (final Cursor cursor = db.rawQuery("SELECT query_text, date_added, query_display FROM favorites ORDER BY date_added DESC", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                db.beginTransaction();
                favorites = new ArrayList<>();
                do {
                    tempFav = new FavoriteModel(
                            (cursor.getString(0).charAt(0) == '@' || cursor.getString(0).charAt(0) == '#' || cursor.getString(0).contains("/"))
                            ? cursor.getString(0)
                            : "@" + cursor.getString(0), // query text
                            cursor.getLong(1),  // date added
                            cursor.getString(2) == null ? (cursor.getString(0).charAt(0) == '@' || cursor.getString(0).charAt(0) == '#' || cursor
                                    .getString(0).contains("/"))
                                                          ? cursor.getString(0)
                                                          : "@" + cursor.getString(0) : cursor.getString(2) // display
                    );
                    if (cursor.getString(2) == null) {
                        try {
                            final ContentValues values = new ContentValues();
                            values.put(KEY_DATE_ADDED, tempFav.getDate());
                            values.put(KEY_QUERY_TEXT, tempFav.getQuery());
                            values.put(KEY_QUERY_DISPLAY, tempFav.getDisplayName());

                            final int rows = db.update(TABLE_FAVORITES, values, KEY_QUERY_TEXT + "=?", new String[]{tempFav.getQuery()});

                            if (rows != 1)
                                db.insertOrThrow(TABLE_FAVORITES, null, values);
                        } catch (final Exception e) {
                            if (logCollector != null)
                                logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "delFavorite");
                            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                        }
                    }
                    favorites.add(tempFav);
                } while (cursor.moveToNext());
                db.endTransaction();
            }
        } catch (final Exception x) {
            Log.e("austin_debug", "", x);
            try {
                db.execSQL("ALTER TABLE favorites ADD query_display TEXT");
                Toast.makeText(c, "DB has migrated, launch quick access again.", Toast.LENGTH_SHORT).show();
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.DATA_BOX_FAVORITES, "migrate");
                Toast.makeText(c, "DB migration failed, contact maintainer.", Toast.LENGTH_SHORT).show();
                if (BuildConfig.DEBUG) Log.e(TAG, "", e);
            }
        }

        return favorites;
    }

    public final String getFavorite(@NonNull final String query) {
        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery("SELECT query_text, date_added FROM favorites WHERE "
                                                       + KEY_QUERY_TEXT + "='" + query + "' ORDER BY date_added DESC", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0) + "/" + String.valueOf(cursor.getLong(1));
            }
        }

        return null;
    }

    public final void addOrUpdateUser(final String uid,
                                      final String username,
                                      final String cookie,
                                      final String fullName,
                                      final String profilePicUrl) {
        if (Utils.isEmpty(uid)) return;
        try (final SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                final ContentValues values = new ContentValues();
                values.put(KEY_USERNAME, username);
                values.put(KEY_COOKIE, cookie);
                values.put(KEY_UID, uid);
                values.put(KEY_FULL_NAME, fullName);
                values.put(KEY_PROFILE_PIC, profilePicUrl);

                final int rows = db.update(TABLE_COOKIES, values, KEY_UID + "=?", new String[]{uid});

                if (rows != 1)
                    db.insertOrThrow(TABLE_COOKIES, null, values);

                db.setTransactionSuccessful();
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    public final synchronized void delUserCookie(@NonNull final CookieModel cookieModel) {
        final String cookieModelUid = cookieModel.getUid();
        if (!Utils.isEmpty(cookieModelUid)) {
            try (final SQLiteDatabase db = getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final int rowsDeleted = db.delete(TABLE_COOKIES, KEY_UID + "=? AND " + KEY_USERNAME + "=? AND " + KEY_COOKIE + "=?",
                                                      new String[]{cookieModelUid, cookieModel.getUsername(), cookieModel.getCookie()});

                    if (rowsDeleted > 0) db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    public final synchronized void deleteAllUserCookies() {
        try (final SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                final int rowsDeleted = db.delete(TABLE_COOKIES, null, null);

                if (rowsDeleted > 0) db.setTransactionSuccessful();
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    public final int getCookieCount() {
        int cookieCount = 0;
        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery("SELECT * FROM cookies", null)) {
            if (cursor != null) cookieCount = cursor.getCount();
        }
        return cookieCount;
    }

    @Nullable
    public final CookieModel getCookie(final String uid) {
        CookieModel cookie = null;
        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery(
                     "SELECT "
                             + KEY_UID + ","
                             + KEY_USERNAME + ","
                             + KEY_COOKIE + ","
                             + KEY_FULL_NAME + ","
                             + KEY_PROFILE_PIC
                             + " FROM " + TABLE_COOKIES
                             + " WHERE " + KEY_UID + " = ?",
                     new String[]{uid})
        ) {
            if (cursor != null && cursor.moveToFirst())
                cookie = new CookieModel(
                        cursor.getString(cursor.getColumnIndex(KEY_UID)),
                        cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),
                        cursor.getString(cursor.getColumnIndex(KEY_COOKIE)),
                        cursor.getString(cursor.getColumnIndex(KEY_FULL_NAME)),
                        cursor.getString(cursor.getColumnIndex(KEY_PROFILE_PIC))
                );
        }
        return cookie;
    }

    @Nullable
    public final ArrayList<CookieModel> getAllCookies() {
        ArrayList<CookieModel> cookies = null;

        try (final SQLiteDatabase db = getReadableDatabase();
             final Cursor cursor = db.rawQuery(
                     "SELECT "
                             + KEY_UID + ","
                             + KEY_USERNAME + ","
                             + KEY_COOKIE + ","
                             + KEY_FULL_NAME + ","
                             + KEY_PROFILE_PIC
                             + " FROM " + TABLE_COOKIES, null)
        ) {
            if (cursor != null && cursor.moveToFirst()) {
                cookies = new ArrayList<>();
                do {
                    cookies.add(new CookieModel(
                            cursor.getString(cursor.getColumnIndex(KEY_UID)),
                            cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),
                            cursor.getString(cursor.getColumnIndex(KEY_COOKIE)),
                            cursor.getString(cursor.getColumnIndex(KEY_FULL_NAME)),
                            cursor.getString(cursor.getColumnIndex(KEY_PROFILE_PIC))
                    ));
                } while (cursor.moveToNext());
            }
        }

        return cookies;
    }

    public static class CookieModel {
        private final String uid;
        private final String username;
        private final String cookie;
        private final String fullName;
        private final String profilePic;
        private boolean selected;

        public CookieModel(final String uid,
                           final String username,
                           final String cookie,
                           final String fullName,
                           final String profilePic) {
            this.uid = uid;
            this.username = username;
            this.cookie = cookie;
            this.fullName = fullName;
            this.profilePic = profilePic;
        }

        public String getUid() {
            return uid;
        }

        public String getUsername() {
            return username;
        }

        public String getCookie() {
            return cookie;
        }

        public String getFullName() {
            return fullName;
        }

        public String getProfilePic() {
            return profilePic;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(final boolean selected) {
            this.selected = selected;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CookieModel that = (CookieModel) o;
            return ObjectsCompat.equals(uid, that.uid) &&
                    ObjectsCompat.equals(username, that.username) &&
                    ObjectsCompat.equals(cookie, that.cookie);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(uid, username, cookie);
        }

        @NonNull
        @Override
        public String toString() {
            return username;
        }
    }

    public static class FavoriteModel {
        private final String query, displayName;
        private final long date;

        public FavoriteModel(final String query, final long date, final String displayName) {
            this.query = query;
            this.date = date;
            this.displayName = displayName;
        }

        public String getQuery() {
            return query;
        }

        public String getDisplayName() {
            return displayName;
        }

        public long getDate() {
            return date;
        }

        @NonNull
        @Override
        public String toString() {
            return query;
        }
    }
}