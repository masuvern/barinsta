package awais.instagrabber.repositories.responses.directmessages;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DirectThread {
    private final String threadId;
    private final String threadV2Id;
    private final List<DirectUser> users;
    private final List<String> leftUsers;
    private final List<String> adminUserIds;
    private final List<DirectItem> items;
    private final long lastActivityAt;
    private final boolean muted;
    private final boolean isPin;
    private final boolean named;
    private final boolean canonical;
    private final boolean pending;
    private final boolean archived;
    private final boolean valuedRequest;
    private final String threadType;
    private final long viewerId;
    private final String threadTitle;
    private final String pendingScore;
    private final long folder;
    private final boolean vcMuted;
    private final boolean isGroup;
    private final boolean mentionsMuted;
    private final DirectUser inviter;
    private final boolean hasOlder;
    private final boolean hasNewer;
    private final Map<Long, DirectThreadLastSeenAt> lastSeenAt;
    private final String newestCursor;
    private final String oldestCursor;
    private final boolean isSpam;
    private final DirectItem lastPermanentItem;
    private final DirectThreadDirectStory directStory;

    public DirectThread(final String threadId,
                        final String threadV2Id,
                        final List<DirectUser> users,
                        final List<String> leftUsers,
                        final List<String> adminUserIds,
                        final List<DirectItem> items,
                        final long lastActivityAt,
                        final boolean muted,
                        final boolean isPin,
                        final boolean named,
                        final boolean canonical,
                        final boolean pending,
                        final boolean archived,
                        final boolean valuedRequest,
                        final String threadType,
                        final long viewerId,
                        final String threadTitle,
                        final String pendingScore,
                        final long folder,
                        final boolean vcMuted,
                        final boolean isGroup,
                        final boolean mentionsMuted,
                        final DirectUser inviter,
                        final boolean hasOlder,
                        final boolean hasNewer,
                        final Map<Long, DirectThreadLastSeenAt> lastSeenAt,
                        final String newestCursor,
                        final String oldestCursor,
                        final boolean isSpam,
                        final DirectItem lastPermanentItem,
                        final DirectThreadDirectStory directStory) {
        this.threadId = threadId;
        this.threadV2Id = threadV2Id;
        this.users = users;
        this.leftUsers = leftUsers;
        this.adminUserIds = adminUserIds;
        this.items = items;
        this.lastActivityAt = lastActivityAt;
        this.muted = muted;
        this.isPin = isPin;
        this.named = named;
        this.canonical = canonical;
        this.pending = pending;
        this.archived = archived;
        this.valuedRequest = valuedRequest;
        this.threadType = threadType;
        this.viewerId = viewerId;
        this.threadTitle = threadTitle;
        this.pendingScore = pendingScore;
        this.folder = folder;
        this.vcMuted = vcMuted;
        this.isGroup = isGroup;
        this.mentionsMuted = mentionsMuted;
        this.inviter = inviter;
        this.hasOlder = hasOlder;
        this.hasNewer = hasNewer;
        this.lastSeenAt = lastSeenAt;
        this.newestCursor = newestCursor;
        this.oldestCursor = oldestCursor;
        this.isSpam = isSpam;
        this.lastPermanentItem = lastPermanentItem;
        this.directStory = directStory;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getThreadV2Id() {
        return threadV2Id;
    }

    public List<DirectUser> getUsers() {
        return users;
    }

    public List<String> getLeftUsers() {
        return leftUsers;
    }

    public List<String> getAdminUserIds() {
        return adminUserIds;
    }

    public List<DirectItem> getItems() {
        return items;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isPin() {
        return isPin;
    }

    public boolean isNamed() {
        return named;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public boolean isPending() {
        return pending;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isValuedRequest() {
        return valuedRequest;
    }

    public String getThreadType() {
        return threadType;
    }

    public long getViewerId() {
        return viewerId;
    }

    public String getThreadTitle() {
        return threadTitle;
    }

    public String getPendingScore() {
        return pendingScore;
    }

    public long getFolder() {
        return folder;
    }

    public boolean isVcMuted() {
        return vcMuted;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isMentionsMuted() {
        return mentionsMuted;
    }

    public DirectUser getInviter() {
        return inviter;
    }

    public boolean hasOlder() {
        return hasOlder;
    }

    public boolean hasNewer() {
        return hasNewer;
    }

    public Map<Long, DirectThreadLastSeenAt> getLastSeenAt() {
        return lastSeenAt;
    }

    public String getNewestCursor() {
        return newestCursor;
    }

    public String getOldestCursor() {
        return oldestCursor;
    }

    public boolean isSpam() {
        return isSpam;
    }

    public DirectItem getLastPermanentItem() {
        return lastPermanentItem;
    }

    public DirectThreadDirectStory getDirectStory() {
        return directStory;
    }

    @Nullable
    public DirectItem getFirstDirectItem() {
        DirectItem firstItem = null;
        if (!items.isEmpty()) {
            int position = 0;
            while (firstItem == null && position < items.size()) {
                firstItem = items.get(position);
                position++;
            }
        }
        return firstItem;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DirectThread that = (DirectThread) o;
        return Objects.equals(threadId, that.threadId) &&
                Objects.equals(threadV2Id, that.threadV2Id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadId, threadV2Id);
    }
}
