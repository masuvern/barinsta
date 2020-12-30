package awais.instagrabber.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Browser;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import awais.instagrabber.R;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.FavoriteType;
import awaisomereport.LogCollector;

public final class Utils {
    private static final String TAG = "Utils";
    private static final int VIDEO_CACHE_MAX_BYTES = 10 * 1024 * 1024;

    public static LogCollector logCollector;
    public static SettingsHelper settingsHelper;
    public static boolean sessionVolumeFull = false;
    public static final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public static ClipboardManager clipboardManager;
    public static DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    public static SimpleDateFormat datetimeParser;
    public static SimpleCache simpleCache;
    private static int statusBarHeight;
    private static int actionBarHeight;

    public static int convertDpToPx(final float dp) {
        if (displayMetrics == null)
            displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round((dp * displayMetrics.densityDpi) / 160.0f);
    }

    public static void copyText(@NonNull final Context context, final CharSequence string) {
        if (clipboardManager == null)
            clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        int toastMessage = R.string.clipboard_error;
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), string));
            toastMessage = R.string.clipboard_copied;
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    public static Map<String, String> sign(final Map<String, Object> form) {
        final String signed = sign(new JSONObject(form).toString());
        if (signed == null) {
            return null;
        }
        final Map<String, String> map = new HashMap<>();
        map.put("ig_sig_key_version", Constants.SIGNATURE_VERSION);
        map.put("signed_body", signed.split("&signed_body=")[1]);
        return map;
    }

    public static String sign(final String message) {
        return sign(Constants.SIGNATURE_KEY, message);
    }

    public static String sign(final String key, final String message) {
        try {
            final Mac hasher = Mac.getInstance("HmacSHA256");
            hasher.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
            byte[] hash = hasher.doFinal(message.getBytes());
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "ig_sig_key_version=" + Constants.SIGNATURE_VERSION + "&signed_body=" + hexString.toString() + "." + message;
        } catch (Exception e) {
            Log.e(TAG, "Error signing", e);
            return null;
        }
    }

    public static boolean isImage(final Uri itemUri, final ContentResolver contentResolver) {
        String mimeType;
        if (itemUri == null) return false;
        final String scheme = itemUri.getScheme();
        if (TextUtils.isEmpty(scheme))
            mimeType = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(itemUri.toString()).toLowerCase());
        else
            mimeType = scheme.equals(ContentResolver.SCHEME_CONTENT) ? contentResolver.getType(itemUri)
                                                                     : mimeTypeMap.getMimeTypeFromExtension
                                                                             (MimeTypeMap.getFileExtensionFromUrl(itemUri.toString()).toLowerCase());

        if (TextUtils.isEmpty(mimeType)) return true;
        mimeType = mimeType.toLowerCase();
        return mimeType.startsWith("image");
    }

    public static void errorFinish(@NonNull final Activity activity) {
        Toast.makeText(activity, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    public static SimpleCache getSimpleCacheInstance(final Context context) {
        if (context == null) {
            return null;
        }
        final ExoDatabaseProvider exoDatabaseProvider = new ExoDatabaseProvider(context);
        final File cacheDir = context.getCacheDir();
        if (simpleCache == null && cacheDir != null) {
            simpleCache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_MAX_BYTES), exoDatabaseProvider);
        }
        return simpleCache;
    }

    @Nullable
    public static Pair<FavoriteType, String> migrateOldFavQuery(final String queryText) {
        if (queryText.startsWith("@")) {
            return new Pair<>(FavoriteType.USER, queryText.substring(1));
        } else if (queryText.contains("/")) {
            return new Pair<>(FavoriteType.LOCATION, queryText.substring(0, queryText.indexOf("/")));
        } else if (queryText.startsWith("#")) {
            return new Pair<>(FavoriteType.HASHTAG, queryText.substring(1));
        }
        return null;
    }

    public static int getStatusBarHeight(final Context context) {
        if (statusBarHeight > 0) {
            return statusBarHeight;
        }
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static int getActionBarHeight(@NonNull final Context context) {
        if (actionBarHeight > 0) {
            return actionBarHeight;
        }
        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, displayMetrics);
        }
        return actionBarHeight;
    }

    public static void openURL(final Context context, final String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }
        final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        i.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true);
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "openURL: No activity found to handle URLs", e);
            Toast.makeText(context, context.getString(R.string.no_external_app_url), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "openURL", e);
        }
    }

    public static void openEmailAddress(final Context context, final String emailAddress) {
        if (context == null || TextUtils.isEmpty(emailAddress)) {
            return;
        }
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAddress));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(emailIntent);
    }

    public static void displayToastAboveView(@NonNull final Context context,
                                             @NonNull final View view,
                                             @NonNull final String text) {
        final Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.START,
                         view.getLeft(),
                         view.getTop() - view.getHeight() - 4);
        toast.show();
    }

    public static PostsLayoutPreferences getPostsLayoutPreferences(final String layoutPreferenceKey) {
        PostsLayoutPreferences layoutPreferences = PostsLayoutPreferences.fromJson(settingsHelper.getString(layoutPreferenceKey));
        if (layoutPreferences == null) {
            layoutPreferences = PostsLayoutPreferences.builder().build();
            settingsHelper.putString(layoutPreferenceKey, layoutPreferences.getJson());
        }
        return layoutPreferences;
    }
}
