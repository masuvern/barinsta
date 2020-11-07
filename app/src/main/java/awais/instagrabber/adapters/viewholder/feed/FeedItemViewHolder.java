package awais.instagrabber.adapters.viewholder.feed;

import android.text.method.LinkMovementMethod;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.databinding.ItemFeedBottomBinding;
import awais.instagrabber.databinding.ItemFeedTopBinding;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.TextUtils;

import static android.text.TextUtils.TruncateAt.END;

public abstract class FeedItemViewHolder extends RecyclerView.ViewHolder {
    public static final int MAX_LINES_COLLAPSED = 5;
    private final ItemFeedTopBinding topBinding;
    private final ItemFeedBottomBinding bottomBinding;
    private final FeedAdapterV2.FeedItemCallback feedItemCallback;

    public FeedItemViewHolder(@NonNull final View root,
                              final ItemFeedTopBinding topBinding,
                              final ItemFeedBottomBinding bottomBinding,
                              final FeedAdapterV2.FeedItemCallback feedItemCallback) {
        super(root);
        this.topBinding = topBinding;
        this.bottomBinding = bottomBinding;
        topBinding.title.setMovementMethod(new LinkMovementMethod());
        this.feedItemCallback = feedItemCallback;
    }

    public void bind(final FeedModel feedModel) {
        if (feedModel == null) {
            return;
        }
        setupProfilePic(feedModel);
        setupLocation(feedModel);
        bottomBinding.tvPostDate.setText(feedModel.getPostDate());
        setupComments(feedModel);
        setupCaption(feedModel);
        if (feedModel.getItemType() != MediaItemType.MEDIA_TYPE_SLIDER) {
            bottomBinding.btnDownload.setOnClickListener(v -> feedItemCallback.onDownloadClick(feedModel, -1));
        }
        bindItem(feedModel);
    }

    private void setupComments(final FeedModel feedModel) {
        final long commentsCount = feedModel.getCommentsCount();
        bottomBinding.commentsCount.setText(String.valueOf(commentsCount));
        bottomBinding.commentsCount.setOnClickListener(v -> feedItemCallback.onCommentsClick(feedModel));
    }

    private void setupProfilePic(final FeedModel feedModel) {
        final ProfileModel profileModel = feedModel.getProfileModel();
        if (profileModel != null) {
            topBinding.ivProfilePic.setOnClickListener(v -> feedItemCallback.onProfilePicClick(feedModel, topBinding.ivProfilePic));
            topBinding.ivProfilePic.setImageURI(profileModel.getSdProfilePic());
            setupTitle(feedModel);
        }
    }

    private void setupTitle(final FeedModel feedModel) {
        // final int titleLen = profileModel.getUsername().length() + 1;
        // final SpannableString spannableString = new SpannableString();
        // spannableString.setSpan(new CommentMentionClickSpan(), 0, titleLen, 0);
        final ProfileModel profileModel = feedModel.getProfileModel();
        final String title = "@" + profileModel.getUsername();
        topBinding.title.setText(title);
        topBinding.title.setOnClickListener(v -> feedItemCallback.onNameClick(feedModel, topBinding.ivProfilePic));
    }

    private void setupCaption(final FeedModel feedModel) {
        bottomBinding.viewerCaption.clearOnMentionClickListeners();
        bottomBinding.viewerCaption.clearOnHashtagClickListeners();
        bottomBinding.viewerCaption.clearOnURLClickListeners();
        bottomBinding.viewerCaption.clearOnEmailClickListeners();
        final CharSequence postCaption = feedModel.getPostCaption();
        final boolean captionEmpty = TextUtils.isEmpty(postCaption);
        bottomBinding.viewerCaption.setVisibility(captionEmpty ? View.GONE : View.VISIBLE);
        if (captionEmpty) return;
        bottomBinding.viewerCaption.setText(postCaption);
        bottomBinding.viewerCaption.setMaxLines(MAX_LINES_COLLAPSED);
        bottomBinding.viewerCaption.setEllipsize(END);
        bottomBinding.viewerCaption.setOnClickListener(v -> bottomBinding.getRoot().post(() -> {
            TransitionManager.beginDelayedTransition(bottomBinding.getRoot());
            if (bottomBinding.viewerCaption.getMaxLines() == MAX_LINES_COLLAPSED) {
                bottomBinding.viewerCaption.setMaxLines(Integer.MAX_VALUE);
                bottomBinding.viewerCaption.setEllipsize(null);
                return;
            }
            bottomBinding.viewerCaption.setMaxLines(MAX_LINES_COLLAPSED);
            bottomBinding.viewerCaption.setEllipsize(END);
        }));
        bottomBinding.viewerCaption.addOnMentionClickListener(autoLinkItem -> feedItemCallback.onMentionClick(autoLinkItem.getOriginalText()));
        bottomBinding.viewerCaption.addOnHashtagListener(autoLinkItem -> feedItemCallback.onHashtagClick(autoLinkItem.getOriginalText()));
        bottomBinding.viewerCaption.addOnEmailClickListener(autoLinkItem -> feedItemCallback.onEmailClick(autoLinkItem.getOriginalText()));
        bottomBinding.viewerCaption.addOnURLClickListener(autoLinkItem -> feedItemCallback.onURLClick(autoLinkItem.getOriginalText()));
    }

    private void setupLocation(final FeedModel feedModel) {
        final String locationName = feedModel.getLocationName();
        if (TextUtils.isEmpty(locationName)) {
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
            topBinding.location.setOnClickListener(v -> feedItemCallback.onLocationClick(feedModel));
        }
    }

    public abstract void bindItem(final FeedModel feedModel);
}