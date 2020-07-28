package awais.instagrabber.customviews.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.activities.CommentsViewer;
import awais.instagrabber.adapters.FeedAdapter;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

// wasted around 3 hours to get this working, made from scrach, forgot to take a shower so i'm gonna go take a shower (time: May 11, 2020 @ 8:09:30 PM)
public class VideoAwareRecyclerScroller extends RecyclerView.OnScrollListener {
    private static final Object LOCK = new Object();
    private LinearLayoutManager layoutManager;
    private View firstItemView, lastItemView;
    private int videoPosShown = -1, lastVideoPos = -1, lastChangedVideoPos, lastStoppedVideoPos, lastPlayedVideoPos;
    private boolean videoAttached = false;
    private final List<FeedModel> feedModels;
    ////////////////////////////////////////////////////
    private SimpleExoPlayer player;
    private ImageView btnMute;
    private final Context context;
    private final View.OnClickListener commentClickListener = new View.OnClickListener() {
        @Override
        public void onClick(@NonNull final View v) {
            final Object tag = v.getTag();
            if (tag instanceof FeedModel && context instanceof Activity) {
                if (player != null) player.setPlayWhenReady(false);
                ((Activity) context).startActivityForResult(new Intent(context, CommentsViewer.class)
                        .putExtra(Constants.EXTRAS_SHORTCODE, ((FeedModel) tag).getShortCode())
                        .putExtra(Constants.EXTRAS_POST, ((FeedModel) tag).getPostId())
                        .putExtra(Constants.EXTRAS_POST, ((FeedModel) tag).getProfileModel().getId()), 6969);
            }
        }
    };
    private final View.OnClickListener muteClickListener = v -> {
        if (player == null) return;
        final float intVol = player.getVolume() == 0f ? 1f : 0f;
        player.setVolume(intVol);
        if (btnMute != null) btnMute.setImageResource(intVol == 0f ? R.drawable.mute : R.drawable.vol);
        Utils.sessionVolumeFull = intVol == 1f;
    };
    private final VideoChangeCallback videoChangeCallback;
    // private final ScrollerVideoCallback videoCallback;
    // private View lastVideoHolder;
    // private int videoState = -1;

    public VideoAwareRecyclerScroller(final Context context, final List<FeedModel> feedModels,
                                      final VideoChangeCallback videoChangeCallback) {
        this.context = context;
        this.feedModels = feedModels;
        this.videoChangeCallback = videoChangeCallback;
    }

    @Override
    public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
        if (layoutManager == null) {
            final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) this.layoutManager = (LinearLayoutManager) layoutManager;
        }

        if (feedModels.size() > 0 && layoutManager != null) {
            int firstVisibleItemPos = layoutManager.findFirstCompletelyVisibleItemPosition();
            int lastVisibleItemPos = layoutManager.findLastCompletelyVisibleItemPosition();

            if (firstVisibleItemPos == -1 && lastVisibleItemPos == -1) {
                firstVisibleItemPos = layoutManager.findFirstVisibleItemPosition();
                lastVisibleItemPos = layoutManager.findLastVisibleItemPosition();
            }

            boolean processFirstItem = false, processLastItem = false;
            View currView;
            if (firstVisibleItemPos != -1) {
                currView = layoutManager.findViewByPosition(firstVisibleItemPos);
                if (currView != null && currView.getId() == R.id.videoHolder) {
                    firstItemView = currView;
                    processFirstItem = true;
                }
            }
            if (lastVisibleItemPos != -1) {
                currView = layoutManager.findViewByPosition(lastVisibleItemPos);
                if (currView != null && currView.getId() == R.id.videoHolder) {
                    lastItemView = currView;
                    processLastItem = true;
                }
            }

            final Rect visibleItemRect = new Rect();

            int firstVisibleItemHeight = 0, lastVisibleItemHeight = 0;

            final boolean isFirstItemVideoHolder = firstItemView != null && firstItemView.getId() == R.id.videoHolder;
            if (isFirstItemVideoHolder) {
                firstItemView.getGlobalVisibleRect(visibleItemRect);
                firstVisibleItemHeight = visibleItemRect.height();
            }
            final boolean isLastItemVideoHolder = lastItemView != null && lastItemView.getId() == R.id.videoHolder;
            if (isLastItemVideoHolder) {
                lastItemView.getGlobalVisibleRect(visibleItemRect);
                lastVisibleItemHeight = visibleItemRect.height();
            }

            if (processFirstItem && firstVisibleItemHeight > lastVisibleItemHeight) videoPosShown = firstVisibleItemPos;
            else if (processLastItem && lastVisibleItemHeight != 0) videoPosShown = lastVisibleItemPos;

            if (firstItemView != lastItemView) {
                final int mox = lastVisibleItemHeight - firstVisibleItemHeight;
                if (processLastItem && lastVisibleItemHeight > firstVisibleItemHeight) videoPosShown = lastVisibleItemPos;
                if ((processFirstItem || processLastItem) && mox >= 0) videoPosShown = lastVisibleItemPos;
            }

            if (lastChangedVideoPos != -1 && lastVideoPos != -1) {
                currView = layoutManager.findViewByPosition(lastChangedVideoPos);
                if (currView != null && currView.getId() == R.id.videoHolder &&
                        lastStoppedVideoPos != lastChangedVideoPos && lastPlayedVideoPos != lastChangedVideoPos) {
                    lastStoppedVideoPos = lastChangedVideoPos;
                    stopVideo(lastChangedVideoPos, recyclerView, currView);
                }

                currView = layoutManager.findViewByPosition(lastVideoPos);
                if (currView != null && currView.getId() == R.id.videoHolder) {
                    final Rect rect = new Rect();
                    currView.getGlobalVisibleRect(rect);

                    final int holderTop = currView.getTop();
                    final int holderHeight = currView.getBottom() - holderTop;
                    final int halfHeight = holderHeight / 2;
                    //halfHeight -= halfHeight / 5;

                    if (rect.height() < halfHeight) {
                        if (lastStoppedVideoPos != lastVideoPos) {
                            lastStoppedVideoPos = lastVideoPos;
                            stopVideo(lastVideoPos, recyclerView, currView);
                        }
                    } else if (lastPlayedVideoPos != lastVideoPos) {
                        lastPlayedVideoPos = lastVideoPos;
                        playVideo(lastVideoPos, recyclerView, currView);
                    }
                }

                if (lastChangedVideoPos != lastVideoPos) lastChangedVideoPos = lastVideoPos;
            }

            if (lastVideoPos != -1 && lastVideoPos != videoPosShown) {
                if (videoAttached) {
                    //if ((currView = layoutManager.findViewByPosition(lastVideoPos)) != null && currView.getId() == R.id.videoHolder)
                    releaseVideo(lastVideoPos, recyclerView, null);
                    videoAttached = false;
                }
            }
            if (videoPosShown != -1) {
                lastVideoPos = videoPosShown;
                if (!videoAttached) {
                    if ((currView = layoutManager.findViewByPosition(videoPosShown)) != null && currView.getId() == R.id.videoHolder)
                        attachVideo(videoPosShown, recyclerView, currView);
                    videoAttached = true;
                }
            }
        }
    }

    private synchronized void attachVideo(final int itemPos, final RecyclerView recyclerView, final View itemView) {
        synchronized (LOCK) {
            if (recyclerView != null) {
                final RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                if (adapter instanceof FeedAdapter) {
                    final SimpleExoPlayer pagerPlayer = ((FeedAdapter) adapter).pagerPlayer;
                    if (pagerPlayer != null) pagerPlayer.setPlayWhenReady(false);
                }
            }

            if (player != null) {
                player.stop(true);
                player.release();
                player = null;
            }

            player = new SimpleExoPlayer.Builder(context).build();

            if (itemView != null) {
                final Object tag = itemView.getTag();

                final View btnComments = itemView.findViewById(R.id.btnComments);
                if (btnComments != null && tag instanceof FeedModel) {
                    final FeedModel feedModel = (FeedModel) tag;

                    if (feedModel.getCommentsCount() <= 0) btnComments.setEnabled(false);
                    else {
                        btnComments.setTag(feedModel);
                        btnComments.setEnabled(true);
                        btnComments.setOnClickListener(commentClickListener);
                    }
                }

                final PlayerView playerView = itemView.findViewById(R.id.playerView);
                if (playerView == null) return;
                playerView.setPlayer(player);

                if (player != null) {
                    btnMute = itemView.findViewById(R.id.btnMute);

                    float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
                    if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;
                    player.setVolume(vol);

                    if (btnMute != null) {
                        btnMute.setVisibility(View.VISIBLE);
                        btnMute.setImageResource(vol == 0f ? R.drawable.vol : R.drawable.mute);
                        btnMute.setOnClickListener(muteClickListener);
                    }

                    player.setPlayWhenReady(settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));

                    final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(context, "instagram"))
                            .createMediaSource(Uri.parse(feedModels.get(itemPos).getDisplayUrl()));

                    player.setRepeatMode(Player.REPEAT_MODE_ALL);
                    player.prepare(mediaSource);
                    player.setVolume(vol);

                    playerView.setOnClickListener(v -> {
                        if (player.getPlayWhenReady() == true) {
                            player.setPlayWhenReady(false);
                            player.getPlaybackState();
                        }
                        else {
                            player.setPlayWhenReady(true);
                            player.getPlaybackState();
                        }
                    });
                }
            }

            if (videoChangeCallback != null) videoChangeCallback.playerChanged(itemPos, player);
        }
    }

    private void releaseVideo(final int itemPos, final RecyclerView recyclerView, final View itemView) {
//                    Log.d("AWAISKING_APP", "release: " + itemPos);
//                    if (player != null) {
//                        player.stop(true);
//                        player.release();
//                    }
//                    player = null;
    }

    private void playVideo(final int itemPos, final RecyclerView recyclerView, final View itemView) {
//                    if (player != null) {
//                        final int playbackState = player.getPlaybackState();
//                        if (!player.isPlaying()
//                               || playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
//                        ) {
//                            player.setPlayWhenReady(true);
//                        }
//                    }
//                    if (player != null) {
//                        player.setPlayWhenReady(true);
//                        player.getPlaybackState();
//                    }
    }

    private void stopVideo(final int itemPos, final RecyclerView recyclerView, final View itemView) {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    public interface VideoChangeCallback {
        void playerChanged(final int itemPos, final SimpleExoPlayer player);
    }
}