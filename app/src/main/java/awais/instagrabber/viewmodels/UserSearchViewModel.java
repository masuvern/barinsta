package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.UserSearchResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectUser;
import awais.instagrabber.utils.Debouncer;
import awais.instagrabber.webservices.UserService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserSearchViewModel extends ViewModel {
    private static final String TAG = UserSearchViewModel.class.getSimpleName();
    public static final String DEBOUNCE_KEY = "search";

    private String prevQuery;
    private String currentQuery;
    private Call<UserSearchResponse> searchRequest;

    private final MutableLiveData<Resource<List<DirectUser>>> users = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showAction = new MutableLiveData<>(false);
    private final Debouncer<String> searchDebouncer;
    private final Set<DirectUser> selectedUsers = new HashSet<>();
    private final UserService userService;
    private long[] hideUserIds;

    public UserSearchViewModel() {
        userService = UserService.getInstance();
        final Debouncer.Callback<String> searchCallback = new Debouncer.Callback<String>() {
            @Override
            public void call(final String key) {
                if (userService == null || (currentQuery != null && currentQuery.equalsIgnoreCase(prevQuery))) return;
                searchRequest = userService.search(currentQuery);
                handleRequest(searchRequest);
                prevQuery = currentQuery;
            }

            @Override
            public void onError(final Throwable t) {
                Log.e(TAG, "onError: ", t);
            }
        };
        searchDebouncer = new Debouncer<>(searchCallback, 1000);
    }

    public LiveData<Resource<List<DirectUser>>> getUsers() {
        return users;
    }

    public void search(final String query) {
        currentQuery = query;
        users.postValue(Resource.loading(null));
        searchDebouncer.call(DEBOUNCE_KEY);
    }

    public void cleanup() {
        searchDebouncer.terminate();
    }

    private void handleRequest(final Call<UserSearchResponse> request) {
        request.enqueue(new Callback<UserSearchResponse>() {
            @Override
            public void onResponse(@NonNull final Call<UserSearchResponse> call, @NonNull final Response<UserSearchResponse> response) {
                if (!response.isSuccessful()) {
                    handleErrorResponse(response);
                    return;
                }
                final UserSearchResponse userSearchResponse = response.body();
                if (userSearchResponse == null) return;
                handleResponse(userSearchResponse);
            }

            @Override
            public void onFailure(@NonNull final Call<UserSearchResponse> call, @NonNull final Throwable t) {

            }
        });
    }

    private void handleResponse(final UserSearchResponse userSearchResponse) {
        users.postValue(Resource.success(userSearchResponse
                                                 .getUsers()
                                                 .stream()
                                                 .filter(directUser -> Arrays.binarySearch(hideUserIds, directUser.getPk()) < 0)
                                                 .collect(Collectors.toList())
        ));
    }

    private void handleErrorResponse(final Response<UserSearchResponse> response) {
        final ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            users.postValue(Resource.error("Request failed!", Collections.emptyList()));
            return;
        }
        String errorString;
        try {
            errorString = errorBody.string();
            Log.e(TAG, "handleErrorResponse: " + errorString);
        } catch (IOException e) {
            Log.e(TAG, "handleErrorResponse: ", e);
            errorString = e.getMessage();
        }
        users.postValue(Resource.error(errorString, Collections.emptyList()));
    }

    public void setSelectedUser(final DirectUser user, final boolean selected) {
        if (selected) {
            selectedUsers.add(user);
        } else {
            selectedUsers.remove(user);
        }
        showAction.postValue(!selectedUsers.isEmpty());
    }

    public Set<DirectUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void clearResults() {
        users.postValue(Resource.success(Collections.emptyList()));
        prevQuery = "";
    }

    public void cancelSearch() {
        searchDebouncer.cancel(DEBOUNCE_KEY);
        if (searchRequest != null) {
            searchRequest.cancel();
            searchRequest = null;
        }
    }

    public LiveData<Boolean> showAction() {
        return showAction;
    }

    public void setHideUserIds(final long[] hideUserIds) {
        this.hideUserIds = hideUserIds;
    }
}
