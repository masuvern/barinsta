package awais.instagrabber.models.direct_messages;

public final class InboxModel {
    private final boolean hasOlder, hasPendingTopRequests, blendedInboxEnabled;
    private final int unseenCount, pendingRequestsCount;
    private final long seqId, unseenCountTimestamp;
    private final InboxThreadModel[] threads;
    private String oldestCursor;

    public InboxModel(final boolean hasOlder, final boolean hasPendingTopRequests, final boolean blendedInboxEnabled,
                      final int unseenCount, final int pendingRequestsCount, final long seqId, final long unseenCountTimestamp,
                      final String oldestCursor, final InboxThreadModel[] threads) {
        this.hasOlder = hasOlder;
        this.hasPendingTopRequests = hasPendingTopRequests;
        this.blendedInboxEnabled = blendedInboxEnabled;
        this.unseenCount = unseenCount;
        this.pendingRequestsCount = pendingRequestsCount;
        this.unseenCountTimestamp = unseenCountTimestamp;
        this.oldestCursor = oldestCursor;
        this.threads = threads;
        this.seqId = seqId;
    }

    public boolean isHasOlder() {
        return hasOlder;
    }

    public boolean isHasPendingTopRequests() {
        return hasPendingTopRequests;
    }

    public boolean isBlendedInboxEnabled() {
        return blendedInboxEnabled;
    }

    public int getUnseenCount() {
        return unseenCount;
    }

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public long getUnseenCountTimestamp() {
        return unseenCountTimestamp;
    }

    public long getSeqId() {
        return seqId;
    }

    public String getOldestCursor() {
        return oldestCursor;
    }

    public void setOldestCursor(final String oldestCursor) {
        this.oldestCursor = oldestCursor;
    }

    public InboxThreadModel[] getThreads() {
        return threads;
    }
}