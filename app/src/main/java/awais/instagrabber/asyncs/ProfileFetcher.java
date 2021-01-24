package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.FriendshipStatus;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.webservices.GraphQLService;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.UserService;

public final class ProfileFetcher extends AsyncTask<Void, Void, String> {
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
    protected String doInBackground(final Void... voids) {
        if (isLoggedIn) {
            userService.getUsernameInfo(userName, new ServiceCallback<User>() {
                @Override
                public void onSuccess(final User user) {
                    Log.d("austin_debug", user.getUsername() + " " + userName);
                    userService.getUserFriendship(user.getPk(), new ServiceCallback<FriendshipStatus>() {
                        @Override
                        public void onSuccess(final FriendshipStatus status) {
                            user.setFriendshipStatus(status);
                            fetchListener.onResult(user);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error", t);
                        }
                    });
                }

                @Override
                public void onFailure(final Throwable t) {
                    Log.e(TAG, "Error", t);
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
                }
            });
        }
        return "yeah";
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }
}
