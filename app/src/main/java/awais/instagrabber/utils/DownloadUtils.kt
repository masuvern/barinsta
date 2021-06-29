package awais.instagrabber.utils

import android.content.Context
import android.content.DialogInterface
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import awais.instagrabber.R
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.models.StoryModel
import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.workers.DownloadWorker
import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import java.util.regex.Pattern


object DownloadUtils {
    private val TAG = DownloadUtils::class.java.simpleName

    // private static final String DIR_BARINSTA = "Barinsta";
    private const val DIR_DOWNLOADS = "Downloads"
    private const val DIR_CAMERA = "Camera"
    private const val DIR_EDIT = "Edit"
    private const val DIR_RECORDINGS = "Sent Recordings"
    private const val DIR_TEMP = "Temp"
    private const val DIR_BACKUPS = "Backups"
    private var root: DocumentFile? = null
    @JvmStatic
    @Throws(ReselectDocumentTreeException::class)
    fun init(
        context: Context,
        barinstaDirUri: String?
    ) {
        if (isEmpty(barinstaDirUri)) {
            throw ReselectDocumentTreeException("folder path is null or empty")
        }
        val uri = Uri.parse(barinstaDirUri)
        if (!barinstaDirUri!!.startsWith("content://com.android.externalstorage.documents")) {
            // reselect the folder in selector view
            throw ReselectDocumentTreeException(uri)
        }
        val existingPermissions = context.contentResolver.persistedUriPermissions
        if (existingPermissions.isEmpty()) {
            // reselect the folder in selector view
            throw ReselectDocumentTreeException(uri)
        }
        val anyMatch = existingPermissions.stream()
            .anyMatch { uriPermission: UriPermission -> uriPermission.uri == uri }
        if (!anyMatch) {
            // reselect the folder in selector view
            throw ReselectDocumentTreeException(uri)
        }
        root = DocumentFile.fromTreeUri(context, uri)
        if (root == null || !root!!.exists() || root!!.lastModified() == 0L) {
            root = null
            throw ReselectDocumentTreeException(uri)
        }
        Utils.settingsHelper.putString(PreferenceKeys.PREF_BARINSTA_DIR_URI, uri.toString())
    }

    fun destroy() {
        root = null
    }

    fun getDownloadDir(vararg dirs: String?): DocumentFile? {
        if (root == null) {
            return null
        }
        var subDir = root
        for (dir in dirs) {
            if (subDir == null || isEmpty(dir)) continue
            val subDirFile = subDir.findFile(dir!!)
            val exists = subDirFile != null && subDirFile.exists()
            subDir = if (exists) subDirFile else subDir.createDirectory(dir)
        }
        return subDir
    }

    @JvmStatic
    val downloadDir: DocumentFile?
        get() = getDownloadDir(DIR_DOWNLOADS)

    @JvmStatic
    fun getCameraDir(): DocumentFile? {
        return getDownloadDir(DIR_CAMERA)
    }

    @JvmStatic
    fun getImageEditDir(sessionId: String?): DocumentFile? {
        return getDownloadDir(DIR_EDIT, sessionId)
    }

    fun getRecordingsDir(): DocumentFile? {
        return getDownloadDir(DIR_RECORDINGS)
    }

    @JvmStatic
    fun getBackupsDir(): DocumentFile? {
        return getDownloadDir(DIR_BACKUPS)
    }

    // @Nullable
    // private static DocumentFile getDownloadDir(@NonNull final Context context, @Nullable final String username) {
    //     return getDownloadDir(context, username, false);
    // }
    private fun getDownloadDir(
        context: Context?,
        username: String?
    ): DocumentFile? {
        val userFolderPaths: List<String> = getSubPathForUserFolder(username)
        var dir = root
        for (dirName in userFolderPaths) {
            val file = dir!!.findFile(dirName)
            if (file != null) {
                dir = file
                continue
            }
            dir = dir.createDirectory(dirName)
            if (dir == null) break
        }
        // final String joined = android.text.TextUtils.join("/", userFolderPaths);
        // final Uri userFolderUri = DocumentsContract.buildDocumentUriUsingTree(root.getUri(), joined);
        // final DocumentFile userFolder = DocumentFile.fromSingleUri(context, userFolderUri);
        if (context != null && (dir == null || !dir.exists())) {
            Toast.makeText(context, R.string.error_creating_folders, Toast.LENGTH_SHORT).show()
            return null
        }
        return dir
    }

    private fun getSubPathForUserFolder(username: String?): MutableList<String> {
        val list: MutableList<String> = ArrayList()
        if (!Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_USER_FOLDER) ||
                username.isNullOrEmpty()) {
            list.add(DIR_DOWNLOADS)
            return list
        }
        val finalUsername = if (username.startsWith("@")) username.substring(1) else username
        list.add(DIR_DOWNLOADS)
        list.add(finalUsername)
        return list
    }

    private fun getTempDir(): DocumentFile? {
        var file = root!!.findFile(DIR_TEMP)
        if (file == null) {
            file = root!!.createDirectory(DIR_TEMP)
        }
        return file
    }

    private fun getDownloadSavePaths(
        paths: MutableList<String>,
        postId: String?,
        displayUrl: String?
    ): Pair<List<String>, String?>? {
        return getDownloadSavePaths(paths, postId, "", displayUrl, "")
    }

    private fun getDownloadSavePaths(
        paths: MutableList<String>,
        postId: String?,
        displayUrl: String,
        username: String
    ): Pair<List<String>, String?>? {
        return getDownloadSavePaths(paths, postId, "", displayUrl, username)
    }

    private fun getDownloadChildSavePaths(
        paths: MutableList<String>,
        postId: String?,
        childPosition: Int,
        url: String?,
        username: String
    ): Pair<List<String>, String?>? {
        val sliderPostfix = "_slide_$childPosition"
        return getDownloadSavePaths(paths, postId, sliderPostfix, url, username)
    }

    private fun getDownloadSavePaths(
        paths: MutableList<String>?,
        postId: String?,
        sliderPostfix: String,
        displayUrl: String?,
        username: String
    ): Pair<List<String>, String?>? {
        if (paths == null) return null
        val extension = getFileExtensionFromUrl(displayUrl)
        val usernamePrepend = if (isEmpty(username)) "" else username + "_"
        val fileName = usernamePrepend + postId + sliderPostfix + extension
        // return new File(finalDir, fileName);
        // DocumentFile file = finalDir.findFile(fileName);
        // if (file == null) {
        val mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(
            if (extension.startsWith(".")) extension.substring(1) else extension
        )
        // file = finalDir.createFile(mimeType, fileName);
        // }
        paths.add(fileName)
        return Pair(paths, mimeType)
    }

    // public static DocumentFile getTempFile() {
    //     return getTempFile(null, null);
    // }
    fun getTempFile(fileName: String?, extension: String): DocumentFile? {
        val dir = getTempDir()
        var name = fileName
        if (isEmpty(name)) {
            name = UUID.randomUUID().toString()
        }
        var mimeType: String? = "application/octet-stream"
        if (!isEmpty(extension)) {
            name += ".$extension"
            val mimeType1 = Utils.mimeTypeMap.getMimeTypeFromExtension(extension)
            if (mimeType1 != null) {
                mimeType = mimeType1
            }
        }
        var file = dir!!.findFile(name!!)
        if (file == null) {
            file = dir.createFile(mimeType!!, name)
        }
        return file
    }

    /**
     * Copied from [MimeTypeMap.getFileExtensionFromUrl])
     *
     *
     * Returns the file extension or an empty string if there is no
     * extension. This method is a convenience method for obtaining the
     * extension of a url and has undefined results for other Strings.
     *
     * @param url URL
     * @return The file extension of the given url.
     */
    @JvmStatic
    fun getFileExtensionFromUrl(url: String?): String {
        var url = url
        if (!isEmpty(url)) {
            val fragment = url!!.lastIndexOf('#')
            if (fragment > 0) {
                url = url.substring(0, fragment)
            }
            val query = url.lastIndexOf('?')
            if (query > 0) {
                url = url.substring(0, query)
            }
            val filenamePos = url.lastIndexOf('/')
            val filename = if (0 <= filenamePos) url.substring(filenamePos + 1) else url

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() &&
                Pattern.matches("[a-zA-Z_0-9.\\-()%]+", filename)
            ) {
                val dotPos = filename.lastIndexOf('.')
                if (0 <= dotPos) {
                    return filename.substring(dotPos)
                }
            }
        }
        return ""
    }

    @JvmStatic
    fun checkDownloaded(media: Media, context: Context): List<Boolean> {
        val checkList: MutableList<Boolean> = LinkedList()
        val user = media.user
        var username = "username"
        if (user != null) {
            username = user.username
        }
        val userFolderPaths: List<String> = getSubPathForUserFolder(username)
        when (media.mediaType) {
            MediaItemType.MEDIA_TYPE_IMAGE, MediaItemType.MEDIA_TYPE_VIDEO -> {
                val url =
                    if (media.mediaType == MediaItemType.MEDIA_TYPE_VIDEO) ResponseBodyUtils.getVideoUrl(
                        media
                    ) else ResponseBodyUtils.getImageUrl(media)
                val file = getDownloadSavePaths(ArrayList(userFolderPaths), media.code, url, "")
                val fileExists = file!!.first != null && checkPathExists(file.first, context)
                var usernameFileExists = false
                if (!fileExists) {
                    val usernameFile = getDownloadSavePaths(
                        ArrayList(userFolderPaths), media.code, url, username
                    )
                    usernameFileExists = usernameFile!!.first != null && checkPathExists(usernameFile.first, context)
                }
                checkList.add(fileExists || usernameFileExists)
            }
            MediaItemType.MEDIA_TYPE_SLIDER -> {
                val sliderItems = media.carouselMedia
                var i = 0
                while (i < sliderItems!!.size) {
                    val child = sliderItems[i]
                    val url =
                        if (child.mediaType == MediaItemType.MEDIA_TYPE_VIDEO) ResponseBodyUtils.getVideoUrl(
                            child
                        ) else ResponseBodyUtils.getImageUrl(child)
                    val file = getDownloadChildSavePaths(
                        ArrayList(userFolderPaths), media.code, i + 1, url, ""
                    )
                    val fileExists = file!!.first != null && checkPathExists(file.first, context)
                    var usernameFileExists = false
                    if (!fileExists) {
                        val usernameFile = getDownloadChildSavePaths(
                            ArrayList(userFolderPaths), media.code, i + 1, url, username
                        )
                        usernameFileExists = usernameFile!!.first != null && checkPathExists(usernameFile.first, context)
                    }
                    checkList.add(fileExists || usernameFileExists)
                    i++
                }
            }
            else -> {
            }
        }
        return checkList
    }

    private fun checkPathExists(paths: List<String>, context: Context): Boolean {
        if (root == null) return false
        val uri = root!!.uri
        var found = false
        var docId = DocumentsContract.getTreeDocumentId(uri)
        for (path in paths) {
            val docUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
            val docCursor = context.contentResolver.query(
                docUri, arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                ), null, null, null
            )
            if (docCursor == null) return false
            while (docCursor.moveToNext() && !found) {
                if (path.equals(docCursor.getString(0))) {
                    docId = docCursor.getString(1)
                    found = true
                }
            }
            docCursor.close()
            if (!found) return false
            found = false
        }
        return true
    }

    @JvmStatic
    fun showDownloadDialog(
        context: Context,
        feedModel: Media,
        childPosition: Int
    ) {
        if (childPosition >= 0) {
            val clickListener =
                DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                    when (which) {
                        0 -> download(context, feedModel, childPosition)
                        1 -> download(context, feedModel)
                        DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                        else -> dialog.dismiss()
                    }
                }
            val items = arrayOf(
                context.getString(R.string.post_viewer_download_current),
                context.getString(R.string.post_viewer_download_album)
            )
            AlertDialog.Builder(context)
                .setTitle(R.string.post_viewer_download_dialog_title)
                .setItems(items, clickListener)
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }
        download(context, feedModel)
    }

    @JvmStatic
    fun download(
        context: Context,
        storyModel: StoryModel
    ) {
        val downloadDir = getDownloadDir(context, storyModel.username) ?: return
        val url =
            if (storyModel.itemType == MediaItemType.MEDIA_TYPE_VIDEO) storyModel.videoUrl else storyModel.storyUrl
        val extension = getFileExtensionFromUrl(url)
        val baseFileName = (storyModel.storyMediaId + "_"
                + storyModel.timestamp + extension)
        val usernamePrepend =
            if (Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME)
                && storyModel.username != null
            ) storyModel.username + "_" else ""
        val fileName = usernamePrepend + baseFileName
        var saveFile = downloadDir.findFile(fileName)
        if (saveFile == null) {
            val mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(
                if (extension.startsWith(".")) extension.substring(1) else extension
            )
                ?: return
            saveFile = downloadDir.createFile(mimeType, fileName)
        }
        // final File saveFile = new File(downloadDir, fileName);
        download(context, url, saveFile)
    }

    @JvmOverloads
    @JvmStatic
    fun download(
        context: Context,
        feedModel: Media,
        position: Int = -1
    ) {
        download(context, listOf(feedModel), position)
    }

    @JvmStatic
    fun download(
        context: Context,
        feedModels: List<Media>
    ) {
        download(context, feedModels, -1)
    }

    private fun download(
        context: Context,
        feedModels: List<Media>,
        childPositionIfSingle: Int
    ) {
        val map: MutableMap<String, DocumentFile> = HashMap()
        for (media in feedModels) {
            val mediaUser = media.user
            val username = mediaUser?.username ?: ""
            val userFolderPaths = getSubPathForUserFolder(username)
            when (media.mediaType) {
                MediaItemType.MEDIA_TYPE_IMAGE, MediaItemType.MEDIA_TYPE_VIDEO -> {
                    val url = getUrlOfType(media)
                    var fileName = media.id
                    if (mediaUser != null && isEmpty(media.code)) {
                        fileName = mediaUser.username + "_" + fileName
                    }
                    if (!isEmpty(media.code)) {
                        fileName = media.code
                        if (Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME) && mediaUser != null) {
                            fileName = mediaUser.username + "_" + fileName
                        }
                    }
                    val pair = getDownloadSavePaths(userFolderPaths, fileName, url)
                    val file = createFile(pair!!) ?: continue
                    map[url!!] = file
                }
                MediaItemType.MEDIA_TYPE_VOICE -> {
                    val url = getUrlOfType(media)
                    var fileName = media.id
                    if (mediaUser != null) {
                        fileName = mediaUser.username + "_" + fileName
                    }
                    val pair = getDownloadSavePaths(userFolderPaths, fileName, url)
                    val file = createFile(pair!!) ?: continue
                    map[url!!] = file
                }
                MediaItemType.MEDIA_TYPE_SLIDER -> {
                    val sliderItems = media.carouselMedia
                    var i = 0
                    while (i < sliderItems!!.size) {
                        if (childPositionIfSingle >= 0 && feedModels.size == 1 && i != childPositionIfSingle) {
                            i++
                            continue
                        }
                        val child = sliderItems[i]
                        val url = getUrlOfType(child)
                        val usernamePrepend =
                            if (Utils.settingsHelper.getBoolean(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME) && mediaUser != null) mediaUser.username else ""
                        val pair = getDownloadChildSavePaths(
                            ArrayList(userFolderPaths), media.code, i + 1, url, usernamePrepend
                        )
                        val file = createFile(pair!!)
                        if (file == null) {
                            i++
                            continue
                        }
                        map[url!!] = file
                        i++
                    }
                }
            }
        }
        if (map.isEmpty()) return
        download(context, map)
    }

    private fun createFile(pair: Pair<List<String>, String?>): DocumentFile? {
        if (root == null) return null
        if (pair.first == null || pair.second == null) return null
        var dir = root
        val first = pair.first
        for (i in first.indices) {
            val name = first[i]
            val file = dir!!.findFile(name)
            if (file != null) {
                dir = file
                continue
            }
            dir = if (i == first.size - 1) dir.createFile(
                pair.second!!,
                name
            ) else dir.createDirectory(name)
            if (dir == null) break
        }
        return dir
    }

    private fun getUrlOfType(media: Media): String? {
        when (media.mediaType) {
            MediaItemType.MEDIA_TYPE_IMAGE -> {
                return ResponseBodyUtils.getImageUrl(media)
            }
            MediaItemType.MEDIA_TYPE_VIDEO -> {
                val videoVersions = media.videoVersions
                var url: String? = null
                if (videoVersions != null && !videoVersions.isEmpty()) {
                    url = videoVersions[0].url
                }
                return url
            }
            MediaItemType.MEDIA_TYPE_VOICE -> {
                val audio = media.audio
                var url: String? = null
                if (audio != null) {
                    url = audio.audioSrc
                }
                return url
            }
        }
        return null
    }

    @JvmStatic
    fun download(
        context: Context?,
        url: String?,
        filePath: DocumentFile?
    ) {
        if (context == null || filePath == null) return
        download(context, Collections.singletonMap(url!!, filePath))
    }

    private fun download(context: Context?, urlFilePathMap: Map<String, DocumentFile>) {
        if (context == null) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = DownloadWorker.DownloadRequest.builder()
            .setUrlToFilePathMap(urlFilePathMap)
            .build()
        val requestJson = Gson().toJson(request)
        val tempFile = getTempFile(null, "json")
        if (tempFile == null) {
            Log.e(TAG, "download: temp file is null")
            return
        }
        val uri = tempFile.uri
        val contentResolver = context.contentResolver ?: return
        try {
            BufferedWriter(OutputStreamWriter(contentResolver.openOutputStream(uri))).use { writer ->
                writer.write(
                    requestJson
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "download: Error writing request to file", e)
            tempFile.delete()
            return
        }
        val downloadWorkRequest: WorkRequest =
            OneTimeWorkRequest.Builder(DownloadWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(
                            DownloadWorker.KEY_DOWNLOAD_REQUEST_JSON,
                            tempFile.uri.toString()
                        )
                        .build()
                )
                .setConstraints(constraints)
                .addTag("download")
                .build()
        WorkManager.getInstance(context)
            .enqueue(downloadWorkRequest)
    }

    @JvmStatic
    fun getRootDirUri(): Uri? {
        return if (root != null) root!!.uri else null
    }

    class ReselectDocumentTreeException : Exception {
        val initialUri: Uri?

        constructor() {
            initialUri = null
        }

        constructor(message: String?) : super(message) {
            initialUri = null
        }

        constructor(initialUri: Uri?) {
            this.initialUri = initialUri
        }
    }
}