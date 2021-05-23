package awais.instagrabber.managers;

import android.content.ContentResolver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.requests.directmessages.BroadcastOptions;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponse;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.webservices.DirectMessagesService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class DirectMessagesManager {
    private static final String TAG = DirectMessagesManager.class.getSimpleName();
    private static final Object LOCK = new Object();

    private static DirectMessagesManager instance;

    private final InboxManager inboxManager;
    private final InboxManager pendingInboxManager;

    private DirectMessagesService service;

    public static DirectMessagesManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DirectMessagesManager();
                }
            }
        }
        return instance;
    }

    private DirectMessagesManager() {
        inboxManager = InboxManager.getInstance(false);
        pendingInboxManager = InboxManager.getInstance(true);
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final long viewerId = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (csrfToken == null) return;
        service = DirectMessagesService.getInstance(csrfToken, viewerId, deviceUuid);
    }

    public void moveThreadFromPending(@NonNull final String threadId) {
        final List<DirectThread> pendingThreads = pendingInboxManager.getThreads().getValue();
        if (pendingThreads == null) return;
        final int index = Iterables.indexOf(pendingThreads, t -> t != null && t.getThreadId().equals(threadId));
        if (index < 0) return;
        final DirectThread thread = pendingThreads.get(index);
        final DirectItem threadFirstDirectItem = thread.getFirstDirectItem();
        if (threadFirstDirectItem == null) return;
        final List<DirectThread> threads = inboxManager.getThreads().getValue();
        int insertIndex = 0;
        if (threads != null) {
            for (final DirectThread tempThread : threads) {
                final DirectItem firstDirectItem = tempThread.getFirstDirectItem();
                if (firstDirectItem == null) continue;
                final long timestamp = firstDirectItem.getTimestamp();
                if (timestamp < threadFirstDirectItem.getTimestamp()) {
                    break;
                }
                insertIndex++;
            }
        }
        thread.setPending(false);
        inboxManager.addThread(thread, insertIndex);
        pendingInboxManager.removeThread(threadId);
        final Integer currentTotal = inboxManager.getPendingRequestsTotal().getValue();
        if (currentTotal == null) return;
        inboxManager.setPendingRequestsTotal(currentTotal - 1);
    }

    public InboxManager getInboxManager() {
        return inboxManager;
    }

    public InboxManager getPendingInboxManager() {
        return pendingInboxManager;
    }

    public ThreadManager getThreadManager(@NonNull final String threadId,
                                          final boolean pending,
                                          @NonNull final User currentUser,
                                          @NonNull final ContentResolver contentResolver) {
        return ThreadManager.getInstance(threadId, pending, currentUser, contentResolver);
    }

    public void createThread(final long userPk,
                             @Nullable final Function<DirectThread, Void> callback) {
        if (service == null) return;
        final Call<DirectThread> createThreadRequest = service.createThread(Collections.singletonList(userPk), null);
        createThreadRequest.enqueue(new Callback<DirectThread>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThread> call, @NonNull final Response<DirectThread> response) {
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
                final DirectThread thread = response.body();
                if (thread == null) {
                    Log.e(TAG, "onResponse: thread is null");
                    return;
                }
                if (callback != null) {
                    callback.apply(thread);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThread> call, @NonNull final Throwable t) {

            }
        });
    }

    public void sendMedia(@NonNull final Set<RankedRecipient> recipients, final String mediaId) {
        final int[] resultsCount = {0};
        final Function<Void, Void> callback = unused -> {
            resultsCount[0]++;
            if (resultsCount[0] == recipients.size()) {
                inboxManager.refresh();
            }
            return null;
        };
        for (final RankedRecipient recipient : recipients) {
            if (recipient == null) continue;
            sendMedia(recipient, mediaId, false, callback);
        }
    }

    public void sendMedia(@NonNull final RankedRecipient recipient, final String mediaId) {
        sendMedia(recipient, mediaId, true, null);
    }

    private void sendMedia(@NonNull final RankedRecipient recipient,
                           @NonNull final String mediaId,
                           final boolean refreshInbox,
                           @Nullable final Function<Void, Void> callback) {
        if (recipient.getThread() == null && recipient.getUser() != null) {
            // create thread and forward
            createThread(recipient.getUser().getPk(), directThread -> {
                sendMedia(directThread, mediaId, unused -> {
                    if (refreshInbox) {
                        inboxManager.refresh();
                    }
                    if (callback != null) {
                        callback.apply(null);
                    }
                    return null;
                });
                return null;
            });
        }
        if (recipient.getThread() == null) return;
        // just forward
        final DirectThread thread = recipient.getThread();
        sendMedia(thread, mediaId, unused -> {
            if (refreshInbox) {
                inboxManager.refresh();
            }
            if (callback != null) {
                callback.apply(null);
            }
            return null;
        });
    }

    @NonNull
    public LiveData<Resource<Object>> sendMedia(@NonNull final DirectThread thread,
                                                @NonNull final String mediaId,
                                                @Nullable final Function<Void, Void> callback) {
        return sendMedia(thread.getThreadId(), mediaId, callback);
    }

    @NonNull
    public LiveData<Resource<Object>> sendMedia(@NonNull final String threadId,
                                                @NonNull final String mediaId,
                                                @Nullable final Function<Void, Void> callback) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>();
        data.postValue(Resource.loading(null));
        final Call<DirectThreadBroadcastResponse> request = service.broadcastMediaShare(
                UUID.randomUUID().toString(),
                BroadcastOptions.ThreadIdOrUserIds.of(threadId),
                mediaId
        );
        request.enqueue(new Callback<DirectThreadBroadcastResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThreadBroadcastResponse> call,
                                   @NonNull final Response<DirectThreadBroadcastResponse> response) {
                if (response.isSuccessful()) {
                    data.postValue(Resource.success(new Object()));
                    if (callback != null) {
                        callback.apply(null);
                    }
                    return;
                }
                if (response.errorBody() != null) {
                    try {
                        final String string = response.errorBody().string();
                        final String msg = String.format(Locale.US,
                                                         "onResponse: url: %s, responseCode: %d, errorBody: %s",
                                                         call.request().url().toString(),
                                                         response.code(),
                                                         string);
                        Log.e(TAG, msg);
                        data.postValue(Resource.error(msg, null));
                    } catch (IOException e) {
                        Log.e(TAG, "onResponse: ", e);
                        data.postValue(Resource.error(e.getMessage(), null));
                    }
                    if (callback != null) {
                        callback.apply(null);
                    }
                    return;
                }
                final String msg = "onResponse: request was not successful and response error body was null";
                Log.e(TAG, msg);
                data.postValue(Resource.error(msg, null));
                if (callback != null) {
                    callback.apply(null);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThreadBroadcastResponse> call, @NonNull final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                data.postValue(Resource.error(t.getMessage(), null));
                if (callback != null) {
                    callback.apply(null);
                }
            }
        });
        return data;
    }
}
