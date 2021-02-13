package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;

public class Audio implements Serializable {
    private final String audioSrc;
    private final long duration;
    private final List<Float> waveformData;
    private final int waveformSamplingFrequencyHz;
    private final long audioSrcExpirationTimestampUs;

    public Audio(final String audioSrc,
                 final long duration,
                 final List<Float> waveformData,
                 final int waveformSamplingFrequencyHz,
                 final long audioSrcExpirationTimestampUs) {
        this.audioSrc = audioSrc;
        this.duration = duration;
        this.waveformData = waveformData;
        this.waveformSamplingFrequencyHz = waveformSamplingFrequencyHz;
        this.audioSrcExpirationTimestampUs = audioSrcExpirationTimestampUs;
    }

    public String getAudioSrc() {
        return audioSrc;
    }

    public long getDuration() {
        return duration;
    }

    public List<Float> getWaveformData() {
        return waveformData;
    }

    public int getWaveformSamplingFrequencyHz() {
        return waveformSamplingFrequencyHz;
    }

    public long getAudioSrcExpirationTimestampUs() {
        return audioSrcExpirationTimestampUs;
    }
}
