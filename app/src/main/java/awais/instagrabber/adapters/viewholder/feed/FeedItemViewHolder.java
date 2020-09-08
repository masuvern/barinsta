package awais.instagrabber.adapters.viewholder.feed;

import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.customviews.CommentMentionClickSpan;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.databinding.ItemFeedBottomBinding;
import awais.instagrabber.databinding.ItemFeedTopBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Utils;

public abstract class FeedItemViewHolder extends RecyclerView.ViewHolder {
    public static final int MAX_CHARS = 255;
    private final ItemFeedTopBinding topBinding;
    private final ItemFeedBottomBinding bottomBinding;
    private final MentionClickListener mentionClickListener;

    public FeedItemViewHolder(@NonNull final View root,
                              final ItemFeedTopBinding topBinding,
                              final ItemFeedBottomBinding bottomBinding,
                              final MentionClickListener mentionClickListener,
                              final View.OnClickListener clickListener,
                              final View.OnLongClickListener longClickListener) {
        super(root);
        this.topBinding = topBinding;
        this.bottomBinding = bottomBinding;
        this.mentionClickListener = mentionClickListener;
        // topBinding.title.setMovementMethod(new LinkMovementMethod());
        bottomBinding.btnComments.setOnClickListener(clickListener);
        topBinding.viewStoryPost.setOnClickListener(clickListener);
        topBinding.ivProfilePic.setOnClickListener(clickListener);
        bottomBinding.btnDownload.setOnClickListener(clickListener);
        bottomBinding.viewerCaption.setOnClickListener(clickListener);
        bottomBinding.viewerCaption.setOnLongClickListener(longClickListener);
        bottomBinding.viewerCaption.setMentionClickListener(mentionClickListener);
    }

    public void bind(final FeedModel feedModel) {
        if (feedModel == null) {
            return;
        }
        topBinding.viewStoryPost.setTag(feedModel);
        topBinding.ivProfilePic.setTag(feedModel);
        bottomBinding.btnDownload.setTag(feedModel);
        bottomBinding.viewerCaption.setTag(feedModel);
        bottomBinding.btnComments.setTag(feedModel);
        final ProfileModel profileModel = feedModel.getProfileModel();
        if (profileModel != null) {
            topBinding.ivProfilePic.setImageURI(profileModel.getSdProfilePic());
            final int titleLen = profileModel.getUsername().length() + 1;
            final SpannableString spannableString = new SpannableString("@" + profileModel.getUsername());
            spannableString.setSpan(new CommentMentionClickSpan(), 0, titleLen, 0);
            topBinding.title.setText(spannableString);
            topBinding.title.setMentionClickListener(
                    (view, text, isHashtag, isLocation) -> mentionClickListener.onClick(null, profileModel.getUsername(), false, false));
        }
        bottomBinding.tvPostDate.setText(feedModel.getPostDate());
        final long commentsCount = feedModel.getCommentsCount();
        bottomBinding.commentsCount.setText(String.valueOf(commentsCount));

        final String locationName = feedModel.getLocationName();
        final String locationId = feedModel.getLocationId();
        setLocation(locationName, locationId);
        CharSequence postCaption = feedModel.getPostCaption();
        final boolean captionEmpty = Utils.isEmpty(postCaption);
        bottomBinding.viewerCaption.setVisibility(captionEmpty ? View.GONE : View.VISIBLE);
        if (!captionEmpty) {
            if (Utils.hasMentions(postCaption)) {
                postCaption = Utils.getMentionText(postCaption);
                feedModel.setPostCaption(postCaption);
                bottomBinding.viewerCaption.setText(postCaption, TextView.BufferType.SPANNABLE);
            } else {
                bottomBinding.viewerCaption.setText(postCaption);
            }
        }
        expandCollapseTextView(bottomBinding.viewerCaption, feedModel.getPostCaption());
        bindItem(feedModel);
    }

    private void setLocation(final String locationName, final String locationId) {
        if (Utils.isEmpty(locationName)) {
            topBinding.location.setVisibility(View.GONE);
            topBinding.title.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
            ));
        } else {
            topBinding.location.setVisibility(View.VISIBLE);
            topBinding.location.setText(locationName);
            topBinding.title.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            ));
            topBinding.location.setOnClickListener(v -> mentionClickListener.onClick(topBinding.location, locationId, false, true));
        }
    }

    /**
     * expands or collapses {@link RamboTextView} [stg idek why i wrote this documentation]
     *
     * @param textView the {@link RamboTextView} view, to expand and collapse
     * @param caption caption
     * @return isExpanded
     */
    public static boolean expandCollapseTextView(@NonNull final RamboTextView textView, final CharSequence caption) {
        if (Utils.isEmpty(caption)) return false;

        final TextView.BufferType bufferType = caption instanceof Spanned ? TextView.BufferType.SPANNABLE : TextView.BufferType.NORMAL;

        if (textView.isCaptionExpanded()) {
            textView.setText(caption, bufferType);
            textView.setCaptionIsExpanded(false);
            return true;
        }
        int i = Utils.indexOfChar(caption, '\r', 0);
        if (i == -1) i = Utils.indexOfChar(caption, '\n', 0);
        if (i == -1) i = MAX_CHARS;

        final int captionLen = caption.length();
        final int minTrim = Math.min(MAX_CHARS, i);
        if (captionLen <= minTrim) return false;

        if (Utils.hasMentions(caption))
            textView.setText(Utils.getMentionText(caption), TextView.BufferType.SPANNABLE);
        textView.setCaptionIsExpandable(true);
        textView.setCaptionIsExpanded(true);
        return true;
    }

    public abstract void bindItem(final FeedModel feedModel);
}