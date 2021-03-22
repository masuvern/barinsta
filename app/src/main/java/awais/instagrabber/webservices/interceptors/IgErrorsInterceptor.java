package awais.instagrabber.webservices.interceptors;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.json.JSONObject;

import java.io.IOException;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.TextUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class IgErrorsInterceptor implements Interceptor {
    private static final String TAG = IgErrorsInterceptor.class.getSimpleName();

    private MainActivity mainActivity;

    public IgErrorsInterceptor(@NonNull final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull final Chain chain) throws IOException {
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        if (response.isSuccessful()) {
            return response;
        }
        checkError(response);
        return response;
    }

    private void checkError(@NonNull final Response response) {
        final int errorCode = response.code();
        switch (errorCode) {
            case 429: // "429 Too Many Requests"
                // ('Throttled by Instagram because of too many API requests.');
                showErrorDialog(R.string.throttle_error);
                return;
            case 431: // "431 Request Header Fields Too Large"
                // show dialog?
                Log.e(TAG, "Network error: " + getMessage(errorCode, "The request start-line and/or headers are too large to process."));
                return;
        }
        final ResponseBody body = response.body();
        if (body == null) return;
        try {
            final String bodyString = body.string();
            final JSONObject jsonObject = new JSONObject(bodyString);
            String message = jsonObject.optString("message", null);
            if (!TextUtils.isEmpty(message)) {
                message = message.toLowerCase();
                switch (message) {
                    case "user_has_logged_out":
                        showErrorDialog(R.string.account_logged_out);
                        return;
                    case "login_required":
                        showErrorDialog(R.string.login_required);
                        return;
                    case "not authorized to view user": // Do we handle this in profile view fragment?
                    case "challenge_required": // Since we make users login using browser, we should not be getting this error in api requests
                    default:
                        Log.e(TAG, "checkError: " + bodyString);
                        return;
                }
            }
            final String errorType = jsonObject.optString("error_type", null);
            if (TextUtils.isEmpty(errorType)) return;
            if (errorType.equals("sentry_block")) {
                showErrorDialog(R.string.sentry_block);
                return;
            }
            if (errorType.equals("inactive user")) {
                showErrorDialog(R.string.inactive_user);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkError: ", e);
        }
    }

    @NonNull
    private String getMessage(final int errorCode, final String message) {
        return String.format("code: %s, internalMessage: %s", errorCode, message);
    }

    private void showErrorDialog(@StringRes final int messageResId) {
        if (mainActivity == null) return;
        if (messageResId == 0) return;
        final ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                Constants.GLOBAL_NETWORK_ERROR_DIALOG_REQUEST_CODE,
                R.string.error,
                messageResId,
                R.string.ok,
                0,
                0
        );
        dialogFragment.show(mainActivity.getSupportFragmentManager(), "network_error_dialog");
    }

    public void destroy() {
        mainActivity = null;
    }
}