package awais.instagrabber.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.UriPermission;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import awais.instagrabber.R;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Audio;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.VideoVersion;
import awais.instagrabber.workers.DownloadWorker;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;

public final class DownloadUtils {
    private static final String TAG = DownloadUtils.class.getSimpleName();
    // private static final String DIR_BARINSTA = "Barinsta";
    private static final String DIR_DOWNLOADS = "Downloads";
    private static final String DIR_CAMERA = "Camera";
    private static final String DIR_EDIT = "Edit";
    private static final String DIR_RECORDINGS = "Sent Recordings";
    private static final String DIR_TEMP = "Temp";
    private static final String DIR_BACKUPS = "Backups";

    private static DocumentFile root;

    public static void init(@NonNull final Context context) throws ReselectDocumentTreeException {
        // if (DOWNLOADS_DIR_FILE == null) {
        //     final Uri uri = Utils.getSafUris(context, new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS))[0];
        //     DOWNLOADS_DIR_FILE = DocumentFile.fromTreeUri(context, uri);
        // }
        // if (!Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
        //     root = DOWNLOADS_DIR_FILE; // DocumentFile.fromFile(DOWNLOADS_DIR_FILE);
        //     return;
        // }
        final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
        if (TextUtils.isEmpty(customPath)) {
            throw new ReselectDocumentTreeException();
            // root = DOWNLOADS_DIR_FILE; // DocumentFile.fromFile(DOWNLOADS_DIR_FILE);
            // return;
        }
        if (!customPath.startsWith("content")) {
            // if (customPath.equals(DOWNLOADS_DIR_FILE.getAbsolutePath())) {
            //     throw new ReselectDocumentTreeException();
            // }
            // reselect the folder in selector view
            throw new ReselectDocumentTreeException(Uri.parse(customPath));
        }
        final Uri uri = Uri.parse(customPath);
        final List<UriPermission> existingPermissions = context.getContentResolver().getPersistedUriPermissions();
        if (existingPermissions.isEmpty()) {
            // reselect the folder in selector view
            throw new ReselectDocumentTreeException(uri);
        }
        final boolean anyMatch = existingPermissions.stream().anyMatch(uriPermission -> uriPermission.getUri().equals(uri));
        if (!anyMatch) {
            // reselect the folder in selector view
            throw new ReselectDocumentTreeException(uri);
        }
        root = DocumentFile.fromTreeUri(context, uri);
        // Log.d(TAG, "init: " + root);
        // final File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // final DocumentFile documentFile = DocumentFile.fromFile(parent);
        // Log.d(TAG, "init: " + documentFile);
    }

    public static void destroy() {
        root = null;
    }

    @Nullable
    public static DocumentFile getDownloadDir(final String... dirs) {
        if (root == null) {
            return null;
        }
        DocumentFile subDir = root;
        if (dirs != null) {
            for (final String dir : dirs) {
                if (subDir == null || TextUtils.isEmpty(dir)) continue;
                final DocumentFile subDirFile = subDir.findFile(dir);
                final boolean exists = subDirFile != null && subDirFile.exists();
                subDir = exists ? subDirFile : subDir.createDirectory(dir);
            }
        }
        return subDir;
    }

    @Nullable
    public static DocumentFile getDownloadDir() {
        // final File parent = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
        // final File dir = new File(new File(parent, "barinsta"), "downloads");
        // if (!dir.exists()) {
        //     final boolean mkdirs = dir.mkdirs();
        //     if (!mkdirs) {
        //         Log.e(TAG, "getDownloadDir: failed to create dir");
        //     }
        // }
        // if (Utils.settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
        //     final String customPath = Utils.settingsHelper.getString(FOLDER_PATH);
        //     if (!TextUtils.isEmpty(customPath)) {
        //         dir = new File(customPath);
        //     }
        // }
        return getDownloadDir(DIR_DOWNLOADS);
    }

    @Nullable
    public static DocumentFile getCameraDir() {
        return getDownloadDir(DIR_CAMERA);
    }

    @Nullable
    public static DocumentFile getImageEditDir(final String sessionId) {
        return getDownloadDir(DIR_EDIT, sessionId);
    }

    @Nullable
    public static DocumentFile getRecordingsDir() {
        return getDownloadDir(DIR_RECORDINGS);
    }

    @Nullable
    public static DocumentFile getBackupsDir() {
        return getDownloadDir(DIR_BACKUPS);
    }

    // @Nullable
    // private static DocumentFile getDownloadDir(@NonNull final Context context, @Nullable final String username) {
    //     return getDownloadDir(context, username, false);
    // }

    @Nullable
    private static DocumentFile getDownloadDir(final Context context,
                                               @Nullable final String username) {
        final List<String> userFolderPaths = getSubPathForUserFolder(username);
        DocumentFile dir = root;
        for (final String dirName : userFolderPaths) {
            final DocumentFile file = dir.findFile(dirName);
            if (file != null) {
                dir = file;
                continue;
            }
            dir = dir.createDirectory(dirName);
            if (dir == null) break;
        }
        // final String joined = android.text.TextUtils.join("/", userFolderPaths);
        // final Uri userFolderUri = DocumentsContract.buildDocumentUriUsingTree(root.getUri(), joined);
        // final DocumentFile userFolder = DocumentFile.fromSingleUri(context, userFolderUri);
        if (context != null && (dir == null || !dir.exists())) {
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
            return null;
        }
        return dir;
    }

    private static List<String> getSubPathForUserFolder(final String username) {
        final List<String> list = new ArrayList<>();
        if (!Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) || TextUtils.isEmpty(username)) {
            list.add(DIR_DOWNLOADS);
            return list;
        }
        final String finalUsername = username.startsWith("@") ? username.substring(1) : username;
        list.add(DIR_DOWNLOADS);
        list.add(finalUsername);
        return list;
    }

    private static DocumentFile getTempDir() {
        DocumentFile file = root.findFile(DIR_TEMP);
        if (file == null) {
            file = root.createDirectory(DIR_TEMP);
        }
        return file;
    }

    //    public static void dmDownload(@NonNull final Context context,
    //                                  @Nullable final String username,
    //                                  final String modelId,
    //                                  final String url) {
    //        if (url == null) return;
    //        if (ContextCompat.checkSelfPermission(context, PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
    //            dmDownloadImpl(context, username, modelId, url);
    //        } else if (context instanceof Activity) {
    //            ActivityCompat.requestPermissions((Activity) context, PERMS, 8020);
    //        }
    //    }

    // private static void dmDownloadImpl(@NonNull final Context context,
    //                                    @Nullable final String username,
    //                                    final String modelId,
    //                                    final String url) {
    //     final DocumentFile dir = getDownloadDir(context, username);
    //     if (dir != null && dir.exists()) {
    //         download(context, url, getDownloadSavePaths(dir, modelId, url));
    //         return;
    //     }
    //     Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show();
    // }

    private static Pair<List<String>, String> getDownloadSavePaths(final List<String> paths,
                                                                   final String postId,
                                                                   final String displayUrl) {
        return getDownloadSavePaths(paths, postId, "", displayUrl, "");
    }

    private static Pair<List<String>, String> getDownloadSavePaths(final List<String> paths,
                                                                   final String postId,
                                                                   final String displayUrl,
                                                                   final String username) {
        return getDownloadSavePaths(paths, postId, "", displayUrl, username);
    }

    private static Pair<List<String>, String> getDownloadChildSavePaths(final List<String> paths,
                                                                        final String postId,
                                                                        final int childPosition,
                                                                        final String url,
                                                                        final String username) {
        final String sliderPostfix = "_slide_" + childPosition;
        return getDownloadSavePaths(paths, postId, sliderPostfix, url, username);
    }

    private static Pair<List<String>, String> getDownloadSavePaths(final List<String> paths,
                                                                   final String postId,
                                                                   final String sliderPostfix,
                                                                   final String displayUrl,
                                                                   final String username) {
        if (paths == null) return null;
        final String extension = getFileExtensionFromUrl(displayUrl);
        final String usernamePrepend = TextUtils.isEmpty(username) ? "" : (username + "_");
        final String fileName = usernamePrepend + postId + sliderPostfix + extension;
        // return new File(finalDir, fileName);
        // DocumentFile file = finalDir.findFile(fileName);
        // if (file == null) {
        final String mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(extension.startsWith(".") ? extension.substring(1) : extension);
        // file = finalDir.createFile(mimeType, fileName);
        // }
        paths.add(fileName);
        return new Pair<>(paths, mimeType);
    }

    // public static DocumentFile getTempFile() {
    //     return getTempFile(null, null);
    // }

    public static DocumentFile getTempFile(final String fileName, final String extension) {
        final DocumentFile dir = getTempDir();
        String name = fileName;
        if (TextUtils.isEmpty(name)) {
            name = UUID.randomUUID().toString();
        }
        String mimeType = "application/octet-stream";
        if (!TextUtils.isEmpty(extension)) {
            name += "." + extension;
            final String mimeType1 = Utils.mimeTypeMap.getMimeTypeFromExtension(extension);
            if (mimeType1 != null) {
                mimeType = mimeType1;
            }
        }
        DocumentFile file = dir.findFile(name);
        if (file == null) {
            file = dir.createFile(mimeType, name);
        }
        return file;
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
                    return filename.substring(dotPos);
                }
            }
        }

        return "";
    }

    public static List<Boolean> checkDownloaded(@NonNull final Media media) {
        final List<Boolean> checkList = new LinkedList<>();
        final User user = media.getUser();
        String username = "username";
        if (user != null) {
            username = user.getUsername();
        }
        final List<String> userFolderPaths = getSubPathForUserFolder(username);
        switch (media.getMediaType()) {
            case MEDIA_TYPE_IMAGE:
            case MEDIA_TYPE_VIDEO: {
                final String url = ResponseBodyUtils.getImageUrl(media);
                final Pair<List<String>, String> file = getDownloadSavePaths(new ArrayList<>(userFolderPaths), media.getCode(), url, "");
                final boolean fileExists = file.first != null && checkPathExists(file.first);
                boolean usernameFileExists = false;
                if (!fileExists) {
                    final Pair<List<String>, String> usernameFile = getDownloadSavePaths(
                            new ArrayList<>(userFolderPaths), media.getCode(), url, username);
                    usernameFileExists = usernameFile.first != null && checkPathExists(usernameFile.first);
                }
                checkList.add(fileExists || usernameFileExists);
                break;
            }
            case MEDIA_TYPE_SLIDER:
                final List<Media> sliderItems = media.getCarouselMedia();
                for (int i = 0; i < sliderItems.size(); i++) {
                    final Media child = sliderItems.get(i);
                    if (child == null) continue;
                    final String url = ResponseBodyUtils.getImageUrl(child);
                    final Pair<List<String>, String> file = getDownloadChildSavePaths(
                            new ArrayList<>(userFolderPaths), media.getCode(), i + 1, url, "");
                    final boolean fileExists = file.first != null && checkPathExists(file.first);
                    boolean usernameFileExists = false;
                    if (!fileExists) {
                        final Pair<List<String>, String> usernameFile = getDownloadChildSavePaths(
                                new ArrayList<>(userFolderPaths), media.getCode(), i + 1, url, username);
                        usernameFileExists = usernameFile.first != null && checkPathExists(usernameFile.first);
                    }
                    checkList.add(fileExists || usernameFileExists);
                }
                break;
            default:
        }
        return checkList;
    }

    private static boolean checkPathExists(@NonNull final List<String> paths) {
        if (root == null) return false;
        DocumentFile dir = root;
        for (final String path : paths) {
            dir = dir.findFile(path);
            if (dir == null || !dir.exists()) {
                return false;
            }
        }
        return true;
    }

    public static void showDownloadDialog(@NonNull Context context,
                                          @NonNull final Media feedModel,
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
        final DocumentFile downloadDir = getDownloadDir(context, storyModel.getUsername());
        if (downloadDir == null) return;
        final String url = storyModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO
                           ? storyModel.getVideoUrl()
                           : storyModel.getStoryUrl();
        final String extension = DownloadUtils.getFileExtensionFromUrl(url);
        final String baseFileName = storyModel.getStoryMediaId() + "_"
                + storyModel.getTimestamp() + extension;
        final String usernamePrepend = Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_PREPEND_USER_NAME)
                                               && storyModel.getUsername() != null ? storyModel.getUsername() + "_" : "";
        final String fileName = usernamePrepend + baseFileName;
        DocumentFile saveFile = downloadDir.findFile(fileName);
        if (saveFile == null) {
            final String mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(extension.startsWith(".") ? extension.substring(1) : extension);
            if (mimeType == null) return;
            saveFile = downloadDir.createFile(mimeType, fileName);
        }
        // final File saveFile = new File(downloadDir, fileName);
        download(context, url, saveFile);
    }

    public static void download(@NonNull final Context context,
                                @NonNull final Media feedModel) {
        download(context, feedModel, -1);
    }

    public static void download(@NonNull final Context context,
                                @NonNull final Media feedModel,
                                final int position) {
        download(context, Collections.singletonList(feedModel), position);
    }

    public static void download(@NonNull final Context context,
                                @NonNull final List<Media> feedModels) {
        download(context, feedModels, -1);
    }

    private static void download(@NonNull final Context context,
                                 @NonNull final List<Media> feedModels,
                                 final int childPositionIfSingle) {
        final Map<String, DocumentFile> map = new HashMap<>();
        for (final Media media : feedModels) {
            final User mediaUser = media.getUser();
            final String username = mediaUser == null ? "" : mediaUser.getUsername();
            final List<String> userFolderPaths = getSubPathForUserFolder(username);
            // final DocumentFile downloadDir = getDownloadDir(context, mediaUser == null ? "" : mediaUser.getUsername());
            switch (media.getMediaType()) {
                case MEDIA_TYPE_IMAGE:
                case MEDIA_TYPE_VIDEO: {
                    final String url = getUrlOfType(media);
                    String fileName = media.getId();
                    if (mediaUser != null && TextUtils.isEmpty(media.getCode())) {
                        fileName = mediaUser.getUsername() + "_" + fileName;
                    }
                    if (!TextUtils.isEmpty(media.getCode())) {
                        fileName = media.getCode();
                        if (Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_PREPEND_USER_NAME) && mediaUser != null) {
                            fileName = mediaUser.getUsername() + "_" + fileName;
                        }
                    }
                    final Pair<List<String>, String> pair = getDownloadSavePaths(userFolderPaths, fileName, url);
                    final DocumentFile file = createFile(pair);
                    if (file == null) continue;
                    map.put(url, file);
                    break;
                }
                case MEDIA_TYPE_VOICE: {
                    final String url = getUrlOfType(media);
                    String fileName = media.getId();
                    if (mediaUser != null) {
                        fileName = mediaUser.getUsername() + "_" + fileName;
                    }
                    final Pair<List<String>, String> pair = getDownloadSavePaths(userFolderPaths, fileName, url);
                    final DocumentFile file = createFile(pair);
                    if (file == null) continue;
                    map.put(url, file);
                    break;
                }
                case MEDIA_TYPE_SLIDER:
                    final List<Media> sliderItems = media.getCarouselMedia();
                    for (int i = 0; i < sliderItems.size(); i++) {
                        if (childPositionIfSingle >= 0 && feedModels.size() == 1 && i != childPositionIfSingle) continue;
                        final Media child = sliderItems.get(i);
                        final String url = getUrlOfType(child);
                        final String usernamePrepend = Utils.settingsHelper.getBoolean(Constants.DOWNLOAD_PREPEND_USER_NAME) && mediaUser != null
                                                       ? mediaUser.getUsername()
                                                       : "";
                        final Pair<List<String>, String> pair = getDownloadChildSavePaths(
                                new ArrayList<>(userFolderPaths), media.getCode(), i + 1, url, usernamePrepend);
                        final DocumentFile file = createFile(pair);
                        if (file == null) continue;
                        map.put(url, file);
                    }
                    break;
                default:
            }
        }
        if (map.isEmpty()) return;
        download(context, map);
    }

    @Nullable
    private static DocumentFile createFile(@NonNull final Pair<List<String>, String> pair) {
        if (root == null) return null;
        if (pair.first == null || pair.second == null) return null;
        DocumentFile dir = root;
        final List<String> first = pair.first;
        for (int i = 0; i < first.size(); i++) {
            final String name = first.get(i);
            final DocumentFile file = dir.findFile(name);
            if (file != null) {
                dir = file;
                continue;
            }
            dir = i == first.size() - 1 ? dir.createFile(pair.second, name) : dir.createDirectory(name);
            if (dir == null) break;
        }
        return dir;
    }

    @Nullable
    private static String getUrlOfType(@NonNull final Media media) {
        switch (media.getMediaType()) {
            case MEDIA_TYPE_IMAGE: {
                return ResponseBodyUtils.getImageUrl(media);
            }
            case MEDIA_TYPE_VIDEO: {
                final List<VideoVersion> videoVersions = media.getVideoVersions();
                String url = null;
                if (videoVersions != null && !videoVersions.isEmpty()) {
                    final VideoVersion videoVersion = videoVersions.get(0);
                    if (videoVersion != null) {
                        url = videoVersion.getUrl();
                    }
                }
                return url;
            }
            case MEDIA_TYPE_VOICE: {
                final Audio audio = media.getAudio();
                String url = null;
                if (audio != null) {
                    url = audio.getAudioSrc();
                }
                return url;
            }
        }
        return null;
    }

    public static void download(final Context context,
                                final String url,
                                final DocumentFile filePath) {
        if (context == null || url == null || filePath == null) return;
        download(context, Collections.singletonMap(url, filePath));
    }

    private static void download(final Context context, final Map<String, DocumentFile> urlFilePathMap) {
        if (context == null) return;
        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        final DownloadWorker.DownloadRequest request = DownloadWorker.DownloadRequest.builder()
                                                                                     .setUrlToFilePathMap(urlFilePathMap)
                                                                                     .build();
        final String requestJson = new Gson().toJson(request);
        final DocumentFile tempFile = getTempFile(null, "json");
        if (tempFile == null) {
            Log.e(TAG, "download: temp file is null");
            return;
        }
        final Uri uri = tempFile.getUri();
        final ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver == null) return;
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(contentResolver.openOutputStream(uri)))) {
            writer.write(requestJson);
        } catch (IOException e) {
            Log.e(TAG, "download: Error writing request to file", e);
            tempFile.delete();
            return;
        }
        final WorkRequest downloadWorkRequest = new OneTimeWorkRequest.Builder(DownloadWorker.class)
                .setInputData(
                        new Data.Builder()
                                .putString(DownloadWorker.KEY_DOWNLOAD_REQUEST_JSON, tempFile.getUri().toString())
                                .build()
                )
                .setConstraints(constraints)
                .addTag("download")
                .build();
        WorkManager.getInstance(context)
                   .enqueue(downloadWorkRequest);
    }

    public static class ReselectDocumentTreeException extends Exception {
        private final Uri initialUri;

        public ReselectDocumentTreeException() {
            initialUri = null;
        }

        public ReselectDocumentTreeException(final Uri initialUri) {
            this.initialUri = initialUri;
        }

        public Uri getInitialUri() {
            return initialUri;
        }
    }
}
