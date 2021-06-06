package awais.instagrabber.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {
    @Test
    fun testNoUsernameNoCurrentUser() {
        val state = SavedStateHandle(mutableMapOf<String, Any>(
            "username" to ""
        ))
        val viewModel = ProfileFragmentViewModel(state)

    }
}