package awais.instagrabber.viewmodels;

import android.app.Application;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.collect.ImmutableList;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.MultiOptionDialogFragment.Option;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.FriendshipChangeResponse;
import awais.instagrabber.repositories.responses.FriendshipRestrictResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadDetailsChangeResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadParticipantRequestsResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.DirectMessagesService;
import awais.instagrabber.webservices.FriendshipService;
import awais.instagrabber.webservices.ServiceCallback;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DirectSettingsViewModel extends AndroidViewModel {
    private static final String TAG = DirectSettingsViewModel.class.getSimpleName();
    private static final String ACTION_KICK = "kick";
    private static final String ACTION_MAKE_ADMIN = "make_admin";
    private static final String ACTION_REMOVE_ADMIN = "remove_admin";
    private static final String ACTION_BLOCK = "block";
    private static final String ACTION_UNBLOCK = "unblock";
    // private static final String ACTION_REPORT = "report";
    private static final String ACTION_RESTRICT = "restrict";
    private static final String ACTION_UNRESTRICT = "unrestrict";

    private final MutableLiveData<Pair<List<User>, List<User>>> users = new MutableLiveData<>(
            new Pair<>(Collections.emptyList(), Collections.emptyList()));
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<List<Long>> adminUserIds = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> muted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> mentionsMuted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> approvalRequiredToJoin = new MutableLiveData<>(false);
    private final MutableLiveData<DirectThreadParticipantRequestsResponse> pendingRequests = new MutableLiveData<>(null);
    private final DirectMessagesService directMessagesService;
    private final long userId;
    private final Resources resources;
    private final FriendshipService friendshipService;
    private final String csrfToken;

    private DirectThread thread;
    private boolean viewerIsAdmin;
    private User viewer;

    public DirectSettingsViewModel(final Application application) {
        super(application);
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        userId = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (TextUtils.isEmpty(csrfToken) || userId <= 0 || TextUtils.isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("User is not logged in!");
        }
        directMessagesService = DirectMessagesService.getInstance(csrfToken, userId, deviceUuid);
        friendshipService = FriendshipService.getInstance(deviceUuid, csrfToken, userId);
        resources = getApplication().getResources();
    }

    @NonNull
    public DirectThread getThread() {
        return thread;
    }

    public void setThread(@NonNull final DirectThread thread) {
        this.thread = thread;
        List<User> users = thread.getUsers();
        if (viewer != null) {
            final ImmutableList.Builder<User> builder = ImmutableList.<User>builder().add(viewer);
            if (users != null) {
                builder.addAll(users);
            }
            users = builder.build();
        }
        this.users.postValue(new Pair<>(users, thread.getLeftUsers()));
        setTitle(thread.getThreadTitle());
        final List<Long> adminUserIds = thread.getAdminUserIds();
        this.adminUserIds.postValue(adminUserIds);
        viewerIsAdmin = adminUserIds.contains(userId);
        muted.postValue(thread.isMuted());
        mentionsMuted.postValue(thread.isMentionsMuted());
        approvalRequiredToJoin.postValue(thread.isApprovalRequiredForNewMembers());
        if (thread.isGroup() && viewerIsAdmin) {
            fetchPendingRequests();
        }
    }

    public boolean isGroup() {
        if (thread != null) {
            return thread.isGroup();
        }
        return false;
    }

    public LiveData<Pair<List<User>, List<User>>> getUsers() {
        return users;
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        if (title == null) {
            this.title.postValue("");
            return;
        }
        this.title.postValue(title.trim());
    }

    public LiveData<List<Long>> getAdminUserIds() {
        return adminUserIds;
    }

    public LiveData<Boolean> getMuted() {
        return muted;
    }

    public LiveData<Boolean> getApprovalRequiredToJoin() {
        return approvalRequiredToJoin;
    }

    public LiveData<DirectThreadParticipantRequestsResponse> getPendingRequests() {
        return pendingRequests;
    }

    public boolean isViewerAdmin() {
        return viewerIsAdmin;
    }

    public LiveData<Resource<Object>> updateTitle(final String newTitle) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        final Call<DirectThreadDetailsChangeResponse> addUsersRequest = directMessagesService
                .updateTitle(thread.getThreadId(), newTitle.trim());
        handleDetailsChangeRequest(data, addUsersRequest);
        return data;
    }

    public LiveData<Resource<Object>> addMembers(final Set<User> users) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        final Call<DirectThreadDetailsChangeResponse> addUsersRequest = directMessagesService
                .addUsers(thread.getThreadId(), users.stream().map(User::getPk).collect(Collectors.toList()));
        handleDetailsChangeRequest(data, addUsersRequest);
        return data;
    }

    public LiveData<Resource<Object>> removeMember(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        final Call<String> request = directMessagesService
                .removeUsers(thread.getThreadId(), Collections.singleton(user.getPk()));
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                Pair<List<User>, List<User>> usersValue = users.getValue();
                if (usersValue == null) {
                    usersValue = new Pair<>(Collections.emptyList(), Collections.emptyList());
                }
                List<User> activeUsers = usersValue.first;
                if (activeUsers == null) {
                    activeUsers = Collections.emptyList();
                }
                final List<User> updatedActiveUsers = activeUsers.stream()
                                                                 .filter(user1 -> user1.getPk() != user.getPk())
                                                                 .collect(Collectors.toList());
                List<User> leftUsers = usersValue.second;
                if (leftUsers == null) {
                    leftUsers = Collections.emptyList();
                }
                final ImmutableList<User> updateLeftUsers = ImmutableList.<User>builder()
                        .addAll(leftUsers)
                        .add(user)
                        .build();
                users.postValue(new Pair<>(updatedActiveUsers, updateLeftUsers));
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private LiveData<Resource<Object>> makeAdmin(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        if (isAdmin(user)) return data;
        final Call<String> request = directMessagesService.addAdmins(thread.getThreadId(), Collections.singleton(user.getPk()));
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                final List<Long> currentAdmins = adminUserIds.getValue();
                adminUserIds.postValue(ImmutableList.<Long>builder()
                                               .addAll(currentAdmins != null ? currentAdmins : Collections.emptyList())
                                               .add(user.getPk())
                                               .build());
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private LiveData<Resource<Object>> removeAdmin(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        if (!isAdmin(user)) return data;
        final Call<String> request = directMessagesService.removeAdmins(thread.getThreadId(), Collections.singleton(user.getPk()));
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                final List<Long> currentAdmins = adminUserIds.getValue();
                if (currentAdmins == null) return;
                adminUserIds.postValue(currentAdmins.stream()
                                                    .filter(userId1 -> userId1 != user.getPk())
                                                    .collect(Collectors.toList()));
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<Object>> mute() {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        if (thread.isMuted()) {
            data.postValue(Resource.success(new Object()));
            return data;
        }
        final Call<String> request = directMessagesService.mute(thread.getThreadId());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                thread.setMuted(true);
                muted.postValue(true);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<Object>> unmute() {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        if (!thread.isMuted()) {
            data.postValue(Resource.success(new Object()));
            return data;
        }
        final Call<String> request = directMessagesService.unmute(thread.getThreadId());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                thread.setMuted(false);
                muted.postValue(false);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<Object>> muteMentions() {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        if (thread.isMentionsMuted()) {
            data.postValue(Resource.success(new Object()));
            return data;
        }
        final Call<String> request = directMessagesService.muteMentions(thread.getThreadId());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                thread.setMentionsMuted(true);
                mentionsMuted.postValue(true);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<Object>> unmuteMentions() {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        if (!thread.isMentionsMuted()) {
            data.postValue(Resource.success(new Object()));
            return data;
        }
        final Call<String> request = directMessagesService.unmuteMentions(thread.getThreadId());
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                if (!response.isSuccessful()) {
                    handleSettingChangeResponseError(response, data);
                    return;
                }
                thread.setMentionsMuted(false);
                mentionsMuted.postValue(false);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private void handleSettingChangeResponseError(@NonNull final Response<String> response,
                                                  final MutableLiveData<Resource<Object>> data) {
        final ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            handleErrorResponse(response, data);
            return;
        }
        try {
            final JSONObject json = new JSONObject(errorBody.string());
            if (json.has("message")) {
                data.postValue(Resource.error(json.getString("message"), null));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "onResponse: ", e);
            data.postValue(Resource.error(e.getMessage(), null));
        }
    }

    private LiveData<Resource<Object>> blockUser(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        friendshipService.block(user.getPk(), new ServiceCallback<FriendshipChangeResponse>() {
            @Override
            public void onSuccess(final FriendshipChangeResponse result) {
                // refresh thread
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private LiveData<Resource<Object>> unblockUser(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        friendshipService.unblock(user.getPk(), new ServiceCallback<FriendshipChangeResponse>() {
            @Override
            public void onSuccess(final FriendshipChangeResponse result) {
                // refresh thread
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private LiveData<Resource<Object>> restrictUser(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        friendshipService.toggleRestrict(user.getPk(), true, new ServiceCallback<FriendshipRestrictResponse>() {
            @Override
            public void onSuccess(final FriendshipRestrictResponse result) {
                // refresh thread
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private LiveData<Resource<Object>> unRestrictUser(final User user) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        friendshipService.toggleRestrict(user.getPk(), false, new ServiceCallback<FriendshipRestrictResponse>() {
            @Override
            public void onSuccess(final FriendshipRestrictResponse result) {
                // refresh thread
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<Object>> approveUsers(final List<User> users) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        final Call<DirectThreadDetailsChangeResponse> approveUsersRequest = directMessagesService
                .approveParticipantRequests(thread.getThreadId(), users.stream().map(User::getPk).collect(Collectors.toList()));
        handleDetailsChangeRequest(data, approveUsersRequest);
        return data;
    }

    public LiveData<Resource<Object>> denyUsers(final List<User> users) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        final Call<DirectThreadDetailsChangeResponse> approveUsersRequest = directMessagesService
                .declineParticipantRequests(thread.getThreadId(), users.stream().map(User::getPk).collect(Collectors.toList()));
        handleDetailsChangeRequest(data, approveUsersRequest, () -> {
            final DirectThreadParticipantRequestsResponse pendingRequestsValue = pendingRequests.getValue();
            if (pendingRequestsValue == null) return;
            final List<User> pendingUsers = pendingRequestsValue.getUsers();
            if (pendingUsers == null || pendingUsers.isEmpty()) return;
            final List<User> filtered = pendingUsers.stream()
                                                    .filter(o -> !users.contains(o))
                                                    .collect(Collectors.toList());
            try {
                final DirectThreadParticipantRequestsResponse clone = (DirectThreadParticipantRequestsResponse) pendingRequestsValue.clone();
                clone.setUsers(filtered);
                pendingRequests.postValue(clone);
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "denyUsers: ", e);
            }
        });
        return data;
    }

    private interface OnSuccessAction {
        void onSuccess();
    }

    private void handleDetailsChangeRequest(final MutableLiveData<Resource<Object>> data,
                                            final Call<DirectThreadDetailsChangeResponse> request) {
        handleDetailsChangeRequest(data, request, null);
    }

    private void handleDetailsChangeRequest(final MutableLiveData<Resource<Object>> data,
                                            final Call<DirectThreadDetailsChangeResponse> request,
                                            @Nullable final OnSuccessAction action) {
        request.enqueue(new Callback<DirectThreadDetailsChangeResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThreadDetailsChangeResponse> call,
                                   @NonNull final Response<DirectThreadDetailsChangeResponse> response) {
                if (!response.isSuccessful()) {
                    handleErrorResponse(response, data);
                    return;
                }
                final DirectThreadDetailsChangeResponse changeResponse = response.body();
                if (changeResponse == null) {
                    data.postValue(Resource.error("Response is null", null));
                    return;
                }
                data.postValue(Resource.success(new Object()));
                final DirectThread thread = changeResponse.getThread();
                if (thread != null) {
                    setThread(thread);
                }
                if (action != null) {
                    action.onSuccess();
                }
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThreadDetailsChangeResponse> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
    }

    private void handleErrorResponse(@NonNull final Response<?> response,
                                     final MutableLiveData<Resource<Object>> data) {
        final ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            data.postValue(Resource.error("Request failed!", null));
            return;
        }
        try {
            data.postValue(Resource.error(errorBody.string(), null));
        } catch (IOException e) {
            Log.e(TAG, "onResponse: ", e);
            data.postValue(Resource.error(e.getMessage(), null));
        }
    }

    public ArrayList<Option<String>> createUserOptions(final User user) {
        final ArrayList<Option<String>> options = new ArrayList<>();
        if (user == null || isSelf(user) || hasLeft(user)) {
            return options;
        }
        if (viewerIsAdmin) {
            options.add(new Option<>(getString(R.string.dms_action_kick), ACTION_KICK));

            final boolean isAdmin = isAdmin(user);
            options.add(new Option<>(
                    isAdmin ? getString(R.string.dms_action_remove_admin) : getString(R.string.dms_action_make_admin),
                    isAdmin ? ACTION_REMOVE_ADMIN : ACTION_MAKE_ADMIN
            ));
        }

        final boolean blocking = user.getFriendshipStatus().isBlocking();
        options.add(new Option<>(
                blocking ? getString(R.string.unblock) : getString(R.string.block),
                blocking ? ACTION_UNBLOCK : ACTION_BLOCK
        ));

        // options.add(new Option<>(getString(R.string.report), ACTION_REPORT));

        if (!isGroup()) {
            final boolean restricted = user.getFriendshipStatus().isRestricted();
            options.add(new Option<>(
                    restricted ? getString(R.string.unrestrict) : getString(R.string.restrict),
                    restricted ? ACTION_UNRESTRICT : ACTION_RESTRICT
            ));
        }
        return options;
    }

    private boolean hasLeft(final User user) {
        final Pair<List<User>, List<User>> users = this.users.getValue();
        if (users == null || users.second == null) return false;
        return users.second.contains(user);
    }

    private boolean isAdmin(final User user) {
        final List<Long> adminUserIdsValue = adminUserIds.getValue();
        return adminUserIdsValue != null && adminUserIdsValue.contains(user.getPk());
    }

    private boolean isSelf(final User user) {
        return user.getPk() == userId;
    }

    private String getString(@StringRes final int resId) {
        return resources.getString(resId);
    }

    public LiveData<Resource<Object>> doAction(final User user, final String action) {
        if (user == null || action == null) return null;
        switch (action) {
            case ACTION_KICK:
                return removeMember(user);
            case ACTION_MAKE_ADMIN:
                return makeAdmin(user);
            case ACTION_REMOVE_ADMIN:
                return removeAdmin(user);
            case ACTION_BLOCK:
                return blockUser(user);
            case ACTION_UNBLOCK:
                return unblockUser(user);
            // case ACTION_REPORT:
            //     break;
            case ACTION_RESTRICT:
                return restrictUser(user);
            case ACTION_UNRESTRICT:
                return unRestrictUser(user);
            default:
                return null;
        }
    }

    public void setViewer(final User viewer) {
        this.viewer = viewer;
    }

    private void fetchPendingRequests() {
        final Call<DirectThreadParticipantRequestsResponse> request = directMessagesService.participantRequests(thread.getThreadId(), 5, null);
        request.enqueue(new Callback<DirectThreadParticipantRequestsResponse>() {

            @Override
            public void onResponse(@NonNull final Call<DirectThreadParticipantRequestsResponse> call,
                                   @NonNull final Response<DirectThreadParticipantRequestsResponse> response) {
                if (!response.isSuccessful()) {
                    if (response.errorBody() != null) {
                        try {
                            final String string = response.errorBody().string();
                            final String msg = String.format(Locale.US,
                                                             "onResponse: url: %s, responseCode: %d, errorBody: %s",
                                                             call.request().url().toString(),
                                                             response.code(),
                                                             string);
                            Log.e(TAG, msg);
                        } catch (IOException e) {
                            Log.e(TAG, "onResponse: ", e);
                        }
                        return;
                    }
                    Log.e(TAG, "onResponse: request was not successful and response error body was null");
                    return;
                }
                final DirectThreadParticipantRequestsResponse body = response.body();
                if (body == null) {
                    Log.e(TAG, "onResponse: response body was null");
                    return;
                }
                pendingRequests.postValue(body);
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThreadParticipantRequestsResponse> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }
}
