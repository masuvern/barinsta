package awais.instagrabber.viewmodels

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import awais.instagrabber.db.repositories.AccountRepository
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.enums.BroadcastItemType
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.webservices.*

class ProfileFragmentViewModel(
    state: SavedStateHandle,
    userRepository: UserRepository,
    friendshipRepository: FriendshipRepository,
    storiesRepository: StoriesRepository,
    mediaRepository: MediaRepository,
    graphQLRepository: GraphQLRepository,
    accountRepository: AccountRepository,
    favoriteRepository: FavoriteRepository,
) : ViewModel() {
    private val _profile = MutableLiveData<Resource<User?>>(Resource.loading(null))
    private val _isLoggedIn = MutableLiveData(false)
    private var messageManager: DirectMessagesManager? = null

    val profile: LiveData<Resource<User?>> = _profile

    /**
     * Username of profile without '`@`'
     */
    val username: LiveData<String> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.LOADING, Resource.Status.ERROR -> ""
            Resource.Status.SUCCESS -> it.data?.username ?: ""
        }
    }
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    var currentUser: Resource<User?>? = null
        set(value) {
            _isLoggedIn.postValue(value?.data != null)
            // if no profile, and value is valid, set it as profile
            val profileValue = profile.value
            if (
                profileValue?.status != Resource.Status.LOADING
                && profileValue?.data == null
                && value?.status == Resource.Status.SUCCESS
                && value.data != null
            ) {
                _profile.postValue(Resource.success(value.data))
            }
            field = value
        }

    init {
        // Log.d(TAG, "${state.keys()} $userRepository $friendshipRepository $storiesRepository $mediaRepository")
        val usernameFromState = state.get<String?>("username")
        if (usernameFromState.isNullOrBlank()) {
            _profile.postValue(Resource.success(null))
        }
    }

    fun shareDm(result: RankedRecipient) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
//        messageManager?.sendMedia(result, mediaId, BroadcastItemType.PROFILE, viewModelScope)
    }

    fun shareDm(recipients: Set<RankedRecipient>) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
//        messageManager?.sendMedia(recipients, mediaId, BroadcastItemType.PROFILE, viewModelScope)
    }
}

@Suppress("UNCHECKED_CAST")
class ProfileFragmentViewModelFactory(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val storiesRepository: StoriesRepository,
    private val mediaRepository: MediaRepository,
    private val graphQLRepository: GraphQLRepository,
    private val accountRepository: AccountRepository,
    private val favoriteRepository: FavoriteRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        return ProfileFragmentViewModel(
            handle,
            userRepository,
            friendshipRepository,
            storiesRepository,
            mediaRepository,
            graphQLRepository,
            accountRepository,
            favoriteRepository,
        ) as T
    }
}
