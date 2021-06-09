package awais.instagrabber.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import awais.instagrabber.repositories.UserService
import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.UserSearchResponse
import awais.instagrabber.repositories.responses.WrappedUser
import awais.instagrabber.webservices.UserRepository
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {
    private val userService = object: UserService {
        override suspend fun getUserInfo(uid: Long): WrappedUser {
            TODO("Not yet implemented")
        }

        override suspend fun getUsernameInfo(username: String): WrappedUser {
            TODO("Not yet implemented")
        }

        override suspend fun getUserFriendship(uid: Long): FriendshipStatus {
            TODO("Not yet implemented")
        }

        override suspend fun search(timezoneOffset: Float, query: String): UserSearchResponse {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun testNoUsernameNoCurrentUser() {
        val state = SavedStateHandle(mutableMapOf<String, Any>(
            "username" to ""
        ))
        val userRepository = UserRepository(userService)
        val viewModel = ProfileFragmentViewModel(state, userRepository)
    }
}