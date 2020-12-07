package awais.instagrabber.activities;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import awais.instagrabber.R;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.enums.IntentModelType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.IntentUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public final class DirectDownload extends AppCompatActivity {
    private static final int NOTIFICATION_ID = 1900000000;
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;

    private boolean contextFound = false;
    private Intent intent;
    private Context context;
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct);
    }

    @Override
    public void onWindowAttributesChanged(final WindowManager.LayoutParams params) {
        super.onWindowAttributesChanged(params);
        if (!contextFound) {
            intent = getIntent();
            context = getApplicationContext();
            if (intent != null && context != null) {
                contextFound = true;
                checkPermissions();
            }
        }
    }

    @Override
    public Resources getResources() {
        if (!contextFound) {
            intent = getIntent();
            context = getApplicationContext();
            if (intent != null && context != null) {
                contextFound = true;
                checkPermissions();
            }
        }
        return super.getResources();
    }

    private synchronized void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            doDownload();
            return;
        }
        ActivityCompat.requestPermissions(this, DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        final boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == STORAGE_PERM_REQUEST_CODE && granted) {
            doDownload();
        }
    }

    private synchronized void doDownload() {
        CookieUtils.setupCookies(Utils.settingsHelper.getString(Constants.COOKIE));
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (TextUtils.isEmpty(action) || Intent.ACTION_MAIN.equals(action)) {
            finish();
            return;
        }
        boolean error = true;

        String data = null;
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            final Object extraData = extras.get(Intent.EXTRA_TEXT);
            if (extraData != null) {
                error = false;
                data = extraData.toString();
            }
        }
        if (error) {
            final Uri intentData = intent.getData();
            if (intentData != null) data = intentData.toString();
        }
        if (data == null || TextUtils.isEmpty(data)) {
            finish();
            return;
        }
        final IntentModel model = IntentUtils.parseUrl(data);
        if (model == null || model.getType() != IntentModelType.POST) {
            finish();
            return;
        }
        final String text = model.getText();
        new PostFetcher(text, new FetchListener<FeedModel>() {
            @Override
            public void doBefore() {
                if (notificationManager == null) return;
                final Notification fetchingPostNotification = new NotificationCompat.Builder(getApplicationContext(), Constants.DOWNLOAD_CHANNEL_ID)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                        .setSmallIcon(R.drawable.ic_download)
                        .setAutoCancel(false)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setContentText(getString(R.string.direct_download_loading))
                        .build();
                notificationManager.notify(NOTIFICATION_ID, fetchingPostNotification);
            }

            @Override
            public void onResult(final FeedModel result) {
                if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
                if (result == null) {
                    finish();
                    return;
                }
                DownloadUtils.download(getApplicationContext(), result);
                finish();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}