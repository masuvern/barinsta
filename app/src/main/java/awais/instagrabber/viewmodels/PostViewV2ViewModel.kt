package awais.instagrabber.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import awais.instagrabber.R
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.responses.Caption
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.utils.getCsrfTokenFromCookie
import awais.instagrabber.utils.getUserIdFromCookie
import awais.instagrabber.webservices.MediaService
import awais.instagrabber.webservices.ServiceCallback
import com.google.common.collect.ImmutableList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class PostViewV2ViewModel : ViewModel() {
    private val user = MutableLiveData<User?>()
    private val caption = MutableLiveData<Caption?>()
    private val location = MutableLiveData<Location?>()
    private val date = MutableLiveData<String>()
    private val likeCount = MutableLiveData(0L)
    private val commentCount = MutableLiveData(0L)
    private val viewCount = MutableLiveData(0L)
    private val type = MutableLiveData<MediaItemType?>()
    private val liked = MutableLiveData(false)
    private val saved = MutableLiveData(false)
    private val options = MutableLiveData<List<Int>>(ArrayList())
    private val viewerId: Long
    val isLoggedIn: Boolean
    lateinit var media: Media
        private set
    private var mediaService: MediaService? = null
    private var messageManager: DirectMessagesManager? = null

    fun setMedia(media: Media) {
        this.media = media
        user.postValue(media.user)
        caption.postValue(media.caption)
        location.postValue(media.location)
        date.postValue(media.date)
        likeCount.postValue(media.likeCount)
        commentCount.postValue(media.commentCount)
        viewCount.postValue(if (media.mediaType == MediaItemType.MEDIA_TYPE_VIDEO) media.viewCount else null)
        type.postValue(media.mediaType)
        liked.postValue(media.hasLiked)
        saved.postValue(media.hasViewerSaved)
        initOptions()
    }

    private fun initOptions() {
        val builder = ImmutableList.builder<Int>()
        val user1 = media.user
        if (isLoggedIn && user1 != null && user1.pk == viewerId) {
            builder.add(R.id.edit_caption)
            builder.add(R.id.delete)
        }
        options.postValue(builder.build())
    }

    fun getUser(): LiveData<User?> {
        return user
    }

    fun getCaption(): LiveData<Caption?> {
        return caption
    }

    fun getLocation(): LiveData<Location?> {
        return location
    }

    fun getDate(): LiveData<String> {
        return date
    }

    fun getLikeCount(): LiveData<Long> {
        return likeCount
    }

    fun getCommentCount(): LiveData<Long> {
        return commentCount
    }

    fun getViewCount(): LiveData<Long?> {
        return viewCount
    }

    fun getType(): LiveData<MediaItemType?> {
        return type
    }

    fun getLiked(): LiveData<Boolean> {
        return liked
    }

    fun getSaved(): LiveData<Boolean> {
        return saved
    }

    fun getOptions(): LiveData<List<Int>> {
        return options
    }

    fun toggleLike(): LiveData<Resource<Any?>> {
        return if (media.hasLiked) {
            unlike()
        } else like()
    }

    fun like(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        mediaService?.like(media.pk, getLikeUnlikeCallback(data))
        return data
    }

    fun unlike(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        mediaService?.unlike(media.pk, getLikeUnlikeCallback(data))
        return data
    }

    private fun getLikeUnlikeCallback(data: MutableLiveData<Resource<Any?>>): ServiceCallback<Boolean?> {
        return object : ServiceCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                if (result != null && !result) {
                    data.postValue(error("", null))
                    return
                }
                data.postValue(success(true))
                val currentLikesCount = media.likeCount
                val updatedCount: Long
                if (!media.hasLiked) {
                    updatedCount = currentLikesCount + 1
                    media.hasLiked = true
                } else {
                    updatedCount = currentLikesCount - 1
                    media.hasLiked = false
                }
                media.likeCount = updatedCount
                likeCount.postValue(updatedCount)
                liked.postValue(media.hasLiked)
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, null))
                Log.e(TAG, "Error during like/unlike", t)
            }
        }
    }

    fun toggleSave(): LiveData<Resource<Any?>> {
        return if (!media.hasViewerSaved) {
            save(null, false)
        } else unsave()
    }

    fun toggleSave(collection: String?, ignoreSaveState: Boolean): LiveData<Resource<Any?>> {
        return save(collection, ignoreSaveState)
    }

    fun save(collection: String?, ignoreSaveState: Boolean): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        mediaService?.save(media.pk, collection, getSaveUnsaveCallback(data, ignoreSaveState))
        return data
    }

    fun unsave(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        mediaService?.unsave(media.pk, getSaveUnsaveCallback(data, false))
        return data
    }

    private fun getSaveUnsaveCallback(
        data: MutableLiveData<Resource<Any?>>,
        ignoreSaveState: Boolean,
    ): ServiceCallback<Boolean?> {
        return object : ServiceCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                if (result != null && !result) {
                    data.postValue(error("", null))
                    return
                }
                data.postValue(success(true))
                if (!ignoreSaveState) media.hasViewerSaved = !media.hasViewerSaved
                saved.postValue(media.hasViewerSaved)
            }

            override fun onFailure(t: Throwable) {
                data.postValue(error(t.message, null))
                Log.e(TAG, "Error during save/unsave", t)
            }
        }
    }

    fun updateCaption(caption: String): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        mediaService?.editCaption(media.pk, caption, object : ServiceCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                if (result != null && result) {
                    data.postValue(success(""))
                    media.setPostCaption(caption)
                    this@PostViewV2ViewModel.caption.postValue(media.caption)
                    return
                }
                data.postValue(error("", null))
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "Error editing caption", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun translateCaption(): LiveData<Resource<String?>> {
        val data = MutableLiveData<Resource<String?>>()
        data.postValue(loading(null))
        val value = caption.value ?: return data
        mediaService?.translate(value.pk, "1", object : ServiceCallback<String?> {
            override fun onSuccess(result: String?) {
                if (result.isNullOrBlank()) {
                    data.postValue(error("", null))
                    return
                }
                data.postValue(success(result))
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "Error translating comment", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun hasPk(): Boolean {
        return media.pk != null
    }

    fun setViewCount(viewCount: Long?) {
        this.viewCount.postValue(viewCount)
    }

    fun delete(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        val mediaId = media.id
        val mediaType = media.mediaType
        if (mediaId == null || mediaType == null) {
            data.postValue(error("media id or type is null", null))
            return data
        }
        val request = mediaService?.delete(mediaId, mediaType)
        if (request == null) {
            data.postValue(success(Any()))
            return data
        }
        request.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (!response.isSuccessful) {
                    data.postValue(error(R.string.generic_null_response, null))
                    return
                }
                val body = response.body()
                if (body == null) {
                    data.postValue(error(R.string.generic_null_response, null))
                    return
                }
                data.postValue(success(Any()))
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                Log.e(TAG, "onFailure: ", t)
                data.postValue(error(t.message, null))
            }
        })
        return data
    }

    fun shareDm(result: RankedRecipient) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = media.id ?: return
        messageManager?.sendMedia(result, mediaId, viewModelScope)
    }

    fun shareDm(recipients: Set<RankedRecipient>) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = media.id ?: return
        messageManager?.sendMedia(recipients, mediaId, viewModelScope)
    }

    init {
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        val deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
        val csrfToken: String? = getCsrfTokenFromCookie(cookie)
        viewerId = getUserIdFromCookie(cookie)
        isLoggedIn = cookie.isNotBlank() && viewerId != 0L
        if (!csrfToken.isNullOrBlank()) {
            mediaService = MediaService.getInstance(deviceUuid, csrfToken, viewerId)
        }
    }
}