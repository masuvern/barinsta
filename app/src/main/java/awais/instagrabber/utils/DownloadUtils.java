package awais.instagrabber.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import awais.instagrabber.R;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.workers.DownloadWorker;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;

public final class DownloadUtils {
    public static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String[] PERMS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @NonNull
    private static File getDownloadDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");

        if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
            final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
            if (!TextUtils.isEmpty(customPath)) {
                dir = new File(customPath);
            }
        }
        return dir;
    }

    @Nullable
    private static File getDownloadDir(@NonNull final Context context, @Nullable final String username) {
        return getDownloadDir(context, username, false);
    }

    @Nullable
    private static File getDownloadDir(final Context context,
                                       @Nullable final String username,
                                       final boolean skipCreateDir) {
        File dir = getDownloadDir();

        if (Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !TextUtils.isEmpty(username)) {
            final String finaleUsername = username.startsWith("@") ? username : "@" + username;
            dir = new File(dir, finaleUsername);
        }

        if (context != null && !skipCreateDir && !dir.exists() && !dir.mkdirs()) {
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

    @NonNull
    public static File getTempFile() {
        final File dir = getDownloadDir();
        return new File(dir, UUID.randomUUID().toString());
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

    public static List<Boolean> checkDownloaded(@NonNull final FeedModel feedModel) {
        final List<Boolean> checkList = new LinkedList<>();
        final File downloadDir = getDownloadDir(null, "@" + feedModel.getProfileModel().getUsername(), true);
        switch (feedModel.getItemType()) {
            case MEDIA_TYPE_IMAGE:
            case MEDIA_TYPE_VIDEO: {
                final String url = feedModel.getDisplayUrl();
                final File file = getDownloadSaveFile(downloadDir, feedModel.getShortCode(), url);
                checkList.add(file.exists());
                break;
            }
            case MEDIA_TYPE_SLIDER:
                final List<PostChild> sliderItems = feedModel.getSliderItems();
                for (int i = 0; i < sliderItems.size(); i++) {
                    final PostChild child = sliderItems.get(i);
                    final String url = child.getDisplayUrl();
                    final File file = getDownloadChildSaveFile(downloadDir, feedModel.getShortCode(), i + 1, url);
                    checkList.add(file.exists());
                }
                break;
            default:
        }
        return checkList;
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
                                @NonNull final StoryModel storyModel) {
        final File downloadDir = getDownloadDir(context, "@" + storyModel.getUsername());
        final String url = storyModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO
                           ? storyModel.getVideoUrl()
                           : storyModel.getStoryUrl();
        final File saveFile = new File(downloadDir,
                                       storyModel.getStoryMediaId()
                                               + "_" + storyModel.getTimestamp()
                                               + DownloadUtils.getFileExtensionFromUrl(url));
        download(context, url, saveFile.getAbsolutePath());
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
                    final File file = getDownloadSaveFile(downloadDir, feedModel.getShortCode(), url);
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
                        final File file = getDownloadChildSaveFile(downloadDir, feedModel.getShortCode(), i + 1, url);
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
