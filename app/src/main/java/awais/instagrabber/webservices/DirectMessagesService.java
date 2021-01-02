package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import awais.instagrabber.repositories.DirectMessagesRepository;
import awais.instagrabber.repositories.requests.directmessages.BroadcastOptions;
import awais.instagrabber.repositories.requests.directmessages.BroadcastOptions.ThreadIdOrUserIds;
import awais.instagrabber.repositories.requests.directmessages.LinkBroadcastOptions;
import awais.instagrabber.repositories.requests.directmessages.PhotoBroadcastOptions;
import awais.instagrabber.repositories.requests.directmessages.TextBroadcastOptions;
import awais.instagrabber.repositories.requests.directmessages.VideoBroadcastOptions;
import awais.instagrabber.repositories.requests.directmessages.VoiceBroadcastOptions;
import awais.instagrabber.repositories.responses.directmessages.DirectBadgeCount;
import awais.instagrabber.repositories.responses.directmessages.DirectInboxResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponse;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadFeedResponse;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Retrofit;

public class DirectMessagesService extends BaseService {
    private static final String TAG = "DiscoverService";

    private static DirectMessagesService instance;

    private final DirectMessagesRepository repository;
    private final String csrfToken;
    private final String userId;
    private final String deviceUuid;

    private DirectMessagesService(@NonNull final String csrfToken,
                                  @NonNull final String userId,
                                  @NonNull final String deviceUuid) {
        this.csrfToken = csrfToken;
        this.userId = userId;
        this.deviceUuid = deviceUuid;
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(DirectMessagesRepository.class);
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public static DirectMessagesService getInstance(@NonNull final String csrfToken,
                                                    @NonNull final String userId,
                                                    @NonNull final String deviceUuid) {
        if (instance == null
                || !Objects.equals(instance.getCsrfToken(), csrfToken)
                || !Objects.equals(instance.getUserId(), userId)
                || !Objects.equals(instance.getDeviceUuid(), deviceUuid)) {
            instance = new DirectMessagesService(csrfToken, userId, deviceUuid);
        }
        return instance;
    }

    public Call<DirectInboxResponse> fetchInbox(final String cursor,
                                                final long seqId) {
        final ImmutableMap.Builder<String, Object> queryMapBuilder = ImmutableMap.<String, Object>builder()
                .put("visual_message_return_type", "unseen")
                .put("thread_message_limit", 10)
                .put("persistentBadging", true)
                .put("limit", 10);
        if (!TextUtils.isEmpty(cursor)) {
            queryMapBuilder.put("cursor", cursor);
            queryMapBuilder.put("direction", "older");
        }
        if (seqId != 0) {
            queryMapBuilder.put("seq_id", seqId);
        }
        return repository.fetchInbox(queryMapBuilder.build());
    }

    public Call<DirectThreadFeedResponse> fetchThread(final String threadId,
                                                      final String cursor) {
        final ImmutableMap.Builder<String, Object> queryMapBuilder = ImmutableMap.<String, Object>builder()
                .put("visual_message_return_type", "unseen")
                .put("limit", 10)
                .put("direction", "older");
        if (!TextUtils.isEmpty(cursor)) {
            queryMapBuilder.put("cursor", cursor);
        }
        return repository.fetchThread(threadId, queryMapBuilder.build());
    }

    public Call<DirectBadgeCount> fetchUnseenCount() {
        return repository.fetchUnseenCount();
    }

    public Call<DirectThreadBroadcastResponse> broadcastText(final String clientContext,
                                                             final ThreadIdOrUserIds threadIdOrUserIds,
                                                             final String text) {
        final List<String> urls = TextUtils.extractUrls(text);
        if (!urls.isEmpty()) {
            return broadcastLink(clientContext, threadIdOrUserIds, text, urls);
        }
        return broadcast(new TextBroadcastOptions(clientContext, threadIdOrUserIds, text));
    }

    public Call<DirectThreadBroadcastResponse> broadcastLink(final String clientContext,
                                                             final ThreadIdOrUserIds threadIdOrUserIds,
                                                             final String linkText,
                                                             final List<String> urls) {
        return broadcast(new LinkBroadcastOptions(clientContext, threadIdOrUserIds, linkText, urls));
    }

    public Call<DirectThreadBroadcastResponse> broadcastPhoto(final String clientContext,
                                                              final ThreadIdOrUserIds threadIdOrUserIds,
                                                              final String uploadId) {
        return broadcast(new PhotoBroadcastOptions(clientContext, threadIdOrUserIds, true, uploadId));
    }

    public Call<DirectThreadBroadcastResponse> broadcastVideo(final String clientContext,
                                                              final ThreadIdOrUserIds threadIdOrUserIds,
                                                              final String uploadId,
                                                              final String videoResult,
                                                              final boolean sampled) {
        return broadcast(new VideoBroadcastOptions(clientContext, threadIdOrUserIds, videoResult, uploadId, sampled));
    }

    public Call<DirectThreadBroadcastResponse> broadcastVoice(final String clientContext,
                                                              final ThreadIdOrUserIds threadIdOrUserIds,
                                                              final String uploadId,
                                                              final List<Float> waveform,
                                                              final int samplingFreq) {
        return broadcast(new VoiceBroadcastOptions(clientContext, threadIdOrUserIds, uploadId, waveform, samplingFreq));
    }

    private Call<DirectThreadBroadcastResponse> broadcast(@NonNull final BroadcastOptions broadcastOptions) {
        if (TextUtils.isEmpty(broadcastOptions.getClientContext())) {
            throw new IllegalArgumentException("Broadcast requires a valid client context value");
        }
        final Map<String, Object> form = new HashMap<>();
        if (!TextUtils.isEmpty(broadcastOptions.getThreadId())) {
            form.put("thread_id", broadcastOptions.getThreadId());
        } else {
            form.put("recipient_users", new JSONArray(broadcastOptions.getUserIds()).toString());
        }
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("__uuid", deviceUuid);
        form.put("client_context", broadcastOptions.getClientContext());
        form.put("mutation_token", broadcastOptions.getClientContext());
        form.putAll(broadcastOptions.getFormMap());
        form.put("action", "send_item");
        final Map<String, String> signedForm = Utils.sign(form);
        return repository.broadcast(broadcastOptions.getItemType().getValue(), signedForm);
    }
}
