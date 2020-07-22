package awais.instagrabber.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.File;
import java.io.IOException;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.StoriesAdapter;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.customviews.helpers.SwipeGestureListener;
import awais.instagrabber.databinding.ActivityStoryViewerBinding;
import awais.instagrabber.interfaces.SwipeEvent;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_THRESHOLD;
import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class StoryViewer extends BaseLanguageActivity {
    private final StoriesAdapter storiesAdapter = new StoriesAdapter(null, new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Object tag = v.getTag();
            if (tag instanceof StoryModel) {
                currentStory = (StoryModel) tag;
                slidePos = currentStory.getPosition();
                refreshStory();
            }
        }
    });
    private ActivityStoryViewerBinding storyViewerBinding;
    private StoryModel[] storyModels;
    private GestureDetectorCompat gestureDetector;
    private SimpleExoPlayer player;
    private SwipeEvent swipeEvent;
    private MenuItem menuDownload;
    private StoryModel currentStory;
    private String url, username;
    private int slidePos = 0, lastSlidePos = 0;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storyViewerBinding = ActivityStoryViewerBinding.inflate(getLayoutInflater());
        setContentView(storyViewerBinding.getRoot());

        setSupportActionBar(storyViewerBinding.toolbar.toolbar);

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_STORIES)
                || (storyModels = (StoryModel[]) intent.getSerializableExtra(Constants.EXTRAS_STORIES)) == null) {
            Utils.errorFinish(this);
            return;
        }

        username = intent.getStringExtra(Constants.EXTRAS_USERNAME);
        final String highlight = intent.getStringExtra(Constants.EXTRAS_HIGHLIGHT);
        final boolean hasUsername = !Utils.isEmpty(username);
        final boolean hasHighlight = !Utils.isEmpty(highlight);

        if (hasUsername) {
            storyViewerBinding.toolbar.toolbar.setTitle(username);
            storyViewerBinding.toolbar.toolbar.setOnClickListener(v -> {
                searchUsername(username);
            });
            if (hasHighlight) storyViewerBinding.toolbar.toolbar.setSubtitle(getString(R.string.title_highlight, highlight));
            else storyViewerBinding.toolbar.toolbar.setSubtitle(R.string.title_user_story);
        }

        storyViewerBinding.storiesList.setVisibility(View.GONE);
        storyViewerBinding.storiesList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        storyViewerBinding.storiesList.setAdapter(storiesAdapter);

        swipeEvent = new SwipeEvent() {
            private final int storiesLen = storyModels != null ? storyModels.length : 0;

            @Override
            public void onSwipe(final boolean isRightSwipe) {
                if (storyModels != null && storiesLen > 0) {
                    if (((slidePos == storiesLen - 1 && isRightSwipe == false) || (slidePos == 0 && isRightSwipe == true))
                            && intent.hasExtra(Constants.FEED)) {
                        final FeedStoryModel[] storyFeed = (FeedStoryModel[]) intent.getSerializableExtra(Constants.FEED);
                        final int index = intent.getIntExtra(Constants.FEED_ORDER, 1738);
                        final FeedStoryModel feedStoryModel = isRightSwipe ?
                            (storyFeed.length == 0 ? null : storyFeed[index-1]) :
                            (storyFeed.length == index+1 ? null : storyFeed[index+1]);
                        final StoryModel[] nextStoryModels = feedStoryModel.getStoryModels();

                        if (feedStoryModel != null) {
                            final Intent newIntent = new Intent(getApplicationContext(), StoryViewer.class)
                                    .putExtra(Constants.EXTRAS_STORIES, nextStoryModels)
                                    .putExtra(Constants.EXTRAS_USERNAME, feedStoryModel.getProfileModel().getUsername())
                                    .putExtra(Constants.FEED, storyFeed)
                                    .putExtra(Constants.FEED_ORDER, isRightSwipe ? (index-1) : (index+1));
                            newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(newIntent);
                        }
                        else Toast.makeText(getApplicationContext(), R.string.no_more_stories, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        if (isRightSwipe) {
                            if (--slidePos <= 0) slidePos = 0;
                        } else if (++slidePos >= storiesLen) slidePos = storiesLen - 1;

                        currentStory = storyModels[slidePos];
                        slidePos = currentStory.getPosition();
                        refreshStory();
                    }
                }
            }
        };
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener(swipeEvent));

        viewPost();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void viewPost() {
        lastSlidePos = 0;
        storyViewerBinding.storiesList.setVisibility(View.GONE);
        storiesAdapter.setData(null);

        if (menuDownload != null) menuDownload.setVisible(false);

        storyViewerBinding.playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        storyViewerBinding.imageViewer.setOnSingleFlingListener((e1, e2, velocityX, velocityY) -> {
            final float diffX = e2.getX() - e1.getX();
            try {
                if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    swipeEvent.onSwipe(diffX > 0);
                    return true;
                }
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.ACTIVITY_STORY_VIEWER, "viewPost",
                            new Pair<>("swipeEvent", swipeEvent),
                            new Pair<>("diffX", diffX));
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
            return false;
        });

        storyViewerBinding.viewStoryPost.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) startActivity(new Intent(this, PostViewer.class)
                    .putExtra(Constants.EXTRAS_POST, new PostModel(tag.toString())));
        });

        storiesAdapter.setData(storyModels);
        if (storyModels.length > 1) storyViewerBinding.storiesList.setVisibility(View.VISIBLE);

        currentStory = storyModels[0];
        refreshStory();
    }

    private void setupVideo() {
        storyViewerBinding.playerView.setVisibility(View.VISIBLE);
        storyViewerBinding.progressView.setVisibility(View.GONE);
        storyViewerBinding.imageViewer.setVisibility(View.GONE);
        storyViewerBinding.imageViewer.setImageDrawable(null);

        player = new SimpleExoPlayer.Builder(this).build();
        storyViewerBinding.playerView.setPlayer(player);
        player.setPlayWhenReady(settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));

        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(this, "instagram"))
                .createMediaSource(Uri.parse(url));
        mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            @Override
            public void onLoadCompleted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                if (menuDownload != null) menuDownload.setVisible(true);
                storyViewerBinding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadStarted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                if (menuDownload != null) menuDownload.setVisible(true);
                storyViewerBinding.progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadCanceled(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                storyViewerBinding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
                if (menuDownload != null) menuDownload.setVisible(false);
                storyViewerBinding.progressView.setVisibility(View.GONE);
            }
        });
        player.prepare(mediaSource);

        storyViewerBinding.playerView.setOnClickListener(v -> {
            if (player != null) {
                if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                player.setPlayWhenReady(player.getPlaybackState() == Player.STATE_ENDED || !player.isPlaying());
            }
        });
    }

    private void setupImage() {
        storyViewerBinding.progressView.setVisibility(View.VISIBLE);
        storyViewerBinding.playerView.setVisibility(View.GONE);

        storyViewerBinding.imageViewer.setImageDrawable(null);
        storyViewerBinding.imageViewer.setVisibility(View.VISIBLE);
        storyViewerBinding.imageViewer.setZoomable(true);
        storyViewerBinding.imageViewer.setZoomTransitionDuration(420);
        storyViewerBinding.imageViewer.setMaximumScale(7.2f);

        Glide.with(this).load(url).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                storyViewerBinding.progressView.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                if (menuDownload != null) menuDownload.setVisible(true);
                storyViewerBinding.progressView.setVisibility(View.GONE);
                return false;
            }
        }).into(storyViewerBinding.imageViewer);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        menuDownload = menu.findItem(R.id.action_download);
        menuDownload.setVisible(true);
        menuDownload.setOnMenuItemClickListener(item -> {
            if (ContextCompat.checkSelfPermission(this, Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED)
                downloadStory();
            else
                ActivityCompat.requestPermissions(this, Utils.PERMS, 8020);
            return true;
        });

        return true;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED) downloadStory();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < 24) releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= 24) releasePlayer();
    }

    private void downloadStory() {
        int error = 0;
        if (currentStory != null) {
            File dir = new File(Environment.getExternalStorageDirectory(), "Download");

            if (settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
                final String customPath = settingsHelper.getString(FOLDER_PATH);
                if (!Utils.isEmpty(customPath)) dir = new File(customPath);
            }

            if (settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !Utils.isEmpty(username))
                dir = new File(dir, username);

            if (dir.exists() || dir.mkdirs()) {
                final String storyUrl = currentStory.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO ? currentStory.getVideoUrl() : currentStory.getStoryUrl();
                final File saveFile = new File(dir, currentStory.getStoryMediaId() + "_" + currentStory.getTimestamp()
                        + Utils.getExtensionFromModel(storyUrl, currentStory));

                new DownloadAsync(this, storyUrl, saveFile, result -> {
                    final int toastRes = result != null && result.exists() ? R.string.downloader_complete
                            : R.string.downloader_error_download_file;
                    Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show();
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            } else error = 1;
        } else error = 2;

        if (error == 1) Toast.makeText(this, R.string.downloader_error_creating_folder, Toast.LENGTH_SHORT).show();
        else if (error == 2) Toast.makeText(this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    }

    private void refreshStory() {
        if (storyViewerBinding.storiesList.getVisibility() == View.VISIBLE) {
            StoryModel item = storiesAdapter.getItemAt(lastSlidePos);
            if (item != null) {
                item.setCurrentSlide(false);
                storiesAdapter.notifyItemChanged(lastSlidePos, item);
            }

            item = storiesAdapter.getItemAt(slidePos);
            if (item != null) {
                item.setCurrentSlide(true);
                storiesAdapter.notifyItemChanged(slidePos, item);
            }
        }
        lastSlidePos = slidePos;

        final MediaItemType itemType = currentStory.getItemType();

        if (menuDownload != null) menuDownload.setVisible(false);
        url = itemType == MediaItemType.MEDIA_TYPE_VIDEO ? currentStory.getVideoUrl() : currentStory.getStoryUrl();

        final String shortCode = currentStory.getTappableShortCode();
        storyViewerBinding.viewStoryPost.setVisibility(shortCode != null ? View.VISIBLE : View.GONE);
        storyViewerBinding.viewStoryPost.setTag(shortCode);

        releasePlayer();
        if (itemType == MediaItemType.MEDIA_TYPE_VIDEO) setupVideo();
        else setupImage();
    }

    private void searchUsername(final String text) {
        if (Main.scanHack != null) {
            Main.scanHack.onResult(text);
            setResult(6969);
            Intent intent = new Intent(getApplicationContext(), Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.stop(true); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    public static int indexOfIntArray(Object[] array, Object key) {
        int returnvalue = -1;
        for (int i = 0; i < array.length; ++i) {
            if (key == array[i]) {
                returnvalue = i;
                break;
            }
        }
        return returnvalue;
    }
}