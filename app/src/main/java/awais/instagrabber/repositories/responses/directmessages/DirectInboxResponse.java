package awais.instagrabber.repositories.responses.directmessages;

import awais.instagrabber.repositories.responses.User;

public class DirectInboxResponse {
    private final User viewer;
    private final DirectInbox inbox;
    private final long seqId;
    private final long snapshotAtMs;
    private final int pendingRequestsTotal;
    private final User mostRecentInviter;
    private final String status;

    public DirectInboxResponse(final User viewer,
                               final DirectInbox inbox,
                               final long seqId,
                               final long snapshotAtMs,
                               final int pendingRequestsTotal,
                               final User mostRecentInviter,
                               final String status) {
        this.viewer = viewer;
        this.inbox = inbox;
        this.seqId = seqId;
        this.snapshotAtMs = snapshotAtMs;
        this.pendingRequestsTotal = pendingRequestsTotal;
        this.mostRecentInviter = mostRecentInviter;
        this.status = status;
    }

    public User getViewer() {
        return viewer;
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

    public User getMostRecentInviter() {
        return mostRecentInviter;
    }

    public String getStatus() {
        return status;
    }
}
