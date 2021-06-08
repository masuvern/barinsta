package awais.instagrabber.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.UserRepository;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AppStateViewModel extends AndroidViewModel {
    private static final String TAG = AppStateViewModel.class.getSimpleName();

    private final String cookie;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    private UserRepository userRepository;

    public AppStateViewModel(@NonNull final Application application) {
        super(application);
        // Log.d(TAG, "AppStateViewModel: constructor");
        cookie = settingsHelper.getString(Constants.COOKIE);
        final boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != 0;
        if (!isLoggedIn) return;
        userRepository = UserRepository.INSTANCE;
        // final AccountRepository accountRepository = AccountRepository.getInstance(AccountDataSource.getInstance(application));
        fetchProfileDetails();
    }

    @Nullable
    public User getCurrentUser() {
        return currentUser.getValue();
    }

    public LiveData<User> getCurrentUserLiveData() {
        return currentUser;
    }

    private void fetchProfileDetails() {
        final long uid = CookieUtils.getUserIdFromCookie(cookie);
        if (userRepository == null) return;
        userRepository.getUserInfo(uid, CoroutineUtilsKt.getContinuation((user, throwable) -> {
            if (throwable != null) {
                Log.e(TAG, "onFailure: ", throwable);
                return;
            }
            currentUser.postValue(user);
        }, Dispatchers.getIO()));
    }
}
