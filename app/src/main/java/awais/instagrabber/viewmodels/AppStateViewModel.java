package awais.instagrabber.viewmodels;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.db.datasources.AccountDataSource;
import awais.instagrabber.db.entities.Account;
import awais.instagrabber.db.repositories.AccountRepository;
import awais.instagrabber.db.repositories.RepositoryCallback;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AppStateViewModel extends AndroidViewModel {
    private static final String TAG = AppStateViewModel.class.getSimpleName();

    private final String cookie;
    private final boolean isLoggedIn;

    private User currentUser;
    private AccountRepository accountRepository;

    public AppStateViewModel(@NonNull final Application application) {
        super(application);
        // Log.d(TAG, "AppStateViewModel: constructor");
        cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (!isLoggedIn) return;
        accountRepository = AccountRepository.getInstance(AccountDataSource.getInstance(application));
        fetchProfileDetails();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void fetchProfileDetails() {
        final long uid = CookieUtils.getUserIdFromCookie(cookie);
        new ProfileFetcher(null, uid, true, user -> this.currentUser = user).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
