package awais.instagrabber.customviews;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutExoCustomControlsBinding;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.utils.TextUtils;

import static com.google.android.exoplayer2.C.TIME_UNSET;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;

public class VideoPlayerViewHelper implements Player.EventListener {
    private static final String TAG = "VideoPlayerViewHelper";

    private final Context context;
    private final awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding binding;
    private final float initialVolume;
    private final float thumbnailAspectRatio;
    private final String thumbnailUrl;
    private final boolean loadPlayerOnClick;
    private final awais.instagrabber.databinding.LayoutExoCustomControlsBinding controlsBinding;
    private final VideoPlayerCallback videoPlayerCallback;
    private final String videoUrl;
    private final DefaultDataSourceFactory dataSourceFactory;
    private SimpleExoPlayer player;
    private PopupMenu speedPopup;

    public VideoPlayerViewHelper(@NonNull final Context context,
                                 @NonNull final LayoutVideoPlayerWithThumbnailBinding binding,
                                 @NonNull final String videoUrl,
                                 final float initialVolume,
                                 final float thumbnailAspectRatio,
                                 final String thumbnailUrl,
                                 final boolean loadPlayerOnClick,
                                 final LayoutExoCustomControlsBinding controlsBinding,
                                 final VideoPlayerCallback videoPlayerCallback) {
        this.context = context;
        this.binding = binding;
        this.initialVolume = initialVolume;
        this.thumbnailAspectRatio = thumbnailAspectRatio;
        this.thumbnailUrl = thumbnailUrl;
        this.loadPlayerOnClick = loadPlayerOnClick;
        this.controlsBinding = controlsBinding;
        this.videoPlayerCallback = videoPlayerCallback;
        this.videoUrl = videoUrl;
        this.dataSourceFactory = new DefaultDataSourceFactory(binding.getRoot().getContext(), "instagram");
        bind();
    }

    private void bind() {
        binding.thumbnailParent.setOnClickListener(v -> {
            if (videoPlayerCallback != null) {
                videoPlayerCallback.onThumbnailClick();
            }
            if (loadPlayerOnClick) {
                loadPlayer();
            }
        });
        setThumbnail();
        setupControls();
    }

    private void setThumbnail() {
        binding.thumbnail.setAspectRatio(thumbnailAspectRatio);
        final ImageRequest thumbnailRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(thumbnailUrl))
                                                                 .build();
        final DraweeController controller = Fresco.newDraweeControllerBuilder()
                                                  .setControllerListener(new BaseControllerListener<ImageInfo>() {
                                                      @Override
                                                      public void onFailure(final String id, final Throwable throwable) {
                                                          if (videoPlayerCallback != null) {
                                                              videoPlayerCallback.onThumbnailLoaded();
                                                          }
                                                      }

                                                      @Override
                                                      public void onFinalImageSet(final String id,
                                                                                  final ImageInfo imageInfo,
                                                                                  final Animatable animatable) {
                                                          if (videoPlayerCallback != null) {
                                                              videoPlayerCallback.onThumbnailLoaded();
                                                          }
                                                      }
                                                  })
                                                  .setImageRequest(thumbnailRequest)
                                                  .build();
        binding.thumbnail.setController(controller);
    }

    private void loadPlayer() {
        if (videoUrl == null) return;
        if (binding.root.getDisplayedChild() == 0) {
            binding.root.showNext();
        }
        if (videoPlayerCallback != null) {
            videoPlayerCallback.onPlayerViewLoaded();
        }
        player = (SimpleExoPlayer) binding.playerView.getPlayer();
        if (player != null) {
            player.release();
        }
        player = new SimpleExoPlayer.Builder(context)
                .setLooper(Looper.getMainLooper())
                .build();
        player.addListener(this);
        player.setVolume(initialVolume);
        player.setPlayWhenReady(true);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        final ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
        final MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        final ProgressiveMediaSource mediaSource = sourceFactory.createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        setupControls();
        player.prepare();
        binding.playerView.setPlayer(player);
    }

    private void setupControls() {
        if (controlsBinding == null) return;
        binding.playerView.setUseController(false);
        if (player == null) {
            enableControls(false);
            controlsBinding.playPause.setEnabled(true);
            controlsBinding.playPause.setOnClickListener(v -> binding.thumbnailParent.performClick());
            return;
        }
        enableControls(true);
        final Handler handler = new Handler();
        final long initialDelay = 0;
        final long recurringDelay = 60;
        final Runnable positionChecker = new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(this);
                if (player == null) return;
                final long currentPosition = player.getCurrentPosition();
                final long duration = player.getDuration();
                if (duration == TIME_UNSET) {
                    controlsBinding.timeline.setValueFrom(0);
                    controlsBinding.timeline.setValueTo(0);
                    controlsBinding.timeline.setEnabled(false);
                    return;
                }
                controlsBinding.timeline.setValue(Math.min(currentPosition, duration));
                controlsBinding.fromTime.setText(TextUtils.millisToTimeString(currentPosition));
                handler.postDelayed(this, recurringDelay);
            }
        };
        updatePlayPauseDrawable(player.getPlayWhenReady());
        updateMuteIcon(player.getVolume());
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(final int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                    case STATE_IDLE:
                    case STATE_ENDED:
                        handler.removeCallbacks(positionChecker);
                        return;
                    case STATE_READY:
                        setupTimeline();
                        handler.postDelayed(positionChecker, initialDelay);
                        break;
                }
            }

            @Override
            public void onPlayWhenReadyChanged(final boolean playWhenReady, final int reason) {
                updatePlayPauseDrawable(playWhenReady);
            }
        });
        player.addAudioListener(new AudioListener() {
            @Override
            public void onVolumeChanged(final float volume) {
                updateMuteIcon(volume);
            }
        });
        controlsBinding.timeline.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser) return;
            long actualValue = (long) value;
            if (actualValue < 0) {
                actualValue = 0;
            } else if (actualValue > player.getDuration()) {
                actualValue = player.getDuration();
            }
            player.seekTo(actualValue);
        });
        controlsBinding.timeline.setLabelFormatter(value -> TextUtils.millisToTimeString((long) value));
        controlsBinding.playPause.setOnClickListener(v -> player.setPlayWhenReady(!player.getPlayWhenReady()));
        controlsBinding.mute.setOnClickListener(v -> toggleMute());
        controlsBinding.rewWithAmount.setOnClickListener(v -> {
            final long positionMs = player.getCurrentPosition() - 5000;
            player.seekTo(positionMs < 0 ? 0 : positionMs);
        });
        controlsBinding.ffWithAmount.setOnClickListener(v -> {
            long positionMs = player.getCurrentPosition() + 5000;
            long duration = player.getDuration();
            if (duration == TIME_UNSET) {
                duration = 0;
            }
            player.seekTo(Math.min(positionMs, duration));
        });
        controlsBinding.speed.setOnClickListener(this::showMenu);
    }

    private void setupTimeline() {
        final long duration = player.getDuration();
        controlsBinding.timeline.setEnabled(true);
        controlsBinding.timeline.setValueFrom(0);
        controlsBinding.timeline.setValueTo(duration);
        controlsBinding.fromTime.setText(TextUtils.millisToTimeString(0));
        controlsBinding.toTime.setText(TextUtils.millisToTimeString(duration));
    }

    private void enableControls(final boolean enable) {
        controlsBinding.speed.setEnabled(enable);
        controlsBinding.mute.setEnabled(enable);
        controlsBinding.ffWithAmount.setEnabled(enable);
        controlsBinding.rewWithAmount.setEnabled(enable);
        controlsBinding.fromTime.setEnabled(enable);
        controlsBinding.toTime.setEnabled(enable);
        controlsBinding.playPause.setEnabled(enable);
    }

    public void showMenu(View anchor) {
        PopupMenu popup = getPopupMenu(anchor);
        popup.show();
    }

    @NonNull
    private PopupMenu getPopupMenu(final View anchor) {
        if (speedPopup != null) {
            return speedPopup;
        }
        final ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
        // final ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.Widget_MaterialComponents_PopupMenu_Exoplayer);
        speedPopup = new PopupMenu(themeWrapper, anchor);
        speedPopup.getMenuInflater().inflate(R.menu.speed_menu, speedPopup.getMenu());
        speedPopup.setOnMenuItemClickListener(item -> {
            float nextSpeed;
            int textResId;
            int itemId = item.getItemId();
            if (itemId == R.id.pt_two_five_x) {
                nextSpeed = 0.25f;
                textResId = R.string.pt_two_five_x;
            } else if (itemId == R.id.pt_five_x) {
                nextSpeed = 0.5f;
                textResId = R.string.pt_five_x;
            } else if (itemId == R.id.pt_seven_five_x) {
                nextSpeed = 0.75f;
                textResId = R.string.pt_seven_five_x;
            } else if (itemId == R.id.one_x) {
                nextSpeed = 1f;
                textResId = R.string.one_x;
            } else if (itemId == R.id.one_pt_two_five_x) {
                nextSpeed = 1.25f;
                textResId = R.string.one_pt_two_five_x;
            } else if (itemId == R.id.one_pt_five_x) {
                nextSpeed = 1.5f;
                textResId = R.string.one_pt_five_x;
            } else if (itemId == R.id.two_x) {
                nextSpeed = 2f;
                textResId = R.string.two_x;
            } else {
                nextSpeed = 1;
                textResId = R.string.one_x;
            }
            player.setPlaybackParameters(new PlaybackParameters(nextSpeed));
            controlsBinding.speed.setText(textResId);
            return true;
        });
        return speedPopup;
    }

    private void updateMuteIcon(final float volume) {
        if (volume == 0) {
            controlsBinding.mute.setIconResource(R.drawable.ic_volume_off_24);
            return;
        }
        controlsBinding.mute.setIconResource(R.drawable.ic_volume_up_24);
    }

    private void updatePlayPauseDrawable(final boolean playWhenReady) {
        if (playWhenReady) {
            controlsBinding.playPause.setIconResource(R.drawable.ic_pause_24);
            return;
        }
        controlsBinding.playPause.setIconResource(R.drawable.ic_play_arrow_24);
    }

    @Override
    public void onPlayWhenReadyChanged(final boolean playWhenReady, final int reason) {
        if (videoPlayerCallback == null) return;
        if (playWhenReady) {
            videoPlayerCallback.onPlay();
            return;
        }
        videoPlayerCallback.onPause();
    }

    @Override
    public void onPlayerError(final ExoPlaybackException error) {
        Log.e(TAG, "onPlayerError", error);
    }

    public float toggleMute() {
        if (player == null) return 0;
        final float vol = player.getVolume() == 0f ? 1f : 0f;
        player.setVolume(vol);
        return vol;
    }

    public void togglePlayback() {
        if (player == null) return;
        final int playbackState = player.getPlaybackState();
        if (playbackState == STATE_IDLE || playbackState == STATE_ENDED) return;
        final boolean playWhenReady = player.getPlayWhenReady();
        player.setPlayWhenReady(!playWhenReady);
    }

    public void releasePlayer() {
        if (player == null) return;
        player.release();
        player = null;
    }

    public void pause() {
        if (player == null) return;
        player.pause();
    }

    public interface VideoPlayerCallback {
        void onThumbnailLoaded();

        void onThumbnailClick();

        void onPlayerViewLoaded();

        void onPlay();

        void onPause();
    }
}
