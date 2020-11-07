package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import java.io.IOException;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddCookiesInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull final Chain chain) throws IOException {
        final Request request = chain.request();
        final Request.Builder builder = request.newBuilder();
        final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        builder.addHeader("Cookie", cookie);
        final String userAgentHeader = "User-Agent";
        if (request.header(userAgentHeader) == null) {
            builder.addHeader(userAgentHeader, Constants.I_USER_AGENT);
        }
        final String languageHeader = "Accept-Language";
        if (request.header(languageHeader) == null) {
            builder.addHeader(languageHeader, LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
        }
        final Request updatedRequest = builder.build();
        return chain.proceed(updatedRequest);
    }
}