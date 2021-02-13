package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.DirectMessagesService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class CreateThreadAction extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "CommentAction";

    private final String cookie;
    private final long userId;
    private final OnTaskCompleteListener onTaskCompleteListener;
    private final DirectMessagesService directMessagesService;

    public CreateThreadAction(final String cookie, final long userId, final OnTaskCompleteListener onTaskCompleteListener) {
        this.cookie = cookie;
        this.userId = userId;
        this.onTaskCompleteListener = onTaskCompleteListener;
        directMessagesService = DirectMessagesService.getInstance(CookieUtils.getCsrfTokenFromCookie(cookie),
                                                                  CookieUtils.getUserIdFromCookie(cookie),
                                                                  Utils.settingsHelper.getString(Constants.DEVICE_UUID));
    }

    protected Void doInBackground(Void... lmao) {
        final Call<DirectThread> createThreadRequest = directMessagesService.createThread(Collections.singletonList(userId), null);
        createThreadRequest.enqueue(new Callback<DirectThread>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThread> call, @NonNull final Response<DirectThread> response) {
                if (!response.isSuccessful()) {
                    if (response.errorBody() != null) {
                        try {
                            final String string = response.errorBody().string();
                            final String msg = String.format(Locale.US,
                                    "onResponse: url: %s, responseCode: %d, errorBody: %s",
                                    call.request().url().toString(),
                                    response.code(),
                                    string);
                            Log.e(TAG, msg);
                        } catch (IOException e) {
                            Log.e(TAG, "onResponse: ", e);
                        }
                    }
                    Log.e(TAG, "onResponse: request was not successful and response error body was null");
                }
                onTaskCompleteListener.onTaskComplete(response.body());
                if (response.body() == null) {
                    Log.e(TAG, "onResponse: thread is null");
                }
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThread> call, @NonNull final Throwable t) {
                onTaskCompleteListener.onTaskComplete(null);
            }
        });
        return null;
    }

//    @Override
//    protected void onPostExecute() {
//    }

    public interface OnTaskCompleteListener {
        void onTaskComplete(final DirectThread thread);
    }
}
