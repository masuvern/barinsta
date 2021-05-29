package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

import java.util.List;

import awais.instagrabber.models.Comment;

public class ChildCommentsFetchResponse {
    private final int childCommentCount;
    private final String nextMinId;
    private final List<Comment> childComments;

    public ChildCommentsFetchResponse(final int childCommentCount,
                                      final String nextMinId, // unconfirmed
                                      final List<Comment> childComments) {
        this.childCommentCount = childCommentCount;
        this.nextMinId = nextMinId;
        this.childComments = childComments;
    }

    public int getChildCommentCount() {
        return childCommentCount;
    }

    public String getNextMinId() {
        return nextMinId;
    }

    public boolean hasNext() {
        return nextMinId != null;
    }

    public List<Comment> getChildComments() {
        return childComments;
    }

    @NonNull
    @Override
    public String toString() {
        return "CommentsFetchResponse{" +
                "childCommentCount=" + childCommentCount +
                ", nextMinId='" + nextMinId + '\'' +
                ", childComments=" + childComments +
                '}';
    }
}
