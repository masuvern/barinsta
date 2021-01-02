package awais.instagrabber.models;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;

import awais.instagrabber.utils.Utils;

public class CommentModel {
    private final ProfileModel profileModel;
    private final String id;
    private final String text;
    private long likes;
    private final long timestamp;
    private List<CommentModel> childCommentModels;
    private boolean liked, hasNextPage;
    private String endCursor;

    public CommentModel(final String id,
                        final String text,
                        final long timestamp,
                        final long likes,
                        final boolean liked,
                        final ProfileModel profileModel) {
        this.id = id;
        this.text = text;
        this.likes = likes;
        this.liked = liked;
        this.timestamp = timestamp;
        this.profileModel = profileModel;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    @NonNull
    public String getDateTime() {
        return Utils.datetimeParser.format(new Date(timestamp * 1000L));
    }

    public long getLikes() {
        return likes;
    }

    public boolean getLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.likes = liked ? likes + 1 : likes - 1;
        this.liked = liked;
    }

    public ProfileModel getProfileModel() {
        return profileModel;
    }

    public List<CommentModel> getChildCommentModels() {
        return childCommentModels;
    }

    public void setChildCommentModels(final List<CommentModel> childCommentModels) {
        this.childCommentModels = childCommentModels;
    }

    public void setPageCursor(final boolean hasNextPage, final String endCursor) {
        this.hasNextPage = hasNextPage;
        this.endCursor = endCursor;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public String getEndCursor() {
        return endCursor;
    }

    //    @NonNull
    //    @Override
    //    public String toString() {
    //        try {
    //            final JSONObject object = new JSONObject();
    //            object.put(Constants.EXTRAS_ID, id);
    //            object.put("text", text);
    //            object.put(Constants.EXTRAS_NAME, profileModel != null ? profileModel.getUsername() : "");
    //            if (childCommentModels != null) object.put("childComments", childCommentModels);
    //            return object.toString();
    //        } catch (Exception e) {
    //            return "{\"id\":\"" + id + "\", \"text\":\"" + text
    //                     //(text != null ? text.replaceAll("\"", "\\\\\"") : "")
    //                    + "\", \"name\":\"" + (profileModel != null ? profileModel.getUsername() : "") +
    //                    (childCommentModels != null ? "\", \"childComments\":" + childCommentModels.length : "\"") + '}';
    //        }
    //    }
}