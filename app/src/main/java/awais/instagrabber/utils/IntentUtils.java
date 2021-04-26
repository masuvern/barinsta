package awais.instagrabber.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.enums.IntentModelType;

public final class IntentUtils {

    @Nullable
    public static IntentModel parseUrl(@NonNull String url) {
        if (url.contains("instagr.am/")) {
            url = url.replaceFirst("s?://(?:www\\.)?instagr\\.am/", "s://www.instagram.com/");
        }
        final int wwwDel = url.contains("www.") ? 4 : 0;
        final boolean isHttps = url.startsWith("https");

        IntentModelType type = IntentModelType.UNKNOWN;
        if (url.contains("instagram.com/")) {
            url = url.substring((isHttps ? 22 : 21) + wwwDel);

            // final char firstChar = url.charAt(0);
            if (url.startsWith("p/") || url.startsWith("reel/") || url.startsWith("tv/")) {
                url = url.substring(url.startsWith("p/") ? 2 : (url.startsWith("tv/") ? 3 : 5));
                type = IntentModelType.POST;
            } else if (url.startsWith("explore/tags/")) {
                url = url.substring(13);
                type = IntentModelType.HASHTAG;
            } else if (url.startsWith("explore/locations/")) {
                url = url.substring(18);
                type = IntentModelType.LOCATION;
            } else if (url.startsWith("_u/")) { // usually exists in embeds
                url = url.substring(3);
                type = IntentModelType.USERNAME;
            }
            url = cleanString(url);
            if (TextUtils.isEmpty(url)) return null;
            else if (type == IntentModelType.UNKNOWN){
                type = IntentModelType.USERNAME;
            }
        } else if (url.contains("ig.me/u/")) {
            url = url.substring((isHttps ? 16 : 15) + wwwDel);
            url = cleanString(url);
            type = IntentModelType.USERNAME;

        } else return null;

        final int clipLen = url.length() - 1;
        if (url.charAt(clipLen) == '/')
            url = url.substring(0, clipLen);

        if (type == IntentModelType.LOCATION && url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }

        if (!url.contains("/")) return new IntentModel(type, url);
        return null;
    }

    @NonNull
    public static String cleanString(@NonNull final String clipString) {
        final int queryIndex = clipString.indexOf('?');
        final int paramIndex = clipString.indexOf('#');
        int startIndex = -1;
        if (queryIndex > 0 && paramIndex > 0) {
            if (queryIndex < paramIndex) startIndex = queryIndex;
            else if (paramIndex < queryIndex) startIndex = paramIndex;
        } else if (queryIndex == -1 && paramIndex >= 0) {
            startIndex = paramIndex;
        } else if (paramIndex == -1 && queryIndex >= 0) {
            startIndex = queryIndex;
        }
        return startIndex != -1 ? clipString.substring(0, startIndex) : clipString;
    }
}
