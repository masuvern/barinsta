package awais.instagrabber.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;

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
import awais.instagrabber.databinding.DialogImportExportBinding;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;

public final class Utils {
    private static final String TAG = "Utils";
    private static final int VIDEO_CACHE_MAX_BYTES = 10 * 1024 * 1024;

    public static LogCollector logCollector;
    public static SettingsHelper settingsHelper;
    public static DataBox dataBox;
    public static boolean sessionVolumeFull = false;
    public static NotificationManagerCompat notificationManager;
    public static final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public static boolean isChannelCreated = false;
    public static String telegramPackage;
    public static ClipboardManager clipboardManager;
    public static DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    public static SimpleDateFormat datetimeParser;
    public static SimpleCache simpleCache;

    public static int convertDpToPx(final float dp) {
        if (displayMetrics == null)
            displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round((dp * displayMetrics.densityDpi) / 160.0f);
    }

    public static void changeTheme(final Context context) {
        int themeCode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // this is fallback / default

        if (settingsHelper != null) themeCode = settingsHelper.getThemeCode(false);

        if (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && Build.VERSION.SDK_INT < 29)
            themeCode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;

        boolean isAmoledEnabled = false;
        if (settingsHelper != null) {
            isAmoledEnabled = settingsHelper.getBoolean(Constants.AMOLED_THEME);
        }
        AppCompatDelegate.setDefaultNightMode(themeCode);
        // use amoled theme only if enabled in settings
        if (isAmoledEnabled && isNight(context, themeCode)) {
            // set amoled theme
            Log.d(TAG, "settings amoled theme");
            context.setTheme(R.style.Theme_Amoled);
        }
    }

    public static boolean isNight(final Context context, final int themeCode) {
        // check if setting is set to 'Dark'
        boolean isNight = themeCode == AppCompatDelegate.MODE_NIGHT_YES;
        // if not dark check if themeCode is MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_AUTO_BATTERY
        if (!isNight && (themeCode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || themeCode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)) {
            // check if resulting theme would be NIGHT
            final int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isNight = uiMode == Configuration.UI_MODE_NIGHT_YES;
        }
        return isNight;
    }

    public static void setTooltipText(final View view, @StringRes final int tooltipTextRes) {
        if (view != null && tooltipTextRes != 0 && tooltipTextRes != -1) {
            final Context context = view.getContext();
            final String tooltipText = context.getResources().getString(tooltipTextRes);

            if (Build.VERSION.SDK_INT >= 26) view.setTooltipText(tooltipText);
            else view.setOnLongClickListener(v -> {
                Toast.makeText(context, tooltipText, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    public static void copyText(final Context context, final CharSequence string) {
        final boolean ctxNotNull = context != null;
        if (ctxNotNull && clipboardManager == null)
            clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        int toastMessage = R.string.clipboard_error;
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(Constants.CHANNEL_NAME, string));
            toastMessage = R.string.clipboard_copied;
        }
        if (ctxNotNull) Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    public static void showImportExportDialog(final Context context) {
        final DialogImportExportBinding importExportBinding = DialogImportExportBinding.inflate(LayoutInflater.from(context));

        final View passwordParent = (View) importExportBinding.cbPassword.getParent();
        final View exportLoginsParent = (View) importExportBinding.cbExportLogins.getParent();
        final View exportFavoritesParent = (View) importExportBinding.cbExportFavorites.getParent();
        final View exportSettingsParent = (View) importExportBinding.cbExportSettings.getParent();
        final View importLoginsParent = (View) importExportBinding.cbImportLogins.getParent();
        final View importFavoritesParent = (View) importExportBinding.cbImportFavorites.getParent();
        final View importSettingsParent = (View) importExportBinding.cbImportSettings.getParent();

        importExportBinding.cbPassword.setOnCheckedChangeListener((buttonView, isChecked) ->
                                                                          importExportBinding.etPassword.etPassword.setEnabled(isChecked));

        final AlertDialog[] dialog = new AlertDialog[1];
        final View.OnClickListener onClickListener = v -> {
            if (v == passwordParent) importExportBinding.cbPassword.performClick();

            else if (v == exportLoginsParent) importExportBinding.cbExportLogins.performClick();
            else if (v == exportFavoritesParent)
                importExportBinding.cbExportFavorites.performClick();

            else if (v == importLoginsParent) importExportBinding.cbImportLogins.performClick();
            else if (v == importFavoritesParent)
                importExportBinding.cbImportFavorites.performClick();

            else if (v == exportSettingsParent) importExportBinding.cbExportSettings.performClick();
            else if (v == importSettingsParent) importExportBinding.cbImportSettings.performClick();

            else if (context instanceof AppCompatActivity) {
                final FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
                final String folderPath = settingsHelper.getString(FOLDER_PATH);

                if (v == importExportBinding.btnSaveTo) {
                    final Editable text = importExportBinding.etPassword.etPassword.getText();
                    final boolean passwordChecked = importExportBinding.cbPassword.isChecked();
                    if (passwordChecked && TextUtils.isEmpty(text))
                        Toast.makeText(context, R.string.dialog_export_err_password_empty, Toast.LENGTH_SHORT).show();
                    else {
                        new DirectoryChooser().setInitialDirectory(folderPath).setInteractionListener(path -> {
                            final File file = new File(path, "InstaGrabber_Settings_" + System.currentTimeMillis() + ".zaai");
                            final String password = passwordChecked ? text.toString() : null;
                            int flags = 0;
                            if (importExportBinding.cbExportFavorites.isChecked())
                                flags |= ExportImportUtils.FLAG_FAVORITES;
                            if (importExportBinding.cbExportSettings.isChecked())
                                flags |= ExportImportUtils.FLAG_SETTINGS;
                            if (importExportBinding.cbExportLogins.isChecked())
                                flags |= ExportImportUtils.FLAG_COOKIES;

                            ExportImportUtils.Export(password, flags, file, result -> {
                                Toast.makeText(context, result ? R.string.dialog_export_success : R.string.dialog_export_failed, Toast.LENGTH_SHORT)
                                     .show();
                                if (dialog[0] != null && dialog[0].isShowing()) dialog[0].dismiss();
                            });

                        }).show(fragmentManager, null);
                    }

                } else if (v == importExportBinding.btnImport) {
                    new DirectoryChooser().setInitialDirectory(folderPath).setShowZaAiConfigFiles(true).setInteractionListener(path -> {
                        int flags = 0;
                        if (importExportBinding.cbImportFavorites.isChecked())
                            flags |= ExportImportUtils.FLAG_FAVORITES;
                        if (importExportBinding.cbImportSettings.isChecked())
                            flags |= ExportImportUtils.FLAG_SETTINGS;
                        if (importExportBinding.cbImportLogins.isChecked())
                            flags |= ExportImportUtils.FLAG_COOKIES;

                        ExportImportUtils.Import(context, flags, new File(path), result -> {
                            ((AppCompatActivity) context).recreate();
                            Toast.makeText(context, result ? R.string.dialog_import_success : R.string.dialog_import_failed, Toast.LENGTH_SHORT)
                                 .show();
                            if (dialog[0] != null && dialog[0].isShowing()) dialog[0].dismiss();
                        });

                    }).show(fragmentManager, null);
                }
            }
        };

        passwordParent.setOnClickListener(onClickListener);
        exportLoginsParent.setOnClickListener(onClickListener);
        exportSettingsParent.setOnClickListener(onClickListener);
        exportFavoritesParent.setOnClickListener(onClickListener);
        importLoginsParent.setOnClickListener(onClickListener);
        importSettingsParent.setOnClickListener(onClickListener);
        importFavoritesParent.setOnClickListener(onClickListener);
        importExportBinding.btnSaveTo.setOnClickListener(onClickListener);
        importExportBinding.btnImport.setOnClickListener(onClickListener);

        dialog[0] = new AlertDialog.Builder(context).setView(importExportBinding.getRoot()).show();
    }

    public static Map<String, String> sign(final Map<String, Object> form) {
        final String signed = sign(new JSONObject(form).toString());
        if (signed == null) {
            return null;
        }
        final String[] parts = signed.split("&");
        final Map<String, String> map = new HashMap<>();
        for (final String part : parts) {
            final String[] partSplit = part.split("=");
            map.put(partSplit[0], partSplit[1]);
        }
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

    @Nullable
    public static String getInstalledTelegramPackage(@NonNull final Context context) {
        final String[] packages = {
                "org.telegram.messenger",
                "org.thunderdog.challegram",
                "ir.ilmili.telegraph",
                // "org.telegram.BifToGram", see GitHub issue 124
                "org.vidogram.messenger",
                "com.xplus.messenger",
                "com.ellipi.messenger",
                "org.telegram.plus",
                "com.iMe.android",
                "org.viento.colibri",
                "org.viento.colibrix",
                "ml.parsgram",
                "com.ringtoon.app.tl",
        };

        final PackageManager packageManager = context.getPackageManager();
        for (final String pkg : packages) {
            try {
                final PackageInfo packageInfo = packageManager.getPackageInfo(pkg, 0);
                if (packageInfo.applicationInfo.enabled) return pkg;
            } catch (final Exception e) {
                try {
                    if (packageManager.getApplicationInfo(pkg, 0).enabled) return pkg;
                } catch (final Exception e1) {
                    // meh
                }
            }
        }

        return null;
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
}
