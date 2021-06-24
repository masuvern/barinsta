package awais.instagrabber.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.repositories.AccountRepository
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.HighlightModel
import awais.instagrabber.models.Resource
import awais.instagrabber.models.StoryModel
import awais.instagrabber.models.enums.BroadcastItemType
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.ControlledRunner
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.webservices.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ProfileFragmentViewModel(
    state: SavedStateHandle,
    userRepository: UserRepository,
    friendshipRepository: FriendshipRepository,
    private val storiesRepository: StoriesRepository,
    mediaRepository: MediaRepository,
    graphQLRepository: GraphQLRepository,
    accountRepository: AccountRepository,
    private val favoriteRepository: FavoriteRepository,
    ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _currentUser = MutableLiveData<Resource<User?>>(Resource.loading(null))
    private val _isFavorite = MutableLiveData(false)
    private var messageManager: DirectMessagesManager? = null

    val currentUser: LiveData<Resource<User?>> = _currentUser
    val isLoggedIn: LiveData<Boolean> = currentUser.map { it.data != null }
    val isFavorite: LiveData<Boolean> = _isFavorite

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
        val (currentUserResource, stateUsernameResource) = it
        liveData<Resource<User?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            if (currentUserResource.status == Resource.Status.LOADING || stateUsernameResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(null))
                return@liveData
            }
            val currentUser = currentUserResource.data
            val stateUsername = stateUsernameResource.data
            if (stateUsername.isNullOrBlank()) {
                emit(Resource.success(currentUser))
                return@liveData
            }
            try {
                val fetchedUser = profileFetchControlledRunner.cancelPreviousThenRun {
                    return@cancelPreviousThenRun fetchUser(currentUser, userRepository, stateUsername, graphQLRepository)
                }
                emit(Resource.success(fetchedUser))
                if (fetchedUser != null) {
                    checkAndInsertFavorite(fetchedUser)
                }
            } catch (e: Exception) {
                emit(Resource.error(e.message, null))
                Log.e(TAG, "fetching user: ", e)
            }
        }
    }

    private val storyFetchControlledRunner = ControlledRunner<List<StoryModel>?>()
    val userStories: LiveData<Resource<List<StoryModel>?>> = profile.switchMap { userResource ->
        liveData<Resource<List<StoryModel>?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            // don't fetch if not logged in
            if (isLoggedIn.value != true) {
                emit(Resource.success(null))
                return@liveData
            }
            if (userResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(null))
                return@liveData
            }
            val user = userResource.data
            if (user == null) {
                emit(Resource.success(null))
                return@liveData
            }
            try {
                val fetchedStories = storyFetchControlledRunner.cancelPreviousThenRun { fetchUserStory(user) }
                emit(Resource.success(fetchedStories))
            } catch (e: Exception) {
                emit(Resource.error(e.message, null))
                Log.e(TAG, "fetching story: ", e)
            }
        }
    }

    private val highlightsFetchControlledRunner = ControlledRunner<List<HighlightModel>?>()
    val userHighlights: LiveData<Resource<List<HighlightModel>?>> = profile.switchMap { userResource ->
        liveData<Resource<List<HighlightModel>?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            // don't fetch if not logged in
            if (isLoggedIn.value != true) {
                emit(Resource.success(null))
                return@liveData
            }
            if (userResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(null))
                return@liveData
            }
            val user = userResource.data
            if (user == null) {
                emit(Resource.success(null))
                return@liveData
            }
            try {
                val fetchedHighlights = highlightsFetchControlledRunner.cancelPreviousThenRun { fetchUserHighlights(user) }
                emit(Resource.success(fetchedHighlights))
            } catch (e: Exception) {
                emit(Resource.error(e.message, null))
                Log.e(TAG, "fetching story: ", e)
            }
        }
    }

    private suspend fun fetchUser(
        currentUser: User?,
        userRepository: UserRepository,
        stateUsername: String,
        graphQLRepository: GraphQLRepository
    ) = if (currentUser != null) {
        // logged in
        val tempUser = userRepository.getUsernameInfo(stateUsername)
        tempUser.friendshipStatus = userRepository.getUserFriendship(tempUser.pk)
        tempUser
    } else {
        // anonymous
        graphQLRepository.fetchUser(stateUsername)
    }

    private suspend fun fetchUserStory(fetchedUser: User): List<StoryModel> = storiesRepository.getUserStory(
        StoryViewerOptions.forUser(fetchedUser.pk, fetchedUser.fullName)
    )

    private suspend fun fetchUserHighlights(fetchedUser: User): List<HighlightModel> = storiesRepository.fetchHighlights(fetchedUser.pk)

    private suspend fun checkAndInsertFavorite(fetchedUser: User) {
        try {
            val favorite = favoriteRepository.getFavorite(fetchedUser.username, FavoriteType.USER)
            if (favorite == null) {
                _isFavorite.postValue(false)
                return
            }
            _isFavorite.postValue(true)
            favoriteRepository.insertOrUpdateFavorite(
                Favorite(
                    favorite.id,
                    fetchedUser.username,
                    FavoriteType.USER,
                    fetchedUser.fullName,
                    fetchedUser.profilePicUrl,
                    favorite.dateAdded
                )
            )
        } catch (e: Exception) {
            _isFavorite.postValue(false)
            Log.e(TAG, "checkAndInsertFavorite: ", e)
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
        val mediaId = profile.value?.data?.pk ?: return
        messageManager?.sendMedia(result, mediaId.toString(10), BroadcastItemType.PROFILE, viewModelScope)
    }

    fun shareDm(recipients: Set<RankedRecipient>) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = profile.value?.data?.pk ?: return
        messageManager?.sendMedia(recipients, mediaId.toString(10), BroadcastItemType.PROFILE, viewModelScope)
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
