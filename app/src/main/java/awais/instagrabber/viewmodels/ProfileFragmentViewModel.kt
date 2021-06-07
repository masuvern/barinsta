package awais.instagrabber.viewmodels

import androidx.lifecycle.*
import awais.instagrabber.repositories.responses.User

class ProfileFragmentViewModel(
    state: SavedStateHandle,
) : ViewModel() {
    private val _profile = MutableLiveData<User?>()
    val profile: LiveData<User?> = _profile
    val username: LiveData<String> = Transformations.map(profile) { return@map it?.username ?: "" }

    var currentUser: User? = null
    var isLoggedIn = false
        get() = currentUser != null
        private set

    init {
        // Log.d(TAG, state.keys().toString())
    }
}
