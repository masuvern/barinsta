package awais.instagrabber.utils;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.interfaces.FetchListener;
import awaisomereport.LogCollector.LogFile;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class ExportImportUtils {
    public static final int FLAG_COOKIES = 1;
    public static final int FLAG_FAVORITES = 1 << 1;
    public static final int FLAG_SETTINGS = 1 << 2;

    @IntDef(value = {FLAG_COOKIES, FLAG_FAVORITES, FLAG_SETTINGS}, flag = true)
    @interface ExportImportFlags {}

    public static void Export(@Nullable final String password, @ExportImportFlags final int flags, @NonNull final File filePath,
                              final FetchListener<Boolean> fetchListener) {
        final String exportString = ExportImportUtils.getExportString(flags);
        if (!Utils.isEmpty(exportString)) {
            final boolean isPass = !Utils.isEmpty(password);
            byte[] exportBytes = null;

            if (isPass) {
                final byte[] passwordBytes = password.getBytes();
                final byte[] bytes = new byte[32];
                System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));

                try {
                    exportBytes = PasswordUtils.enc(exportString, bytes);
                } catch (final Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (logCollector != null)
                        logCollector.appendException(e, LogFile.UTILS_EXPORT, "Export::isPass");
                    if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                }
            } else {
                exportBytes = Base64.encode(exportString.getBytes(), Base64.DEFAULT | Base64.NO_WRAP | Base64.NO_PADDING);
            }

            if (exportBytes != null && exportBytes.length > 1) {
                try (final FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(isPass ? 'A' : 'Z');
                    fos.write(exportBytes);
                    if (fetchListener != null) fetchListener.onResult(true);
                } catch (final Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (logCollector != null)
                        logCollector.appendException(e, LogFile.UTILS_EXPORT, "Export::notPass");
                    if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                }
            } else if (fetchListener != null) fetchListener.onResult(false);
        }
    }

    public static void Import(@NonNull final Context context, @ExportImportFlags final int flags, @NonNull final File filePath,
                              final FetchListener<Boolean> fetchListener) {
        try (final FileInputStream fis = new FileInputStream(filePath)) {
            final int configType = fis.read();

            final StringBuilder builder = new StringBuilder();
            int c;
            while ((c = fis.read()) != -1) {
                builder.append((char) c);
            }

            if (configType == 'A') {
                // password
                final AppCompatEditText editText = new AppCompatEditText(context);
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                new AlertDialog.Builder(context).setView(editText).setTitle(R.string.password).setPositiveButton(R.string.confirm, (dialog, which) -> {
                    final CharSequence text = editText.getText();
                    if (!Utils.isEmpty(text)) {
                        try {
                            final byte[] passwordBytes = text.toString().getBytes();
                            final byte[] bytes = new byte[32];
                            System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
                            saveToSettings(new String(PasswordUtils.dec(builder.toString(), bytes)), flags, fetchListener);
                        } catch (final Exception e) {
                            if (fetchListener != null) fetchListener.onResult(false);
                            if (logCollector != null)
                                logCollector.appendException(e, LogFile.UTILS_IMPORT, "Import::pass");
                            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                        }

                    } else
                        Toast.makeText(context, R.string.dialog_export_err_password_empty, Toast.LENGTH_SHORT).show();
                }).show();

            } else if (configType == 'Z') {
                saveToSettings(new String(Base64.decode(builder.toString(), Base64.DEFAULT | Base64.NO_PADDING | Base64.NO_WRAP)),
                        flags, fetchListener);

            } else {
                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                if (fetchListener != null) fetchListener.onResult(false);
            }
        } catch (final Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_IMPORT, "Import");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
    }

    private static void saveToSettings(final String json, @ExportImportFlags final int flags, final FetchListener<Boolean> fetchListener) {
        try {
            final JSONObject jsonObject = new JSONObject(json);

            if ((flags & FLAG_SETTINGS) == FLAG_SETTINGS && jsonObject.has("settings")) {
                final JSONObject objSettings = jsonObject.getJSONObject("settings");
                final Iterator<String> keys = objSettings.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    final Object val = objSettings.opt(key);
                    if (val instanceof String) {
                        settingsHelper.putString(key, (String) val);
                    } else if (val instanceof Integer) {
                        settingsHelper.putInteger(key, (int) val);
                    } else if (val instanceof Boolean) {
                        settingsHelper.putBoolean(key, (boolean) val);
                    }
                }
            }

            if ((flags & FLAG_COOKIES) == FLAG_COOKIES && jsonObject.has("cookies")) {
                final JSONArray cookies = jsonObject.getJSONArray("cookies");
                final int cookiesLen = cookies.length();
                for (int i = 0; i < cookiesLen; ++i) {
                    final JSONObject cookieObject = cookies.getJSONObject(i);
                    Utils.dataBox.addUserCookie(new DataBox.CookieModel(cookieObject.getString("i"),
                            cookieObject.getString("u"), cookieObject.getString("c")));
                }
            }

            if ((flags & FLAG_FAVORITES) == FLAG_FAVORITES && jsonObject.has("favs")) {
                final JSONArray favs = jsonObject.getJSONArray("favs");
                final int favsLen = favs.length();
                for (int i = 0; i < favsLen; ++i) {
                    final JSONObject favsObject = favs.getJSONObject(i);
                    Utils.dataBox.addFavorite(new DataBox.FavoriteModel(favsObject.getString("q"),
                            favsObject.getLong("d"), favsObject.has("s") ? favsObject.getString("s") : favsObject.getString("q")));
                }
            }

            if (fetchListener != null) fetchListener.onResult(true);

        } catch (final Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_IMPORT, "saveToSettings");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
    }

    @Nullable
    private static String getExportString(@ExportImportFlags final int flags) {
        String result = null;
        try {
            final JSONObject jsonObject = new JSONObject();

            String str;
            if ((flags & FLAG_SETTINGS) == FLAG_SETTINGS) {
                str = getSettings();
                if (str != null) jsonObject.put("settings", new JSONObject(str));
            }

            if ((flags & FLAG_COOKIES) == FLAG_COOKIES) {
                str = getCookies();
                if (str != null) jsonObject.put("cookies", new JSONArray(str));
            }

            if ((flags & FLAG_FAVORITES) == FLAG_FAVORITES) {
                str = getFavorites();
                if (str != null) jsonObject.put("favs", new JSONArray(str));
            }

            result = jsonObject.toString();
        } catch (final Exception e) {
            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_EXPORT, "getExportString");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
        return result;
    }

    @Nullable
    private static String getSettings() {
        String result = null;

        if (settingsHelper != null) {
            try {
                final JSONObject json = new JSONObject();
                json.put(Constants.APP_THEME, settingsHelper.getInteger(Constants.APP_THEME));
                json.put(Constants.APP_LANGUAGE, settingsHelper.getInteger(Constants.APP_LANGUAGE));

                String str = settingsHelper.getString(Constants.FOLDER_PATH);
                if (!Utils.isEmpty(str)) json.put(Constants.FOLDER_PATH, str);

                str = settingsHelper.getString(Constants.DATE_TIME_FORMAT);
                if (!Utils.isEmpty(str)) json.put(Constants.DATE_TIME_FORMAT, str);

                str = settingsHelper.getString(Constants.DATE_TIME_SELECTION);
                if (!Utils.isEmpty(str)) json.put(Constants.DATE_TIME_SELECTION, str);

                str = settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT);
                if (!Utils.isEmpty(str)) json.put(Constants.CUSTOM_DATE_TIME_FORMAT, str);

                json.put(Constants.DOWNLOAD_USER_FOLDER, settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER));
                json.put(Constants.MUTED_VIDEOS, settingsHelper.getBoolean(Constants.MUTED_VIDEOS));
                json.put(Constants.BOTTOM_TOOLBAR, settingsHelper.getBoolean(Constants.BOTTOM_TOOLBAR));
                json.put(Constants.AUTOPLAY_VIDEOS, settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));
                json.put(Constants.AUTOLOAD_POSTS, settingsHelper.getBoolean(Constants.AUTOLOAD_POSTS));
                json.put(Constants.FOLDER_SAVE_TO, settingsHelper.getBoolean(Constants.FOLDER_SAVE_TO));

                result = json.toString();
            } catch (final Exception e) {
                result = null;
                if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_EXPORT, "getSettings");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }

        return result;
    }

    @Nullable
    private static String getFavorites() {
        String result = null;
        if (Utils.dataBox != null) {
            try {
                final ArrayList<DataBox.FavoriteModel> allFavorites = Utils.dataBox.getAllFavorites();
                final int allFavoritesSize;
                if (allFavorites != null && (allFavoritesSize = allFavorites.size()) > 0) {
                    final JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < allFavoritesSize; i++) {
                        final DataBox.FavoriteModel favorite = allFavorites.get(i);
                        final JSONObject jsonObject = new JSONObject();
                        jsonObject.put("q", favorite.getQuery());
                        jsonObject.put("d", favorite.getDate());
                        jsonObject.put("s", favorite.getDisplayName());
                        jsonArray.put(jsonObject);
                    }
                    result = jsonArray.toString();
                }
            } catch (final Exception e) {
                result = null;
                if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_EXPORT, "getFavorites");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
        return result;
    }

    @Nullable
    private static String getCookies() {
        String result = null;
        if (Utils.dataBox != null) {
            try {
                final ArrayList<DataBox.CookieModel> allCookies = Utils.dataBox.getAllCookies();
                final int allCookiesSize;
                if (allCookies != null && (allCookiesSize = allCookies.size()) > 0) {
                    final JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < allCookiesSize; i++) {
                        final DataBox.CookieModel cookieModel = allCookies.get(i);
                        final JSONObject jsonObject = new JSONObject();
                        jsonObject.put("i", cookieModel.getUid());
                        jsonObject.put("u", cookieModel.getUsername());
                        jsonObject.put("c", cookieModel.getCookie());
                        jsonArray.put(jsonObject);
                    }
                    result = jsonArray.toString();
                }
            } catch (final Exception e) {
                result = null;
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
        return result;
    }

    private final static class PasswordUtils {
        private static final String cipherAlgo = "AES";
        private static final String cipherTran = "AES/CBC/PKCS5Padding";

        private static byte[] dec(final String encrypted, final byte[] keyValue) throws Exception {
            final Cipher cipher = Cipher.getInstance(cipherTran);
            final SecretKeySpec secretKey = new SecretKeySpec(keyValue, cipherAlgo);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            return cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT | Base64.NO_PADDING | Base64.NO_WRAP));
        }

        private static byte[] enc(@NonNull final String str, final byte[] keyValue) throws Exception {
            final Cipher cipher = Cipher.getInstance(cipherTran);
            final SecretKeySpec secretKey = new SecretKeySpec(keyValue, cipherAlgo);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            final byte[] bytes = cipher.doFinal(str.getBytes());
            return Base64.encode(bytes, Base64.DEFAULT | Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }
}