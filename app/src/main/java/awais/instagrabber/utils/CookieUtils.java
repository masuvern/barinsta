package awais.instagrabber.utils;

import android.util.Log;
import android.webkit.CookieManager;

import androidx.annotation.Nullable;

import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;

import awais.instagrabber.BuildConfig;
import awaisomereport.LogCollector;

public final class CookieUtils {
    public static final CookieManager COOKIE_MANAGER = CookieManager.getInstance();
    public static final java.net.CookieManager NET_COOKIE_MANAGER = new java.net.CookieManager(null, CookiePolicy.ACCEPT_ALL);

    public static void setupCookies(final String cookieRaw) {
        final CookieStore cookieStore = NET_COOKIE_MANAGER.getCookieStore();
        if (cookieStore == null || TextUtils.isEmpty(cookieRaw)) {
            return;
        }
        if (cookieRaw.equals("REMOVE")) {
            cookieStore.removeAll();
            Utils.dataBox.deleteAllUserCookies();
            return;
        }
        else if (cookieRaw.equals("LOGOUT")) {
            cookieStore.removeAll();
            return;
        }
        try {
            final URI uri1 = new URI("https://instagram.com");
            final URI uri2 = new URI("https://instagram.com/");
            final URI uri3 = new URI("https://i.instagram.com/");
            for (final String cookie : cookieRaw.split("; ")) {
                final String[] strings = cookie.split("=", 2);
                final HttpCookie httpCookie = new HttpCookie(strings[0].trim(), strings[1].trim());
                httpCookie.setDomain(".instagram.com");
                httpCookie.setPath("/");
                httpCookie.setVersion(0);
                cookieStore.add(uri1, httpCookie);
                cookieStore.add(uri2, httpCookie);
                cookieStore.add(uri3, httpCookie);
            }
        } catch (final URISyntaxException e) {
            if (Utils.logCollector != null)
                Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "setupCookies");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }
    }

    @Nullable
    public static String getUserIdFromCookie(final String cookie) {
        if (!TextUtils.isEmpty(cookie)) {
            final int uidIndex = cookie.indexOf("ds_user_id=");
            if (uidIndex > 0) {
                String uid = cookie.split("ds_user_id=")[1].split(";")[0];
                return !TextUtils.isEmpty(uid) ? uid : null;
            }
        }
        return null;
    }

    public static String getCsrfTokenFromCookie(final String cookie) {
        if (cookie == null) {
            return null;
        }
        return cookie.split("csrftoken=")[1].split(";")[0];
    }

    @Nullable
    public static String getCookie(@Nullable final String webViewUrl) {
        int lastLongestCookieLength = 0;
        String mainCookie = null;

        String cookie;
        if (!TextUtils.isEmpty(webViewUrl)) {
            cookie = COOKIE_MANAGER.getCookie(webViewUrl);
            if (cookie != null) {
                final int cookieLen = cookie.length();
                if (cookieLen > lastLongestCookieLength) {
                    mainCookie = cookie;
                    lastLongestCookieLength = cookieLen;
                }
            }
        }
        cookie = COOKIE_MANAGER.getCookie("https://instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("https://instagram.com/");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("http://instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("http://instagram.com/");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("https://www.instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("https://www.instagram.com/");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("http://www.instagram.com");
        if (cookie != null) {
            final int cookieLen = cookie.length();
            if (cookieLen > lastLongestCookieLength) {
                mainCookie = cookie;
                lastLongestCookieLength = cookieLen;
            }
        }
        cookie = COOKIE_MANAGER.getCookie("http://www.instagram.com/");
        if (cookie != null && cookie.length() > lastLongestCookieLength) mainCookie = cookie;

        return mainCookie;
    }
}
