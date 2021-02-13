package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectBadgeCount;
import awais.instagrabber.repositories.responses.directmessages.DirectInbox;
import awais.instagrabber.repositories.responses.directmessages.DirectInboxResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.DirectMessagesService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DirectPendingInboxViewModel extends ViewModel {
    private static final String TAG = DirectPendingInboxViewModel.class.getSimpleName();

    private final DirectMessagesService service;
    private final MutableLiveData<Boolean> fetchingInbox = new MutableLiveData<>(false);
    private final MutableLiveData<List<DirectThread>> threads = new MutableLiveData<>();

    private Call<DirectInboxResponse> inboxRequest;
    private Call<DirectBadgeCount> unseenCountRequest;
    private long seqId;
    private String cursor;
    private boolean hasOlder = true;
    private User viewer;

    public DirectPendingInboxViewModel() {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final long userId = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (TextUtils.isEmpty(csrfToken) || userId <= 0 || TextUtils.isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("User is not logged in!");
        }
        service = DirectMessagesService.getInstance(csrfToken, userId, deviceUuid);
        fetchInbox();
    }

    public LiveData<List<DirectThread>> getThreads() {
        return threads;
    }

    public void setThreads(final List<DirectThread> threads) {
        this.threads.postValue(threads);
    }

    public void addThreads(final Collection<DirectThread> threads) {
        if (threads == null) return;
        List<DirectThread> list = getThreads().getValue();
        list = list == null ? new LinkedList<>() : new LinkedList<>(list);
        list.addAll(threads);
        this.threads.postValue(list);
    }

    public LiveData<Boolean> getFetchingInbox() {
        return fetchingInbox;
    }

    public User getViewer() {
        return viewer;
    }

    public void fetchInbox() {
        if ((fetchingInbox.getValue() != null && fetchingInbox.getValue()) || !hasOlder) return;
        stopCurrentInboxRequest();
        fetchingInbox.postValue(true);
        inboxRequest = service.fetchPendingInbox(cursor, seqId);
        inboxRequest.enqueue(new Callback<DirectInboxResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectInboxResponse> call, @NonNull final Response<DirectInboxResponse> response) {
                parseInboxResponse(response.body());
                fetchingInbox.postValue(false);
            }

            @Override
            public void onFailure(@NonNull final Call<DirectInboxResponse> call, @NonNull final Throwable t) {
                Log.e(TAG, "Failed fetching pending inbox", t);
                fetchingInbox.postValue(false);
                hasOlder = false;
            }
        });
    }

    private void parseInboxResponse(final DirectInboxResponse response) {
        if (response == null) {
            hasOlder = false;
            return;
        }
        if (!response.getStatus().equals("ok")) {
            Log.e(TAG, "DM pending inbox fetch response: status not ok");
            hasOlder = false;
            return;
        }
        seqId = response.getSeqId();
        if (viewer == null) {
            viewer = response.getViewer();
        }
        final DirectInbox inbox = response.getInbox();
        final List<DirectThread> threads = inbox.getThreads();
        if (!TextUtils.isEmpty(cursor)) {
            addThreads(threads);
        } else {
            setThreads(threads);
        }
        cursor = inbox.getOldestCursor();
        hasOlder = inbox.hasOlder();
    }

    private void stopCurrentInboxRequest() {
        if (inboxRequest == null || inboxRequest.isCanceled() || inboxRequest.isExecuted()) return;
        inboxRequest.cancel();
        inboxRequest = null;
    }

    public void refresh() {
        cursor = null;
        seqId = 0;
        hasOlder = true;
        fetchInbox();
    }

    public void onDestroy() {
        stopCurrentInboxRequest();
    }
}
