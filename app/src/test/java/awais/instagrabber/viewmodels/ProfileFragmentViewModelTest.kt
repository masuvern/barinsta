package awais.instagrabber.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import awais.instagrabber.MainCoroutineScopeRule
import awais.instagrabber.common.*
import awais.instagrabber.db.datasources.AccountDataSource
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.repositories.AccountRepository
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.getOrAwaitValue
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.webservices.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {

    private val testPublicUser = User(
        pk = 100,
        username = "test",
        fullName = "Test user"
    )

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @ExperimentalCoroutinesApi
    @Test
    fun testNoUsernameNoCurrentUser() {
        val viewModel = ProfileFragmentViewModel(
            SavedStateHandle(),
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            AccountRepository(AccountDataSource(AccountDaoAdapter())),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            coroutineScope.dispatcher,
        )
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        viewModel.setCurrentUser(Resource.success(null))
        assertNull(viewModel.profile.getOrAwaitValue().data)
        assertEquals("", viewModel.username.getOrAwaitValue())
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testNoUsernameWithCurrentUser() {
        val viewModel = ProfileFragmentViewModel(
            SavedStateHandle(),
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            AccountRepository(AccountDataSource(AccountDaoAdapter())),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            coroutineScope.dispatcher,
        )
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        assertNull(viewModel.profile.getOrAwaitValue().data)
        val user = User()
        viewModel.setCurrentUser(Resource.success(user))
        assertEquals(true, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(user, profile.data)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testPublicUsernameWithNoCurrentUser() {
        // username without `@`
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        val graphQLRepository = object : GraphQLRepository(GraphQLServiceAdapter()) {
            override suspend fun fetchUser(username: String): User {
                return testPublicUser
            }
        }
        val viewModel = ProfileFragmentViewModel(
            state,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            graphQLRepository,
            AccountRepository(AccountDataSource(AccountDaoAdapter())),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser, profile.data)
    }
}