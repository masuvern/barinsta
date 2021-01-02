package awais.instagrabber.repositories.requests.directmessages;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import awais.instagrabber.models.enums.BroadcastItemType;

public abstract class BroadcastOptions {

    private final String clientContext;
    private final ThreadIdOrUserIds threadIdOrUserIds;
    private final BroadcastItemType itemType;

    public BroadcastOptions(final String clientContext,
                            @NonNull final ThreadIdOrUserIds threadIdOrUserIds,
                            @NonNull final BroadcastItemType itemType) {
        this.clientContext = clientContext;
        this.threadIdOrUserIds = threadIdOrUserIds;
        this.itemType = itemType;
    }

    public String getClientContext() {
        return clientContext;
    }

    public String getThreadId() {
        return threadIdOrUserIds.getThreadId();
    }

    public List<String> getUserIds() {
        return threadIdOrUserIds.getUserIds();
    }

    public BroadcastItemType getItemType() {
        return itemType;
    }

    public abstract Map<String, String> getFormMap();

    public static final class ThreadIdOrUserIds {
        private final String threadId;
        private final List<String> userIds;

        private ThreadIdOrUserIds(final String threadId, final List<String> userIds) {
            this.threadId = threadId;
            this.userIds = userIds;
        }

        public static ThreadIdOrUserIds of(final String threadId) {
            return new ThreadIdOrUserIds(threadId, null);
        }

        public static ThreadIdOrUserIds of(final List<String> userIds) {
            return new ThreadIdOrUserIds(null, userIds);
        }

        public String getThreadId() {
            return threadId;
        }

        public List<String> getUserIds() {
            return userIds;
        }
    }
}
