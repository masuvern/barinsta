package awais.instagrabber.adapters.viewholder.directmessages;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.common.primitives.Floats;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmVoiceMediaBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.repositories.responses.directmessages.Audio;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectItemVoiceMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static com.google.android.exoplayer2.C.TIME_UNSET;

public class DirectItemVoiceMediaViewHolder extends DirectItemViewHolder {
    private static final String TAG = "DirectItemVoiceMediaVH";

    private final LayoutDmVoiceMediaBinding binding;
    private final DefaultDataSourceFactory dataSourceFactory;
    private SimpleExoPlayer player;
    private Handler handler;
    private Runnable positionChecker;
    private Player.EventListener listener;

    public DirectItemVoiceMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                          @NonNull final LayoutDmVoiceMediaBinding binding,
                                          final ProfileModel currentUser,
                                          final DirectThread thread,
                                          final MentionClickListener mentionClickListener,
                                          final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        this.dataSourceFactory = new DefaultDataSourceFactory(binding.getRoot().getContext(), "instagram");
        setItemView(binding.getRoot());
        final int margin = itemView.getResources().getDimensionPixelSize(R.dimen.dm_message_item_margin);
        binding.waveformSeekBar.getLayoutParams().width = Utils.displayMetrics.widthPixels - margin - Utils.convertDpToPx(56);
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        removeBg();
        final DirectItemVoiceMedia voiceMedia = directItemModel.getVoiceMedia();
        if (voiceMedia == null) return;
        final DirectItemMedia media = voiceMedia.getMedia();
        if (media == null) return;
        final Audio audio = media.getAudio();
        if (audio == null) return;
        final List<Float> waveformData = audio.getWaveformData();
        binding.waveformSeekBar.setSample(Floats.toArray(waveformData));
        binding.waveformSeekBar.setEnabled(false);
        final String text = String.format("%s/%s", TextUtils.millisToTimeString(0), TextUtils.millisToTimeString(audio.getDuration()));
        binding.duration.setText(text);
        final AudioItemState audioItemState = new AudioItemState();
        player = new SimpleExoPlayer.Builder(itemView.getContext()).build();
        player.setVolume(1);
        player.setPlayWhenReady(true);
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        handler = new Handler();
        final long initialDelay = 0;
        final long recurringDelay = 60;
        positionChecker = new Runnable() {
            @Override
            public void run() {
                if (handler != null) {
                    handler.removeCallbacks(this);
                }
                if (player == null) return;
                final long currentPosition = player.getCurrentPosition();
                final long duration = player.getDuration();
                // Log.d(TAG, "currentPosition: " + currentPosition + ", duration: " + duration);
                if (duration == TIME_UNSET) return;
                // final float progress = ((float) currentPosition / duration /* * 100 */);
                final int progress = (int) ((float) currentPosition / duration * 100);
                // Log.d(TAG, "progress: " + progress);
                final String text = String.format("%s/%s", TextUtils.millisToTimeString(currentPosition), TextUtils.millisToTimeString(duration));
                binding.duration.setText(text);
                binding.waveformSeekBar.setProgress(progress);
                if (handler != null) {
                    handler.postDelayed(this, recurringDelay);
                }
            }
        };
        player.addListener(listener = new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(final int state) {
                if (!audioItemState.isPrepared() && state == Player.STATE_READY) {
                    binding.playPause.setIconResource(R.drawable.ic_round_pause_24);
                    audioItemState.setPrepared(true);
                    binding.playPause.setVisibility(View.VISIBLE);
                    binding.progressBar.setVisibility(View.GONE);
                    if (handler != null) {
                        handler.postDelayed(positionChecker, initialDelay);
                    }
                    return;
                }
                if (state == Player.STATE_ENDED) {
                    // binding.waveformSeekBar.setProgressInPercentage(0);
                    binding.waveformSeekBar.setProgress(0);
                    binding.playPause.setIconResource(R.drawable.ic_round_play_arrow_24);
                    if (handler != null) {
                        handler.removeCallbacks(positionChecker);
                    }
                }
            }

            @Override
            public void onPlayerError(final ExoPlaybackException error) {
                Log.e(TAG, "onPlayerError: ", error);
            }
        });
        final ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
        final MediaItem mediaItem = MediaItem.fromUri(audio.getAudioSrc());
        final ProgressiveMediaSource mediaSource = sourceFactory.createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        binding.playPause.setOnClickListener(v -> {
            if (player == null) return;
            if (!audioItemState.isPrepared()) {
                player.prepare();
                binding.playPause.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);
                return;
            }
            if (player.isPlaying()) {
                binding.playPause.setIconResource(R.drawable.ic_round_play_arrow_24);
                player.pause();
                return;
            }
            binding.playPause.setIconResource(R.drawable.ic_round_pause_24);
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
                if (handler != null) {
                    handler.postDelayed(positionChecker, initialDelay);
                }
            }
            binding.waveformSeekBar.setEnabled(true);
            player.play();
        });
    }

    @Override
    public void cleanup() {
        if (handler != null && positionChecker != null) {
            handler.removeCallbacks(positionChecker);
            handler = null;
            positionChecker = null;
        }
        if (player != null) {
            player.release();
            if (listener != null) {
                player.removeListener(listener);
            }
            player = null;
        }
    }

    private static class AudioItemState {
        private boolean prepared;

        private AudioItemState() {}

        public boolean isPrepared() {
            return prepared;
        }

        public void setPrepared(final boolean prepared) {
            this.prepared = prepared;
        }
    }
}
