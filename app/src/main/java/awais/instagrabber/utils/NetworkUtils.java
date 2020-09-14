package awais.instagrabber.utils;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Set;

public final class NetworkUtils {
    @NonNull
    public static String readFromConnection(@NonNull final HttpURLConnection conn) throws Exception {
        final StringBuilder sb = new StringBuilder();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public static void setConnectionHeaders(final HttpURLConnection connection, final Map<String, String> headers) {
        if (connection == null || headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
    }

    public static String getQueryString(final Map<String, String> queryParamsMap) {
        if (queryParamsMap == null || queryParamsMap.isEmpty()) {
            return "";
        }
        final Set<Map.Entry<String, String>> params = queryParamsMap.entrySet();
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> param : params) {
            if (TextUtils.isEmpty(param.getKey())) {
                continue;
            }
            if (builder.length() != 0) {
                builder.append("&");
            }
            builder.append(param.getKey());
            builder.append("=");
            builder.append(param.getValue() != null ? param.getValue() : "");
        }
        return builder.toString();
    }
}
