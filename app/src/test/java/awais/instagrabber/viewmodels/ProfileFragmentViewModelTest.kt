package awais.instagrabber.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import awais.instagrabber.common.*
import awais.instagrabber.db.datasources.AccountDataSource
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.repositories.AccountRepository
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.getOrAwaitValue
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.webservices.*
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testNoUsernameNoCurrentUser() {
        val accountDataSource = AccountDataSource(AccountDaoAdapter())
        val viewModel = ProfileFragmentViewModel(
            SavedStateHandle(),
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            AccountRepository(accountDataSource),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter()))
        )
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        assertNull(viewModel.profile.getOrAwaitValue().data)
        assertEquals("", viewModel.username.getOrAwaitValue())
        viewModel.currentUser = Resource.success(null)
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
    }

    @Test
    fun testNoUsernameWithCurrentUser() {
        // val state = SavedStateHandle(
        //     mutableMapOf<String, Any?>(
        //         "username" to "test"
        //     )
        // )
        val userRepository = UserRepository(UserServiceAdapter())
        val friendshipRepository = FriendshipRepository(FriendshipServiceAdapter())
        val storiesRepository = StoriesRepository(StoriesServiceAdapter())
        val mediaRepository = MediaRepository(MediaServiceAdapter())
        val graphQLRepository = GraphQLRepository(GraphQLServiceAdapter())
        val accountDataSource = AccountDataSource(AccountDaoAdapter())
        val accountRepository = AccountRepository(accountDataSource)
        val favoriteRepository = FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter()))
        val viewModel = ProfileFragmentViewModel(
            SavedStateHandle(),
            userRepository,
            friendshipRepository,
            storiesRepository,
            mediaRepository,
            graphQLRepository,
            accountRepository,
            favoriteRepository
        )
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        assertNull(viewModel.profile.getOrAwaitValue().data)
        val user = User()
        viewModel.currentUser = Resource.success(user)
        assertEquals(true, viewModel.isLoggedIn.getOrAwaitValue())
        assertEquals(user, viewModel.profile.getOrAwaitValue().data)
    }
}