package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.FriendshipStatus;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.webservices.GraphQLService;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.UserService;

public final class ProfileFetcher extends AsyncTask<Void, Void, Void> {
    private static final String TAG = ProfileFetcher.class.getSimpleName();
    private final UserService userService;
    private final GraphQLService graphQLService;

    private final FetchListener<User> fetchListener;
    private final boolean isLoggedIn;
    private final String userName;

    public ProfileFetcher(final String userName,
                          final boolean isLoggedIn,
                          final FetchListener<User> fetchListener) {
        this.userName = userName;
        this.isLoggedIn = isLoggedIn;
        this.fetchListener = fetchListener;
        userService = isLoggedIn ? UserService.getInstance() : null;
        graphQLService = isLoggedIn ? null : GraphQLService.getInstance();
    }

    @Nullable
    @Override
    protected Void doInBackground(final Void... voids) {
        if (isLoggedIn) {
            userService.getUsernameInfo(userName, new ServiceCallback<User>() {
                @Override
                public void onSuccess(final User user) {
                    userService.getUserFriendship(user.getPk(), new ServiceCallback<FriendshipStatus>() {
                        @Override
                        public void onSuccess(final FriendshipStatus status) {
                            user.setFriendshipStatus(status);
                            fetchListener.onResult(user);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error", t);
                            fetchListener.onFailure(t);
                        }
                    });
                }

                @Override
                public void onFailure(final Throwable t) {
                    Log.e(TAG, "Error", t);
                    fetchListener.onFailure(t);
                }
            });
        }
        else {
            graphQLService.fetchUser(userName, new ServiceCallback<User>() {
                @Override
                public void onSuccess(final User user) {
                    fetchListener.onResult(user);
                }

                @Override
                public void onFailure(final Throwable t) {
                    Log.e(TAG, "Error", t);
                    fetchListener.onFailure(t);
                }
            });
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }
}
