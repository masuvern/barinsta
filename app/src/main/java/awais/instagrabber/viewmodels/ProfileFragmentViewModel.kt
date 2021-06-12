package awais.instagrabber.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.webservices.FriendshipRepository
import awais.instagrabber.webservices.MediaRepository
import awais.instagrabber.webservices.StoriesRepository
import awais.instagrabber.webservices.UserRepository

class ProfileFragmentViewModel(
    state: SavedStateHandle,
    userRepository: UserRepository,
    friendshipRepository: FriendshipRepository,
    storiesRepository: StoriesRepository,
    mediaRepository: MediaRepository,
) : ViewModel() {
    private val _profile = MutableLiveData<User?>()
    val profile: LiveData<User?> = _profile
    val username: LiveData<String> = Transformations.map(profile) { return@map it?.username ?: "" }

    var currentUser: User? = null
    var isLoggedIn = false
        get() = currentUser != null
        private set

    init {
        Log.d(TAG, "${state.keys()} $userRepository $friendshipRepository $storiesRepository $mediaRepository")
    }
}

@Suppress("UNCHECKED_CAST")
class ProfileFragmentViewModelFactory(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val storiesRepository: StoriesRepository,
    private val mediaRepository: MediaRepository,
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
        ) as T
    }
}
