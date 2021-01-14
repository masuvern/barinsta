package awais.instagrabber.viewmodels;

import android.app.Application;
import android.content.ContentResolver;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.collect.Iterables;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.UploadVideoOptions;
import awais.instagrabber.repositories.requests.UploadFinishOptions;
import awais.instagrabber.repositories.requests.directmessages.BroadcastOptions.ThreadIdOrUserIds;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReactions;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponseMessageMetadata;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponsePayload;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadFeedResponse;
import awais.instagrabber.utils.BitmapUtils;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DirectItemFactory;
import awais.instagrabber.utils.DirectoryUtils;
import awais.instagrabber.utils.MediaController;
import awais.instagrabber.utils.MediaUploadHelper;
import awais.instagrabber.utils.MediaUploader;
import awais.instagrabber.utils.MediaUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.VoiceRecorder;
import awais.instagrabber.webservices.DirectMessagesService;
import awais.instagrabber.webservices.MediaService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DirectThreadViewModel extends AndroidViewModel {
    private static final String TAG = DirectThreadViewModel.class.getSimpleName();
    private static final String ERROR_INVALID_USER = "Invalid user";
    private static final String ERROR_INVALID_THREAD = "Invalid thread";
    private static final String ERROR_RESPONSE_NOT_OK = "Response status from server was not ok";
    private static final String ERROR_VIDEO_TOO_LONG = "Instagram does not allow uploading videos longer than 60 secs for Direct messages";
    private static final String ERROR_AUDIO_TOO_LONG = "Instagram does not allow uploading audio longer than 60 secs";

    private final MutableLiveData<DirectThread> thread = new MutableLiveData<>();
    private final MutableLiveData<List<DirectItem>> items = new MutableLiveData<>(new LinkedList<>());
    private final MutableLiveData<String> threadTitle = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> fetching = new MutableLiveData<>(false);
    private final MutableLiveData<List<User>> users = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<User>> leftUsers = new MutableLiveData<>(new ArrayList<>());

    private final DirectMessagesService service;
    private final ContentResolver contentResolver;
    private final MediaService mediaService;
    private final String csrfToken;
    private final File recordingsDir;
    private final Application application;

    private String cursor;
    private String threadId;
    private boolean hasOlder = true;
    private ThreadIdOrUserIds threadIdOrUserIds;
    private User currentUser;
    private Call<DirectThreadFeedResponse> chatsRequest;
    private VoiceRecorder voiceRecorder;
    private final long viewerId;

    public DirectThreadViewModel(@NonNull final Application application) {
        super(application);
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        viewerId = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (TextUtils.isEmpty(csrfToken) || viewerId <= 0 || TextUtils.isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("User is not logged in!");
        }
        service = DirectMessagesService.getInstance(csrfToken, viewerId, deviceUuid);
        mediaService = MediaService.getInstance(deviceUuid, csrfToken, viewerId);
        contentResolver = application.getContentResolver();
        recordingsDir = DirectoryUtils.getOutputMediaDirectory(application, "Recordings");
        this.application = application;
    }

    public MutableLiveData<String> getThreadTitle() {
        return threadTitle;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(final String threadId) {
        this.threadId = threadId;
        this.threadIdOrUserIds = ThreadIdOrUserIds.of(threadId);
    }

    public LiveData<DirectThread> getThread() {
        return thread;
    }

    public void setThread(final DirectThread thread) {
        if (thread == null) return;
        this.thread.postValue(thread);
        setThreadId(thread.getThreadId());
        fetching.postValue(true);
        setupThreadInfo(thread);
    }

    public LiveData<List<DirectItem>> getItems() {
        return items;
    }

    public long getViewerId() {
        return viewerId;
    }

    public void setItems(final List<DirectItem> items) {
        this.items.postValue(items);
    }

    public void addItems(final Collection<DirectItem> items) {
        addItems(-1, items);
    }

    public void addItems(final int index, final Collection<DirectItem> items) {
        if (items == null) return;
        List<DirectItem> list = this.items.getValue();
        list = list == null ? new LinkedList<>() : new LinkedList<>(list);
        if (index >= 0) {
            list.addAll(index, items);
        } else {
            list.addAll(items);
        }
        this.items.postValue(list);
    }

    private void addReaction(final DirectItem item, final Emoji emoji) {
        if (item == null || emoji == null || currentUser == null) return;
        final boolean isLike = emoji.getUnicode().equals("❤️");
        DirectItemReactions reactions = item.getReactions();
        if (reactions == null) {
            reactions = new DirectItemReactions(null, null);
        } else {
            try {
                reactions = (DirectItemReactions) reactions.clone();
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "addReaction: ", e);
                return;
            }
        }
        if (isLike) {
            final List<DirectItemEmojiReaction> likes = addEmoji(reactions.getLikes(), null, false);
            reactions.setLikes(likes);
        }
        final List<DirectItemEmojiReaction> emojis = addEmoji(reactions.getEmojis(), emoji.getUnicode(), true);
        reactions.setEmojis(emojis);
        List<DirectItem> list = this.items.getValue();
        list = list == null ? new LinkedList<>() : new LinkedList<>(list);
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            final DirectItem directItem = list.get(i);
            if (directItem.getItemId().equals(item.getItemId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            try {
                final DirectItem clone = (DirectItem) list.get(index).clone();
                clone.setReactions(reactions);
                list.set(index, clone);
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "addReaction: error cloning", e);
            }
        }
        this.items.postValue(list);
    }

    private List<DirectItemEmojiReaction> addEmoji(final List<DirectItemEmojiReaction> reactionList,
                                                   final String emoji,
                                                   final boolean shouldReplaceIfAlreadyReacted) {
        final List<DirectItemEmojiReaction> temp = reactionList == null ? new ArrayList<>() : new ArrayList<>(reactionList);
        int index = -1;
        for (int i = 0; i < temp.size(); i++) {
            final DirectItemEmojiReaction directItemEmojiReaction = temp.get(i);
            if (directItemEmojiReaction.getSenderId() == currentUser.getPk()) {
                index = i;
                break;
            }
        }
        final DirectItemEmojiReaction reaction = new DirectItemEmojiReaction(
                currentUser.getPk(),
                System.currentTimeMillis() * 1000,
                emoji,
                "none"
        );
        if (index < 0) {
            temp.add(0, reaction);
        } else if (shouldReplaceIfAlreadyReacted) {
            temp.add(0, reaction);
            temp.remove(index);
        }
        return temp;
    }

    private void removeReaction(final DirectItem item) {
        try {
            final DirectItem itemClone = (DirectItem) item.clone();
            final DirectItemReactions reactions = itemClone.getReactions();
            final DirectItemReactions reactionsClone = (DirectItemReactions) reactions.clone();
            final List<DirectItemEmojiReaction> likes = reactionsClone.getLikes();
            if (likes != null) {
                final List<DirectItemEmojiReaction> updatedLikes = likes.stream()
                                                                        .filter(like -> like.getSenderId() != viewerId)
                                                                        .collect(Collectors.toList());
                reactionsClone.setLikes(updatedLikes);
            }
            final List<DirectItemEmojiReaction> emojis = reactionsClone.getEmojis();
            if (emojis != null) {
                final List<DirectItemEmojiReaction> updatedEmojis = emojis.stream()
                                                                          .filter(emoji -> emoji.getSenderId() != viewerId)
                                                                          .collect(Collectors.toList());
                reactionsClone.setEmojis(updatedEmojis);
            }
            itemClone.setReactions(reactionsClone);
            List<DirectItem> list = this.items.getValue();
            list = list == null ? new LinkedList<>() : new LinkedList<>(list);
            int index = -1;
            for (int i = 0; i < list.size(); i++) {
                final DirectItem directItem = list.get(i);
                if (directItem.getItemId().equals(item.getItemId())) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                list.set(index, itemClone);
            }
            this.items.postValue(list);
        } catch (Exception e) {
            Log.e(TAG, "removeReaction: ", e);
        }
    }

    private void updateItemSent(final String clientContext, final long timestamp) {
        if (clientContext == null) return;
        List<DirectItem> list = this.items.getValue();
        list = list == null ? new LinkedList<>() : new LinkedList<>(list);
        final int index = Iterables.indexOf(list, item -> {
            if (item == null) return false;
            return item.getClientContext().equals(clientContext);
        });
        if (index < 0) return;
        final DirectItem directItem = list.get(index);
        try {
            final DirectItem itemClone = (DirectItem) directItem.clone();
            itemClone.setPending(false);
            itemClone.setTimestamp(timestamp);
            list.set(index, itemClone);
            this.items.postValue(list);
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "updateItemSent: ", e);
        }
    }

    public void removeAllItems() {
        items.setValue(Collections.emptyList());
    }

    public LiveData<Boolean> getFetching() {
        return fetching;
    }

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public LiveData<List<User>> getLeftUsers() {
        return leftUsers;
    }

    public void fetchChats() {
        final Boolean isFetching = fetching.getValue();
        if ((isFetching != null && isFetching) || !hasOlder) return;
        fetching.postValue(true);
        chatsRequest = service.fetchThread(threadId, cursor);
        chatsRequest.enqueue(new Callback<DirectThreadFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThreadFeedResponse> call, @NonNull final Response<DirectThreadFeedResponse> response) {
                final DirectThreadFeedResponse feedResponse = response.body();
                if (feedResponse == null) {
                    Log.e(TAG, "onResponse: response was null!");
                    return;
                }
                if (!feedResponse.getStatus().equals("ok")) return;
                final DirectThread thread = feedResponse.getThread();
                setThread(thread);
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThreadFeedResponse> call, @NonNull final Throwable t) {
                Log.e(TAG, "Failed fetching dm chats", t);
                fetching.postValue(false);
                hasOlder = false;
            }
        });
    }

    public void refreshChats() {
        final Boolean isFetching = fetching.getValue();
        if (isFetching != null && isFetching) {
            stopCurrentRequest();
        }
        cursor = null;
        hasOlder = true;
        fetchChats();
    }

    private void stopCurrentRequest() {
        if (chatsRequest == null || chatsRequest.isExecuted() || chatsRequest.isCanceled()) {
            return;
        }
        chatsRequest.cancel();
        fetching.postValue(false);
    }

    private void setupThreadInfo(final DirectThread thread) {
        if (thread == null) return;
        final List<DirectItem> items = thread.getItems()
                                             .stream()
                                             .filter(directItem -> directItem.getHideInThread() == 0)
                                             .collect(Collectors.toList());
        if (!TextUtils.isEmpty(cursor)) {
            addItems(items);
        } else {
            setItems(items);
        }
        setThreadId(thread.getThreadId());
        threadTitle.postValue(thread.getThreadTitle());
        cursor = thread.getOldestCursor();
        hasOlder = thread.hasOlder();
        users.postValue(thread.getUsers());
        leftUsers.postValue(thread.getLeftUsers());
        fetching.postValue(false);
    }

    public LiveData<Resource<DirectItem>> sendText(final String text) {
        final MutableLiveData<Resource<DirectItem>> data = new MutableLiveData<>();
        final Long userId = handleCurrentUser(data);
        if (userId == null) return data;
        final String clientContext = UUID.randomUUID().toString();
        final DirectItem directItem = DirectItemFactory.createText(userId, clientContext, text);
        // Log.d(TAG, "sendText: sending: itemId: " + directItem.getItemId());
        directItem.setPending(true);
        addItems(0, Collections.singletonList(directItem));
        data.postValue(Resource.loading(directItem));
        final Call<DirectThreadBroadcastResponse> request = service.broadcastText(clientContext, threadIdOrUserIds, text);
        enqueueRequest(request, data, directItem);
        return data;
    }

    public LiveData<Resource<DirectItem>> sendUri(final MediaController.MediaEntry entry) {
        final MutableLiveData<Resource<DirectItem>> data = new MutableLiveData<>();
        if (entry == null) {
            data.postValue(Resource.error("Entry is null", null));
            return data;
        }
        final Uri uri = Uri.fromFile(new File(entry.path));
        if (!entry.isVideo) {
            sendPhoto(data, uri, entry.width, entry.height);
            return data;
        }
        sendVideo(data, uri, entry.size, entry.duration, entry.width, entry.height);
        return data;
    }

    public LiveData<Resource<DirectItem>> sendUri(final Uri uri) {
        final MutableLiveData<Resource<DirectItem>> data = new MutableLiveData<>();
        if (uri == null) {
            data.postValue(Resource.error("Uri is null", null));
            return data;
        }
        final String mimeType = Utils.getMimeType(uri, contentResolver);
        if (TextUtils.isEmpty(mimeType)) {
            data.postValue(Resource.error("Unknown MediaType", null));
            return data;
        }
        final boolean isPhoto = mimeType.startsWith("image");
        if (isPhoto) {
            sendPhoto(data, uri);
            return data;
        }
        if (mimeType.startsWith("video")) {
            sendVideo(data, uri);
        }
        return data;
    }

    private void sendPhoto(final MutableLiveData<Resource<DirectItem>> data,
                           @NonNull final Uri uri) {
        try {
            final Pair<Integer, Integer> dimensions = BitmapUtils.decodeDimensions(contentResolver, uri);
            if (dimensions == null) {
                data.postValue(Resource.error("Decoding dimensions failed", null));
                return;
            }
            sendPhoto(data, uri, dimensions.first, dimensions.second);
        } catch (FileNotFoundException e) {
            data.postValue(Resource.error(e.getMessage(), null));
            Log.e(TAG, "sendPhoto: ", e);
        }
    }

    private void sendPhoto(final MutableLiveData<Resource<DirectItem>> data,
                           @NonNull final Uri uri,
                           final int width,
                           final int height) {
        final Long userId = handleCurrentUser(data);
        if (userId == null) return;
        final String clientContext = UUID.randomUUID().toString();
        final DirectItem directItem = DirectItemFactory.createImageOrVideo(userId, clientContext, uri, width, height, false);
        directItem.setPending(true);
        addItems(0, Collections.singletonList(directItem));
        data.postValue(Resource.loading(directItem));
        MediaUploader.uploadPhoto(uri, contentResolver, new MediaUploader.OnMediaUploadCompleteListener() {
            @Override
            public void onUploadComplete(final MediaUploader.MediaUploadResponse response) {
                if (handleInvalidResponse(data, response, directItem)) return;
                final String uploadId = response.getResponse().optString("upload_id");
                final Call<DirectThreadBroadcastResponse> request = service.broadcastPhoto(clientContext, threadIdOrUserIds, uploadId);
                enqueueRequest(request, data, directItem);
            }

            @Override
            public void onFailure(final Throwable t) {
                data.postValue(Resource.error(t.getMessage(), directItem));
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    private void sendVideo(@NonNull final MutableLiveData<Resource<DirectItem>> data,
                           @NonNull final Uri uri) {
        MediaUtils.getVideoInfo(contentResolver, uri, new MediaUtils.OnInfoLoadListener<MediaUtils.VideoInfo>() {
            @Override
            public void onLoad(@Nullable final MediaUtils.VideoInfo info) {
                if (info == null) {
                    data.postValue(Resource.error("Could not get the video info", null));
                    return;
                }
                sendVideo(data, uri, info.size, info.duration, info.width, info.height);
            }

            @Override
            public void onFailure(final Throwable t) {
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
    }

    private void sendVideo(@NonNull final MutableLiveData<Resource<DirectItem>> data,
                           @NonNull final Uri uri,
                           final long byteLength,
                           final long duration,
                           final int width,
                           final int height) {
        if (duration > 60000) {
            // instagram does not allow uploading videos longer than 60 secs for Direct messages
            data.postValue(Resource.error(ERROR_VIDEO_TOO_LONG, null));
            return;
        }
        final Long userId = handleCurrentUser(data);
        if (userId == null) return;
        final String clientContext = UUID.randomUUID().toString();
        final DirectItem directItem = DirectItemFactory.createImageOrVideo(userId, clientContext, uri, width, height, true);
        directItem.setPending(true);
        addItems(0, Collections.singletonList(directItem));
        data.postValue(Resource.loading(directItem));
        final UploadVideoOptions uploadDmVideoOptions = MediaUploadHelper.createUploadDmVideoOptions(byteLength, duration, width, height);
        MediaUploader.uploadVideo(uri, contentResolver, uploadDmVideoOptions, new MediaUploader.OnMediaUploadCompleteListener() {
            @Override
            public void onUploadComplete(final MediaUploader.MediaUploadResponse response) {
                // Log.d(TAG, "onUploadComplete: " + response);
                if (handleInvalidResponse(data, response, directItem)) return;
                final UploadFinishOptions uploadFinishOptions = new UploadFinishOptions()
                        .setUploadId(uploadDmVideoOptions.getUploadId())
                        .setSourceType("2")
                        .setVideoOptions(new UploadFinishOptions.VideoOptions().setLength(duration / 1000f));
                final Call<String> uploadFinishRequest = mediaService.uploadFinish(uploadFinishOptions);
                uploadFinishRequest.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                        if (response.isSuccessful()) {
                            final Call<DirectThreadBroadcastResponse> request = service.broadcastVideo(
                                    clientContext,
                                    threadIdOrUserIds,
                                    uploadDmVideoOptions.getUploadId(),
                                    "",
                                    true
                            );
                            enqueueRequest(request, data, directItem);
                            return;
                        }
                        if (response.errorBody() != null) {
                            handleErrorBody(call, response, data, directItem);
                            return;
                        }
                        data.postValue(Resource.error("uploadFinishRequest was not successful and response error body was null", directItem));
                        Log.e(TAG, "uploadFinishRequest was not successful and response error body was null");
                    }

                    @Override
                    public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                        data.postValue(Resource.error(t.getMessage(), directItem));
                        Log.e(TAG, "onFailure: ", t);
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                data.postValue(Resource.error(t.getMessage(), directItem));
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    public LiveData<Resource<DirectItem>> startRecording() {
        final MutableLiveData<Resource<DirectItem>> data = new MutableLiveData<>();
        voiceRecorder = new VoiceRecorder(recordingsDir, new VoiceRecorder.VoiceRecorderCallback() {
            @Override
            public void onStart() {}

            @Override
            public void onComplete(final VoiceRecorder.VoiceRecordingResult result) {
                Log.d(TAG, "onComplete: recording complete. Scanning file...");
                MediaScannerConnection.scanFile(
                        application,
                        new String[]{result.getFile().getAbsolutePath()},
                        new String[]{result.getMimeType()},
                        (path, uri) -> {
                            if (uri == null) {
                                final String msg = "Scan failed!";
                                Log.e(TAG, msg);
                                data.postValue(Resource.error(msg, null));
                                return;
                            }
                            Log.d(TAG, "onComplete: scan complete");
                            MediaUtils.getVoiceInfo(contentResolver, uri, new MediaUtils.OnInfoLoadListener<MediaUtils.VideoInfo>() {
                                @Override
                                public void onLoad(@Nullable final MediaUtils.VideoInfo videoInfo) {
                                    sendVoice(data, uri, result.getWaveform(), result.getSamplingFreq(), videoInfo.duration, videoInfo.size);
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    data.postValue(Resource.error(t.getMessage(), null));
                                }
                            });
                        }
                );
            }

            @Override
            public void onCancel() {

            }
        });
        voiceRecorder.startRecording();
        return data;
    }

    public void stopRecording(final boolean delete) {
        if (voiceRecorder == null) return;
        voiceRecorder.stopRecording(delete);
        voiceRecorder = null;
    }

    private void sendVoice(@NonNull final MutableLiveData<Resource<DirectItem>> data,
                           @NonNull final Uri uri,
                           @NonNull final List<Float> waveform,
                           final int samplingFreq,
                           final long duration,
                           final long byteLength) {
        if (duration > 60000) {
            // instagram does not allow uploading audio longer than 60 secs for Direct messages
            data.postValue(Resource.error(ERROR_AUDIO_TOO_LONG, null));
            return;
        }
        final Long userId = handleCurrentUser(data);
        if (userId == null) return;
        final String clientContext = UUID.randomUUID().toString();
        final DirectItem directItem = DirectItemFactory.createVoice(userId, clientContext, uri, duration, waveform, samplingFreq);
        directItem.setPending(true);
        addItems(0, Collections.singletonList(directItem));
        data.postValue(Resource.loading(directItem));
        final UploadVideoOptions uploadDmVoiceOptions = MediaUploadHelper.createUploadDmVoiceOptions(byteLength, duration);
        MediaUploader.uploadVideo(uri, contentResolver, uploadDmVoiceOptions, new MediaUploader.OnMediaUploadCompleteListener() {
            @Override
            public void onUploadComplete(final MediaUploader.MediaUploadResponse response) {
                // Log.d(TAG, "onUploadComplete: " + response);
                if (handleInvalidResponse(data, response, directItem)) return;
                final UploadFinishOptions uploadFinishOptions = new UploadFinishOptions()
                        .setUploadId(uploadDmVoiceOptions.getUploadId())
                        .setSourceType("4");
                final Call<String> uploadFinishRequest = mediaService.uploadFinish(uploadFinishOptions);
                uploadFinishRequest.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                        if (response.isSuccessful()) {
                            final Call<DirectThreadBroadcastResponse> request = service.broadcastVoice(
                                    clientContext,
                                    threadIdOrUserIds,
                                    uploadDmVoiceOptions.getUploadId(),
                                    waveform,
                                    samplingFreq
                            );
                            enqueueRequest(request, data, directItem);
                            return;
                        }
                        if (response.errorBody() != null) {
                            handleErrorBody(call, response, data, directItem);
                            return;
                        }
                        data.postValue(Resource.error("uploadFinishRequest was not successful and response error body was null", directItem));
                        Log.e(TAG, "uploadFinishRequest was not successful and response error body was null");
                    }

                    @Override
                    public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                        data.postValue(Resource.error(t.getMessage(), directItem));
                        Log.e(TAG, "onFailure: ", t);
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                data.postValue(Resource.error(t.getMessage(), directItem));
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    public LiveData<Resource<DirectItem>> sendReaction(final DirectItem item, final Emoji emoji) {
        final MutableLiveData<Resource<DirectItem>> data = new MutableLiveData<>();
        final Long userId = handleCurrentUser(data);
        if (userId == null) return data;
        final String clientContext = UUID.randomUUID().toString();
        // Log.d(TAG, "sendText: sending: itemId: " + directItem.getItemId());
        data.postValue(Resource.loading(item));
        addReaction(item, emoji);
        String emojiUnicode = null;
        if (!emoji.getUnicode().equals("❤️")) {
            emojiUnicode = emoji.getUnicode();
        }
        final Call<DirectThreadBroadcastResponse> request = service.broadcastReaction(
                clientContext, threadIdOrUserIds, item.getItemId(), emojiUnicode, false);
        handleBroadcastReactionRequest(data, item, request);
        return data;
    }

    public LiveData<Resource<DirectItem>> sendDeleteReaction(final String itemId) {
        final MutableLiveData<Resource<DirectItem>> data = new MutableLiveData<>();
        final DirectItem item = getItem(itemId);
        if (item == null) {
            data.postValue(Resource.error("Invalid item", null));
            return data;
        }
        final DirectItemReactions reactions = item.getReactions();
        if (reactions == null) {
            // already removed?
            data.postValue(Resource.success(item));
            return data;
        }
        removeReaction(item);
        final String clientContext = UUID.randomUUID().toString();
        final Call<DirectThreadBroadcastResponse> request = service.broadcastReaction(clientContext, threadIdOrUserIds, item.getItemId(), null, true);
        handleBroadcastReactionRequest(data, item, request);
        return data;
    }

    private void handleBroadcastReactionRequest(final MutableLiveData<Resource<DirectItem>> data,
                                                final DirectItem item,
                                                @NonNull final Call<DirectThreadBroadcastResponse> request) {
        request.enqueue(new Callback<DirectThreadBroadcastResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThreadBroadcastResponse> call,
                                   @NonNull final Response<DirectThreadBroadcastResponse> response) {
                if (!response.isSuccessful()) {
                    if (response.errorBody() != null) {
                        handleErrorBody(call, response, data, item);
                        return;
                    }
                    data.postValue(Resource.error("request was not successful and response error body was null", item));
                    return;
                }
                final DirectThreadBroadcastResponse body = response.body();
                if (body == null) {
                    data.postValue(Resource.error("Response is null!", item));
                }
                // otherwise nothing to do? maybe update the timestamp in the emoji?
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThreadBroadcastResponse> call, @NonNull final Throwable t) {
                data.postValue(Resource.error(t.getMessage(), item));
                Log.e(TAG, "enqueueRequest: onFailure: ", t);
            }
        });
    }

    @Nullable
    private DirectItem getItem(final String itemId) {
        if (itemId == null) return null;
        final List<DirectItem> items = this.items.getValue();
        if (items == null) return null;
        return items.stream()
                    .filter(directItem -> directItem.getItemId().equals(itemId))
                    .findFirst()
                    .orElse(null);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(final User currentUser) {
        this.currentUser = currentUser;
    }

    @Nullable
    public User getUser(final long userId) {
        final LiveData<List<User>> users = getUsers();
        User match = null;
        if (users != null && users.getValue() != null) {
            final List<User> userList = users.getValue();
            match = userList.stream()
                            .filter(user -> user.getPk() == userId)
                            .findFirst()
                            .orElse(null);
        }
        if (match == null) {
            final LiveData<List<User>> leftUsers = getLeftUsers();
            if (leftUsers != null && leftUsers.getValue() != null) {
                final List<User> userList = leftUsers.getValue();
                match = userList.stream()
                                .filter(user -> user.getPk() == userId)
                                .findFirst()
                                .orElse(null);
            }
        }
        return match;
    }

    private void enqueueRequest(@NonNull final Call<DirectThreadBroadcastResponse> request,
                                @NonNull final MutableLiveData<Resource<DirectItem>> data,
                                @NonNull final DirectItem directItem) {
        request.enqueue(new Callback<DirectThreadBroadcastResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectThreadBroadcastResponse> call,
                                   @NonNull final Response<DirectThreadBroadcastResponse> response) {
                if (response.isSuccessful()) {
                    final DirectThreadBroadcastResponse broadcastResponse = response.body();
                    if (broadcastResponse == null) {
                        data.postValue(Resource.error("Response was null from server", directItem));
                        Log.e(TAG, "enqueueRequest: onResponse: response body is null");
                        return;
                    }
                    final String payloadClientContext;
                    final long timestamp;
                    final DirectThreadBroadcastResponsePayload payload = broadcastResponse.getPayload();
                    if (payload == null) {
                        final List<DirectThreadBroadcastResponseMessageMetadata> messageMetadata = broadcastResponse.getMessageMetadata();
                        if (messageMetadata == null || messageMetadata.isEmpty()) {
                            data.postValue(Resource.success(directItem));
                            return;
                        }
                        final DirectThreadBroadcastResponseMessageMetadata metadata = messageMetadata.get(0);
                        payloadClientContext = metadata.getClientContext();
                        timestamp = metadata.getTimestamp();
                    } else {
                        payloadClientContext = payload.getClientContext();
                        timestamp = payload.getTimestamp();
                    }
                    updateItemSent(payloadClientContext, timestamp);
                    data.postValue(Resource.success(directItem));
                    return;
                }
                if (response.errorBody() != null) {
                    handleErrorBody(call, response, data, directItem);
                }
                data.postValue(Resource.error("request was not successful and response error body was null", directItem));
            }

            @Override
            public void onFailure(@NonNull final Call<DirectThreadBroadcastResponse> call,
                                  @NonNull final Throwable t) {
                data.postValue(Resource.error(t.getMessage(), directItem));
                Log.e(TAG, "enqueueRequest: onFailure: ", t);
            }
        });
    }

    @Nullable
    private Long handleCurrentUser(final MutableLiveData<Resource<DirectItem>> data) {
        if (currentUser == null || currentUser.getPk() <= 0) {
            data.postValue(Resource.error(ERROR_INVALID_USER, null));
            return null;
        }
        final long userId = currentUser.getPk();
        if (threadIdOrUserIds == null) {
            data.postValue(Resource.error(ERROR_INVALID_THREAD, null));
            return null;
        }
        return userId;
    }

    private boolean handleInvalidResponse(final MutableLiveData<Resource<DirectItem>> data,
                                          final MediaUploader.MediaUploadResponse response,
                                          final DirectItem directItem) {
        final JSONObject responseJson = response.getResponse();
        if (responseJson == null || response.getResponseCode() != HttpURLConnection.HTTP_OK) {
            data.postValue(Resource.error(ERROR_RESPONSE_NOT_OK, directItem));
            return true;
        }
        final String status = responseJson.optString("status");
        if (TextUtils.isEmpty(status) || !status.equals("ok")) {
            data.postValue(Resource.error(ERROR_RESPONSE_NOT_OK, directItem));
            return true;
        }
        return false;
    }

    private void handleErrorBody(@NonNull final Call<?> call,
                                 @NonNull final Response<?> response,
                                 @NonNull final MutableLiveData<Resource<DirectItem>> data,
                                 @NonNull final DirectItem directItem) {
        try {
            final String string = response.errorBody().string();
            final String msg = String.format(Locale.US,
                                             "onResponse: url: %s, responseCode: %d, errorBody: %s",
                                             call.request().url().toString(),
                                             response.code(),
                                             string);
            data.postValue(Resource.error(msg, directItem));
            Log.e(TAG, msg);
        } catch (IOException e) {
            data.postValue(Resource.error(e.getMessage(), directItem));
            Log.e(TAG, "onResponse: ", e);
        }
    }
}
