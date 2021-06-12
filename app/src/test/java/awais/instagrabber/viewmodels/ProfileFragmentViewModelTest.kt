package awais.instagrabber.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import awais.instagrabber.common.*
import awais.instagrabber.db.datasources.AccountDataSource
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.repositories.AccountRepository
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.webservices.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {
    @Test
    fun testNoUsernameNoCurrentUser() {
        val state = SavedStateHandle(
            mutableMapOf<String, Any>(
                "username" to ""
            )
        )
        val userRepository = UserRepository(UserServiceAdapter())
        val friendshipRepository = FriendshipRepository(FriendshipServiceAdapter())
        val storiesRepository = StoriesRepository(StoriesServiceAdapter())
        val mediaRepository = MediaRepository(MediaServiceAdapter())
        val graphQLRepository = GraphQLRepository(GraphQLServiceAdapter())
        val accountDataSource = AccountDataSource(AccountDaoAdapter())
        val accountRepository = AccountRepository(accountDataSource)
        val favoriteRepository = FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter()))
        val viewModel = ProfileFragmentViewModel(
            state,
            userRepository,
            friendshipRepository,
            storiesRepository,
            mediaRepository,
            graphQLRepository,
            accountRepository,
            favoriteRepository
        )
    }
}