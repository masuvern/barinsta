package awais.instagrabber.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.webservices.UserRepository

class ProfileFragmentViewModel(
    state: SavedStateHandle,
    userRepository: UserRepository,
) : ViewModel() {
    private val _profile = MutableLiveData<User?>()
    val profile: LiveData<User?> = _profile
    val username: LiveData<String> = Transformations.map(profile) { return@map it?.username ?: "" }

    var currentUser: User? = null
    var isLoggedIn = false
        get() = currentUser != null
        private set

    init {
        Log.d(TAG, "${state.keys()} $userRepository")
    }
}

@Suppress("UNCHECKED_CAST")
class ProfileFragmentViewModelFactory(
    private val userRepository: UserRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        return ProfileFragmentViewModel(handle, userRepository) as T
    }
}
