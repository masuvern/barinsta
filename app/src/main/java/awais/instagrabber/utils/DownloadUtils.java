package awais.instagrabber.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.gson.Gson;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.workers.DownloadWorker;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;

public final class DownloadUtils {
    public static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String[] PERMS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static int lastNotificationId = UUID.randomUUID().hashCode();

    public synchronized static int getNextDownloadNotificationId(@NonNull final Context context) {
        lastNotificationId = lastNotificationId + 1;
        if (lastNotificationId == Integer.MAX_VALUE) {
            lastNotificationId = UUID.randomUUID().hashCode();
        }
        return lastNotificationId;
    }

    public static void batchDownload(@NonNull final Context context,
                                     @Nullable String username,
                                     final DownloadMethod method,
                                     final List<? extends BasePostModel> itemsToDownload) {
        if (Utils.settingsHelper == null) Utils.settingsHelper = new SettingsHelper(context);

        if (itemsToDownload == null || itemsToDownload.size() < 1) return;

        if (username != null && username.charAt(0) == '@') username = username.substring(1);

        if (ContextCompat.checkSelfPermission(context, PERMS[0]) == PackageManager.PERMISSION_GRANTED)
            batchDownloadImpl(context, username, method, itemsToDownload);
        else if (context instanceof Activity)
            ActivityCompat.requestPermissions((Activity) context, PERMS, 8020);
    }

    private static void batchDownloadImpl(@NonNull final Context context,
                                          @Nullable final String username,
                                          final DownloadMethod method,
                                          final List<? extends BasePostModel> itemsToDownload) {
        final File dir = getDownloadDir(context, username);
        if (dir == null) return;
        boolean checkEachPost = false;
        switch (method) {
            case DOWNLOAD_SAVED:
            case DOWNLOAD_MAIN:
            case DOWNLOAD_DISCOVER:
                checkEachPost = true;
                break;
        }
        final int itemsToDownloadSize = itemsToDownload.size();
        for (int i = 0; i < itemsToDownloadSize; i++) {
            final BasePostModel selectedItem = itemsToDownload.get(i);
            if (!checkEachPost) {
                final boolean isSlider = itemsToDownloadSize > 1;
                final File saveFile = getDownloadSaveFile(dir,
                                                          selectedItem.getShortCode(),
                                                          isSlider ? "_slide_" + (i + 1) : "",
                                                          selectedItem.getDisplayUrl()
                );
                new DownloadAsync(context,
                                  selectedItem.getDisplayUrl(),
                                  saveFile,
                                  file -> selectedItem.setDownloaded(true))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                final File finalDir = dir;
                new PostFetcher(selectedItem.getShortCode(), result -> {
                    if (result == null) return;
                    final boolean isSlider = result.getItemType() == MediaItemType.MEDIA_TYPE_SLIDER;
                    if (isSlider) {
                        for (int j = 0; j < result.getSliderItems().size(); j++) {
                            final PostChild model = result.getSliderItems().get(j);
                            final File saveFile = getDownloadSaveFile(
                                    finalDir,
                                    model.getShortCode(),
                                    "_slide_" + (j + 1),
                                    model.getDisplayUrl()
                            );
                            new DownloadAsync(context,
                                              model.getDisplayUrl(),
                                              saveFile,
                                              file -> {}/*model.setDownloaded(true)*/)
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    } else {
                        final File saveFile = getDownloadSaveFile(
                                finalDir,
                                result.getPostId(),
                                result.getDisplayUrl()
                        );
                        new DownloadAsync(context,
                                          result.getDisplayUrl(),
                                          saveFile,
                                          file -> result.setDownloaded(true))
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    @Nullable
    private static File getDownloadDir(@NonNull final Context context, @Nullable final String username) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");

        if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
            final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
            if (!TextUtils.isEmpty(customPath)) dir = new File(customPath);
        }

        if (Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !TextUtils.isEmpty(username)) {
            final String finaleUsername = username.startsWith("@") ? username : "@" + username;
            dir = new File(dir, finaleUsername);
        }

        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
            return null;
        }
        return dir;
    }

    public static void dmDownload(@NonNull final Context context,
                                  @Nullable final String username,
                                  final String modelId,
                                  final String url) {
        if (url == null) return;
        if (ContextCompat.checkSelfPermission(context, PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
            dmDownloadImpl(context, username, modelId, url);
        } else if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context, PERMS, 8020);
        }
    }

    private static void dmDownloadImpl(@NonNull final Context context,
                                       @Nullable final String username,
                                       final String modelId,
                                       final String url) {
        final File dir = getDownloadDir(context, username);
        if (dir.exists() || dir.mkdirs()) {
            download(context,
                     url,
                     getDownloadSaveFile(dir, modelId, url).getAbsolutePath());
            return;
        }
        Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private static File getDownloadSaveFile(final File finalDir,
                                            final String postId,
                                            final String displayUrl) {
        return getDownloadSaveFile(finalDir, postId, "", displayUrl);
    }

    private static File getDownloadChildSaveFile(final File downloadDir,
                                                 final String postId,
                                                 final int childPosition,
                                                 final String url) {
        final String sliderPostfix = "_slide_" + childPosition;
        return getDownloadSaveFile(downloadDir, postId, sliderPostfix, url);
    }

    @NonNull
    private static File getDownloadSaveFile(final File finalDir,
                                            final String postId,
                                            final String sliderPostfix,
                                            final String displayUrl) {
        final String fileName = postId + sliderPostfix + "." + getFileExtensionFromUrl(displayUrl);
        return new File(finalDir, fileName);
    }

    /**
     * Copied from {@link MimeTypeMap#getFileExtensionFromUrl(String)})
     * <p>
     * Returns the file extension or an empty string if there is no
     * extension. This method is a convenience method for obtaining the
     * extension of a url and has undefined results for other Strings.
     *
     * @param url URL
     * @return The file extension of the given url.
     */
    public static String getFileExtensionFromUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() &&
                    Pattern.matches("[a-zA-Z_0-9.\\-()%]+", filename)) {
                int dotPos = filename.lastIndexOf('.');
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1);
                }
            }
        }

        return "";
    }

    public static void checkExistence(final File downloadDir,
                                      final File customDir,
                                      final boolean isSlider,
                                      @NonNull final BasePostModel model) {
        boolean exists = false;

        try {
            final String displayUrl = model.getDisplayUrl();
            int index = displayUrl.indexOf('?');
            if (index < 0) {
                return;
            }
            final String fileName = model.getPostId() + '_';
            final String extension = displayUrl.substring(index - 4, index);

            final String fileWithoutPrefix = fileName + '0' + extension;
            exists = new File(downloadDir, fileWithoutPrefix).exists();
            if (!exists) {
                final String fileWithPrefix = fileName + "[\\d]+(|_slide_[\\d]+)(\\.mp4|\\\\" + extension + ")";
                final FilenameFilter filenameFilter = (dir, name) -> Pattern.matches(fileWithPrefix, name);

                File[] files = downloadDir.listFiles(filenameFilter);
                if ((files == null || files.length < 1) && customDir != null)
                    files = customDir.listFiles(filenameFilter);

                if (files != null && files.length >= 1) exists = true;
            }
        } catch (final Exception e) {
            if (Utils.logCollector != null)
                Utils.logCollector.appendException(e, LogCollector.LogFile.UTILS, "checkExistence",
                                                   new Pair<>("isSlider", isSlider),
                                                   new Pair<>("model", model));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        model.setDownloaded(exists);
    }

    public static void showDownloadDialog(@NonNull Context context,
                                          @NonNull final FeedModel feedModel,
                                          final int childPosition) {
        if (childPosition >= 0) {
            final DialogInterface.OnClickListener clickListener = (dialog, which) -> {
                switch (which) {
                    case 0:
                        DownloadUtils.download(context, feedModel, childPosition);
                        break;
                    case 1:
                        DownloadUtils.download(context, feedModel);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                    default:
                        dialog.dismiss();
                        break;
                }
            };
            final String[] items = new String[]{
                    context.getString(R.string.post_viewer_download_current),
                    context.getString(R.string.post_viewer_download_album),
            };
            new AlertDialog.Builder(context)
                    .setTitle(R.string.post_viewer_download_dialog_title)
                    .setItems(items, clickListener)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        DownloadUtils.download(context, feedModel);
    }

    public static void download(@NonNull final Context context,
                                @NonNull final FeedModel feedModel) {
        download(context, feedModel, -1);
    }

    public static void download(@NonNull final Context context,
                                @NonNull final FeedModel feedModel,
                                final int position) {
        download(context, Collections.singletonList(feedModel), position);
    }

    public static void download(@NonNull final Context context,
                                @NonNull final List<FeedModel> feedModels) {
        download(context, feedModels, -1);
    }

    private static void download(@NonNull final Context context,
                                 @NonNull final List<FeedModel> feedModels,
                                 final int childPositionIfSingle) {
        final Map<String, String> map = new HashMap<>();
        for (final FeedModel feedModel : feedModels) {
            final File downloadDir = getDownloadDir(context, "@" + feedModel.getProfileModel().getUsername());
            if (downloadDir == null) return;
            switch (feedModel.getItemType()) {
                case MEDIA_TYPE_IMAGE:
                case MEDIA_TYPE_VIDEO: {
                    final String url = feedModel.getDisplayUrl();
                    final File file = getDownloadSaveFile(downloadDir, feedModel.getPostId(), url);
                    map.put(url, file.getAbsolutePath());
                    break;
                }
                case MEDIA_TYPE_SLIDER:
                    final List<PostChild> sliderItems = feedModel.getSliderItems();
                    for (int i = 0; i < sliderItems.size(); i++) {
                        if (childPositionIfSingle >= 0 && feedModels.size() == 1 && i != childPositionIfSingle) {
                            continue;
                        }
                        final PostChild child = sliderItems.get(i);
                        final String url = child.getDisplayUrl();
                        final File file = getDownloadChildSaveFile(downloadDir, feedModel.getPostId(), i + 1, url);
                        map.put(url, file.getAbsolutePath());
                    }
                    break;
                default:
            }
        }
        download(context, map);
    }

    public static void download(final Context context,
                                final String url,
                                final String filePath) {
        if (context == null || url == null || filePath == null) return;
        download(context, Collections.singletonMap(url, filePath));
    }

    private static void download(final Context context, final Map<String, String> urlFilePathMap) {
        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        final DownloadWorker.DownloadRequest request = DownloadWorker.DownloadRequest.builder()
                                                                                     .setUrlToFilePathMap(urlFilePathMap)
                                                                                     .build();
        final WorkRequest downloadWorkRequest = new OneTimeWorkRequest.Builder(DownloadWorker.class)
                .setInputData(
                        new Data.Builder()
                                .putString(DownloadWorker.KEY_DOWNLOAD_REQUEST_JSON,
                                           new Gson().toJson(request))
                                .build()
                )
                .setConstraints(constraints)
                .addTag("download")
                .build();
        WorkManager.getInstance(context)
                   .enqueue(downloadWorkRequest);
    }
}
