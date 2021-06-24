package awais.instagrabber.viewmodels

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import awais.instagrabber.db.repositories.AccountRepository
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.ControlledRunner
import awais.instagrabber.webservices.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ProfileFragmentViewModel(
    state: SavedStateHandle,
    userRepository: UserRepository,
    friendshipRepository: FriendshipRepository,
    storiesRepository: StoriesRepository,
    mediaRepository: MediaRepository,
    graphQLRepository: GraphQLRepository,
    accountRepository: AccountRepository,
    favoriteRepository: FavoriteRepository,
    ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _currentUser = MutableLiveData<Resource<User?>>(Resource.loading(null))
    private var messageManager: DirectMessagesManager? = null

    val currentUser: LiveData<Resource<User?>> = _currentUser
    val isLoggedIn: LiveData<Boolean> = currentUser.map { it.data != null }

    private val currentUserAndStateUsernameLiveData: LiveData<Pair<Resource<User?>, Resource<String?>>> =
        object : MediatorLiveData<Pair<Resource<User?>, Resource<String?>>>() {
            var user: Resource<User?> = Resource.loading(null)
            var stateUsername: Resource<String?> = Resource.loading(null)

            init {
                addSource(currentUser) { currentUser ->
                    this.user = currentUser
                    value = currentUser to stateUsername
                }
                addSource(state.getLiveData<String?>("username")) { username ->
                    this.stateUsername = Resource.success(username.substringAfter('@'))
                    value = user to this.stateUsername
                }
                // trigger currentUserAndStateUsernameLiveData switch map with a state username success resource
                if (!state.contains("username")) {
                    this.stateUsername = Resource.success(null)
                    value = user to this.stateUsername
                }
            }
        }

    private val profileFetchControlledRunner = ControlledRunner<User?>()
    val profile: LiveData<Resource<User?>> = currentUserAndStateUsernameLiveData.switchMap {
        val (userResource, stateUsernameResource) = it
        liveData<Resource<User?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            if (userResource.status == Resource.Status.LOADING || stateUsernameResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(null))
                return@liveData
            }
            val user = userResource.data
            val stateUsername = stateUsernameResource.data
            if (stateUsername.isNullOrBlank()) {
                emit(Resource.success(user))
                return@liveData
            }
            try {
                val fetchedUser = profileFetchControlledRunner.cancelPreviousThenRun {
                    return@cancelPreviousThenRun if (user != null) {
                        val tempUser = userRepository.getUsernameInfo(stateUsername) // logged in
                        tempUser.friendshipStatus = userRepository.getUserFriendship(tempUser.pk)
                        return@cancelPreviousThenRun tempUser
                    } else {
                        graphQLRepository.fetchUser(stateUsername) // anonymous
                    }
                }
                emit(Resource.success(fetchedUser))
            } catch (e: Exception) {
                emit(Resource.error(e.message, null))
            }
        }
    }

    /**
     * Username of profile without '`@`'
     */
    val username: LiveData<String> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.LOADING, Resource.Status.ERROR -> ""
            Resource.Status.SUCCESS -> it.data?.username ?: ""
        }
    }

    init {
        // Log.d(TAG, "${state.keys()} $userRepository $friendshipRepository $storiesRepository $mediaRepository")
    }

    fun setCurrentUser(currentUser: Resource<User?>) {
        _currentUser.postValue(currentUser)
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
            Dispatchers.IO,
        ) as T
    }
}
