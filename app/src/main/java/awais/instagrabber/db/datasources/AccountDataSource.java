package awais.instagrabber.db.datasources;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.db.entities.Account;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.utils.DataBox.KEY_COOKIE;
import static awais.instagrabber.utils.DataBox.KEY_FULL_NAME;
import static awais.instagrabber.utils.DataBox.KEY_ID;
import static awais.instagrabber.utils.DataBox.KEY_PROFILE_PIC;
import static awais.instagrabber.utils.DataBox.KEY_UID;
import static awais.instagrabber.utils.DataBox.KEY_USERNAME;
import static awais.instagrabber.utils.DataBox.TABLE_COOKIES;

public class AccountDataSource {
    private static final String TAG = AccountDataSource.class.getSimpleName();

    private static AccountDataSource INSTANCE;

    private final DataBox dataBox;

    private AccountDataSource(@NonNull Context context) {
        dataBox = DataBox.getInstance(context);
    }

    public static synchronized AccountDataSource getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AccountDataSource(context);
        }
        return INSTANCE;
    }

    @Nullable
    public final Account getAccount(final String uid) {
        Account cookie = null;
        try (final SQLiteDatabase db = dataBox.getReadableDatabase();
             final Cursor cursor = db.query(TABLE_COOKIES,
                                            new String[]{
                                                    KEY_ID,
                                                    KEY_UID,
                                                    KEY_USERNAME,
                                                    KEY_COOKIE,
                                                    KEY_FULL_NAME,
                                                    KEY_PROFILE_PIC
                                            },
                                            KEY_UID + "=?",
                                            new String[]{uid},
                                            null,
                                            null,
                                            null)) {
            if (cursor != null && cursor.moveToFirst())
                cookie = new Account(
                        cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                        cursor.getString(cursor.getColumnIndex(KEY_UID)),
                        cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),
                        cursor.getString(cursor.getColumnIndex(KEY_COOKIE)),
                        cursor.getString(cursor.getColumnIndex(KEY_FULL_NAME)),
                        cursor.getString(cursor.getColumnIndex(KEY_PROFILE_PIC))
                );
        }
        return cookie;
    }

    @NonNull
    public final List<Account> getAllAccounts() {
        final List<Account> cookies = new ArrayList<>();
        try (final SQLiteDatabase db = dataBox.getReadableDatabase();
             final Cursor cursor = db.query(TABLE_COOKIES,
                                            new String[]{
                                                    KEY_ID,
                                                    KEY_UID,
                                                    KEY_USERNAME,
                                                    KEY_COOKIE,
                                                    KEY_FULL_NAME,
                                                    KEY_PROFILE_PIC
                                            },
                                            null,
                                            null,
                                            null,
                                            null,
                                            null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    cookies.add(new Account(
                            cursor.getInt(cursor.getColumnIndex(KEY_ID)),
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

    // public final void insertOrUpdateAccount(@NonNull final Account account) {
    //     insertOrUpdateAccount(
    //             account.getUid(),
    //             account.getUsername(),
    //             account.getCookie(),
    //             account.getFullName(),
    //             account.getProfilePic()
    //     );
    // }

    public final void insertOrUpdateAccount(final String uid,
                                            final String username,
                                            final String cookie,
                                            final String fullName,
                                            final String profilePicUrl) {
        if (TextUtils.isEmpty(uid)) return;
        try (final SQLiteDatabase db = dataBox.getWritableDatabase()) {
            db.beginTransaction();
            try {
                final ContentValues values = new ContentValues();
                values.put(KEY_USERNAME, username);
                values.put(KEY_COOKIE, cookie);
                values.put(KEY_UID, uid);
                values.put(KEY_FULL_NAME, fullName);
                values.put(KEY_PROFILE_PIC, profilePicUrl);
                final int rows = db.update(TABLE_COOKIES, values, KEY_UID + "=?", new String[]{uid});
                if (rows != 1) {
                    db.insert(TABLE_COOKIES, null, values);
                }
                db.setTransactionSuccessful();
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    public final synchronized void deleteAccount(@NonNull final Account account) {
        final String cookieModelUid = account.getUid();
        if (!TextUtils.isEmpty(cookieModelUid)) {
            try (final SQLiteDatabase db = dataBox.getWritableDatabase()) {
                db.beginTransaction();
                try {
                    final int rowsDeleted = db.delete(TABLE_COOKIES, KEY_UID + "=? AND " + KEY_USERNAME + "=? AND " + KEY_COOKIE + "=?",
                                                      new String[]{cookieModelUid, account.getUsername(), account.getCookie()});

                    if (rowsDeleted > 0) db.setTransactionSuccessful();
                } catch (final Exception e) {
                    if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    public final synchronized void deleteAllAccounts() {
        try (final SQLiteDatabase db = dataBox.getWritableDatabase()) {
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
}
