package awais.instagrabber.adapters.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.ui.PlayerView;

import awais.instagrabber.R;
import awais.instagrabber.customviews.RamboTextView;

public final class FeedItemViewHolder extends RecyclerView.ViewHolder {
    public final ImageView profilePic, btnMute, btnDownload;
    public final TextView username, commentsCount, videoViews, mediaCounter, tvPostDate;
    public final RamboTextView viewerCaption;
    public final View btnComments, videoViewsParent, viewPost;
    public final ViewPager mediaList;
    public final PhotoView imageView;
    public final PlayerView playerView;

    public FeedItemViewHolder(@NonNull final View itemView) {
        super(itemView);

        // common
        viewerCaption = itemView.findViewById(R.id.viewerCaption);
        btnDownload = itemView.findViewById(R.id.btnDownload);
        btnComments = itemView.findViewById(R.id.btnComments);
        profilePic = itemView.findViewById(R.id.ivProfilePic);
        tvPostDate = itemView.findViewById(R.id.tvPostDate);
        viewPost = itemView.findViewById(R.id.viewStoryPost);
        username = itemView.findViewById(R.id.title);

        // video view
        btnMute = itemView.findViewById(R.id.btnMute);
        videoViews = itemView.findViewById(R.id.tvVideoViews);
        commentsCount = btnComments.findViewById(R.id.commentsCount);
        videoViewsParent = videoViews != null ? (View) videoViews.getParent() : null;

        // slider view
        mediaCounter = itemView.findViewById(R.id.mediaCounter);

        // different types
        mediaList = itemView.findViewById(R.id.media_list);
        imageView = itemView.findViewById(R.id.imageViewer);
        playerView = itemView.findViewById(R.id.playerView);
    }
}