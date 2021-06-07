package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

import java.util.List;

import awais.instagrabber.models.Comment;

public class CommentsFetchResponse {
    private final int commentCount;
    private final String nextMinId;
    private final List<Comment> comments;
    private final boolean hasMoreComments;

    public CommentsFetchResponse(final int commentCount,
                                 final String nextMinId,
                                 final List<Comment> comments,
                                 final boolean hasMoreComments) {
        this.commentCount = commentCount;
        this.nextMinId = nextMinId;
        this.comments = comments;
        this.hasMoreComments = hasMoreComments;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getNextMinId() {
        return nextMinId;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public boolean getHasMoreComments() {
        return hasMoreComments;
    }

    @NonNull
    @Override
    public String toString() {
        return "CommentsFetchResponse{" +
                "commentCount=" + commentCount +
                ", nextMinId='" + nextMinId + '\'' +
                ", comments=" + comments +
                ", hasMoreComments=" + hasMoreComments +
                '}';
    }
}
