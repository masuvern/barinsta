package awais.instagrabber.directdownload;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;

import awais.instagrabber.R;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.IntentModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.IntentModelType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.IntentUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.utils.Constants.CHANNEL_ID;
import static awais.instagrabber.utils.Constants.CHANNEL_NAME;
import static awais.instagrabber.utils.Utils.isChannelCreated;
import static awais.instagrabber.utils.Utils.notificationManager;

public final class DirectDownload extends Activity {
    private boolean isFound = false;
    private Intent intent;
    private Context context;

    @Override
    public void onWindowAttributesChanged(final WindowManager.LayoutParams params) {
        super.onWindowAttributesChanged(params);
        if (!isFound) {
            intent = getIntent();
            context = getApplicationContext();
            if (intent != null && context != null) {
                isFound = true;
                checkIntent();
            }
        }
    }

    @Override
    public Resources getResources() {
        if (!isFound) {
            intent = getIntent();
            context = getApplicationContext();
            if (intent != null && context != null) {
                isFound = true;
                checkIntent();
            }
        }
        return super.getResources();
    }

    private synchronized void checkIntent() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            doDownload();
        else {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.direct_download_perms_ask, Toast.LENGTH_LONG).show();
                    handler.removeCallbacks(this);
                }
            });
            ActivityCompat.requestPermissions(this, DownloadUtils.PERMS, 8020);
        }
        finish();
    }

    private synchronized void doDownload() {
        final String action = intent.getAction();
        if (!TextUtils.isEmpty(action) && !Intent.ACTION_MAIN.equals(action)) {
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

            if (data != null && !TextUtils.isEmpty(data)) {
                final IntentModel model = IntentUtils.parseUrl(data);
                if (model != null && model.getType() == IntentModelType.POST) {
                    final String text = model.getText();

                    new PostFetcher(text, new FetchListener<ViewerPostModel[]>() {
                        @Override
                        public void doBefore() {
                            if (notificationManager == null)
                                notificationManager = NotificationManagerCompat.from(context.getApplicationContext());

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isChannelCreated) {
                                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                                        CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH));
                                isChannelCreated = true;
                            }
                            final Notification fetchingPostNotif = new NotificationCompat.Builder(context, CHANNEL_ID)
                                    .setCategory(NotificationCompat.CATEGORY_STATUS).setSmallIcon(R.mipmap.ic_launcher)
                                    .setAutoCancel(false).setPriority(NotificationCompat.PRIORITY_MIN)
                                    .setContentText(context.getString(R.string.direct_download_loading)).build();
                            notificationManager.notify(1900000000, fetchingPostNotif);
                        }

                        @Override
                        public void onResult(final ViewerPostModel[] result) {
                            if (notificationManager != null) notificationManager.cancel(1900000000);
                            if (result != null) {
                                if (result.length == 1) {
                                    DownloadUtils.batchDownload(context, result[0].getProfileModel().getUsername(), DownloadMethod.DOWNLOAD_DIRECT,
                                                                Arrays.asList(result));
                                } else if (result.length > 1) {
                                    context.startActivity(new Intent(context, MultiDirectDialog.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                            .putExtra(Constants.EXTRAS_POST, result));
                                }
                            }
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
    }
}