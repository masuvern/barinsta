package awais.instagrabber.viewmodels;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.asyncs.UsernameFetcher;
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
    private String username;

    public AppStateViewModel(@NonNull final Application application) {
        super(application);
        // Log.d(TAG, "AppStateViewModel: constructor");
        cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (!isLoggedIn) return;
        accountRepository = AccountRepository.getInstance(AccountDataSource.getInstance(application));
        setCurrentUser();
    }

    private void setCurrentUser() {
        if (!isLoggedIn) return;
        final FetchListener<String> usernameListener = username -> {
            if (TextUtils.isEmpty(username)) return;
            this.username = username;
            fetchProfileDetails();
        };
        fetchUsername(usernameListener);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void fetchUsername(final FetchListener<String> usernameListener) {
        if (!TextUtils.isEmpty(username)) {
            usernameListener.onResult(username);
            return;
        }
        final long uid = CookieUtils.getUserIdFromCookie(cookie);
        if (uid <= 0) return;
        accountRepository.getAccount(uid, new RepositoryCallback<Account>() {
            @Override
            public void onSuccess(@NonNull final Account account) {
                final String username = account.getUsername();
                if (TextUtils.isEmpty(username)) return;
                usernameListener.onResult("@" + username);
            }

            @Override
            public void onDataNotAvailable() {
                // if not in database, fetch info
                new UsernameFetcher(uid, usernameListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }

    private void fetchProfileDetails() {
        if (TextUtils.isEmpty(username)) return;
        new ProfileFetcher(
                username.trim().substring(1),
                true,
                user -> this.currentUser = user
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
