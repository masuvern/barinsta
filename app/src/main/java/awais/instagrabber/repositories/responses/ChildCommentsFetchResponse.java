package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

import java.util.List;

import awais.instagrabber.models.Comment;

public class ChildCommentsFetchResponse {
    private final int childCommentCount;
    private final String nextMaxChildCursor;
    private final List<Comment> childComments;
    private final boolean hasMoreTailChildComments;

    public ChildCommentsFetchResponse(final int childCommentCount,
                                      final String nextMaxChildCursor,
                                      final List<Comment> childComments,
                                      final boolean hasMoreTailChildComments) {
        this.childCommentCount = childCommentCount;
        this.nextMaxChildCursor = nextMaxChildCursor;
        this.childComments = childComments;
        this.hasMoreTailChildComments = hasMoreTailChildComments;
    }

    public int getChildCommentCount() {
        return childCommentCount;
    }

    public String getNextMaxChildCursor() {
        return nextMaxChildCursor;
    }

    public boolean getHasMoreTailChildComments() {
        return hasMoreTailChildComments;
    }

    public List<Comment> getChildComments() {
        return childComments;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChildCommentsFetchResponse{" +
                "childCommentCount=" + childCommentCount +
                ", nextMaxChildCursor='" + nextMaxChildCursor + '\'' +
                ", childComments=" + childComments +
                ", hasMoreTailChildComments=" + hasMoreTailChildComments +
                '}';
    }
}
