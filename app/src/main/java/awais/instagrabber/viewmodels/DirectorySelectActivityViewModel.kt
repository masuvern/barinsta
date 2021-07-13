package awais.instagrabber.viewmodels

import android.app.Application
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Parcelable
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import awais.instagrabber.R
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.DownloadUtils.ReselectDocumentTreeException
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DirectorySelectActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _message = MutableLiveData<String>()
    private val _prevUri = MutableLiveData<String?>()
    private val _loading = MutableLiveData(false)
    private val _dirSuccess = MutableLiveData(false)

    val message: LiveData<String> = _message
    val prevUri: LiveData<String?> = _prevUri
    val loading: LiveData<Boolean> = _loading
    val dirSuccess: LiveData<Boolean> = _dirSuccess

    fun setInitialUri(intent: Intent?) {
        if (intent == null) {
            setMessage(null)
            return
        }
        val initialUriParcelable = intent.getParcelableExtra<Parcelable>(Constants.EXTRA_INITIAL_URI)
        if (initialUriParcelable !is Uri) {
            setMessage(null)
            return
        }
        setMessage(initialUriParcelable as Uri?)
    }

    private fun setMessage(initialUri: Uri?) {
        if (initialUri == null) {
            val prevVersionFolderPath = Utils.settingsHelper.getString(PreferenceKeys.FOLDER_PATH)
            if (isEmpty(prevVersionFolderPath)) {
                // default message
                _message.postValue(getApplication<Application>().getString(R.string.dir_select_default_message))
                _prevUri.postValue(null)
                return
            }
            _message.postValue(getApplication<Application>().getString(R.string.dir_select_reselect_message))
            _prevUri.postValue(prevVersionFolderPath)
            return
        }
        val existingPermissions = getApplication<Application>().contentResolver.persistedUriPermissions
        val anyMatch = existingPermissions.stream().anyMatch { uriPermission: UriPermission -> uriPermission.uri == initialUri }
        val documentFile = DocumentFile.fromSingleUri(getApplication(), initialUri)
        val path: String = try {
            URLDecoder.decode(initialUri.toString(), StandardCharsets.UTF_8.toString())
        } catch (e: UnsupportedEncodingException) {
            initialUri.toString()
        }
        if (!anyMatch) {
            _message.postValue(getApplication<Application>().getString(R.string.dir_select_permission_revoked_message))
            _prevUri.postValue(path)
            return
        }
        if (documentFile == null || !documentFile.exists() || documentFile.lastModified() == 0L) {
            _message.postValue(getApplication<Application>().getString(R.string.dir_select_folder_not_exist))
            _prevUri.postValue(path)
        }
    }

    @Throws(ReselectDocumentTreeException::class)
    fun setupSelectedDir(data: Intent) {
        _loading.postValue(true)
        try {
            Utils.setupSelectedDir(getApplication(), data)
            _message.postValue(getApplication<Application>().getString(R.string.dir_select_success_message))
            _dirSuccess.postValue(true)
        } finally {
            _loading.postValue(false)
        }
    }
}