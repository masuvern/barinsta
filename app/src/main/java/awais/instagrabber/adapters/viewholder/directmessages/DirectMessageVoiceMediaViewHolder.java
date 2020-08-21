package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmVoiceMediaBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.utils.Utils;

public class DirectMessageVoiceMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmVoiceMediaBinding binding;

    private DirectItemModel.DirectItemVoiceMediaModel prevVoiceModel;
    private ImageView prevPlayIcon;

    public DirectMessageVoiceMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                             @NonNull final LayoutDmVoiceMediaBinding binding,
                                             final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;

        // todo pause / resume
        // todo release prev audio, start new voice
        binding.btnPlayVoice.setOnClickListener(v -> {
            final Object tag = v.getTag();
            final ImageView playIcon = (ImageView) ((ViewGroup) v).getChildAt(0);
            final DirectItemModel.DirectItemVoiceMediaModel voiceMediaModel = (DirectItemModel.DirectItemVoiceMediaModel) tag;
            final boolean voicePlaying = voiceMediaModel.isPlaying();
            voiceMediaModel.setPlaying(!voicePlaying);

            if (voiceMediaModel == prevVoiceModel) {
                // todo pause / resume
            } else {
                // todo release prev audio, start new voice
                if (prevVoiceModel != null) prevVoiceModel.setPlaying(false);
                if (prevPlayIcon != null)
                    prevPlayIcon.setImageResource(android.R.drawable.ic_media_play);
            }

            if (voicePlaying) {
                playIcon.setImageResource(android.R.drawable.ic_media_play);
            } else {
                playIcon.setImageResource(android.R.drawable.ic_media_pause);
            }
            prevVoiceModel = voiceMediaModel;
            prevPlayIcon = playIcon;
        });
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final DirectItemModel.DirectItemVoiceMediaModel voiceMediaModel = directItemModel.getVoiceMediaModel();
        if (voiceMediaModel != null) {
            final int[] waveformData = voiceMediaModel.getWaveformData();
            if (waveformData != null) binding.waveformSeekBar.setSample(waveformData);

            final long durationMs = voiceMediaModel.getDurationMs();
            binding.tvVoiceDuration.setText(Utils.millisToString(durationMs));
            binding.waveformSeekBar.setProgress(voiceMediaModel.getProgress());
            binding.waveformSeekBar.setProgressChangeListener((waveformSeekBar, progress, fromUser) -> {
                // todo progress audio player
                voiceMediaModel.setProgress(progress);
                if (fromUser)
                    binding.tvVoiceDuration.setText(Utils.millisToString(durationMs * progress / 100));
            });
            binding.btnPlayVoice.setTag(voiceMediaModel);
        } else {
            binding.waveformSeekBar.setProgress(0);
        }
    }
}
