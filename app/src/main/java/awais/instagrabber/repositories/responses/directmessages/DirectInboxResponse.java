package awais.instagrabber.repositories.responses.directmessages;

public class DirectInboxResponse {
    private final DirectInbox inbox;
    private final long seqId;
    private final long snapshotAtMs;
    private final int pendingRequestsTotal;
    private final DirectUser mostRecentInviter;
    private final String status;

    public DirectInboxResponse(final DirectInbox inbox,
                               final long seqId,
                               final long snapshotAtMs,
                               final int pendingRequestsTotal,
                               final DirectUser mostRecentInviter,
                               final String status) {
        this.inbox = inbox;
        this.seqId = seqId;
        this.snapshotAtMs = snapshotAtMs;
        this.pendingRequestsTotal = pendingRequestsTotal;
        this.mostRecentInviter = mostRecentInviter;
        this.status = status;
    }

    public DirectInbox getInbox() {
        return inbox;
    }

    public long getSeqId() {
        return seqId;
    }

    public long getSnapshotAtMs() {
        return snapshotAtMs;
    }

    public int getPendingRequestsTotal() {
        return pendingRequestsTotal;
    }

    public DirectUser getMostRecentInviter() {
        return mostRecentInviter;
    }

    public String getStatus() {
        return status;
    }
}
