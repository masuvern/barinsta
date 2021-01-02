package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

public final class DirectItemVideoCallEvent {
    private final String action;
    private final String encodedServerDataInfo;
    private final String description;
    private final boolean threadHasAudioOnlyCall;
    private final List<DirectItemActionLog.TextRange> textAttributes;

    public DirectItemVideoCallEvent(final String action,
                                    final String encodedServerDataInfo,
                                    final String description,
                                    final boolean threadHasAudioOnlyCall,
                                    final List<DirectItemActionLog.TextRange> textAttributes) {
        this.action = action;
        this.encodedServerDataInfo = encodedServerDataInfo;
        this.description = description;
        this.threadHasAudioOnlyCall = threadHasAudioOnlyCall;
        this.textAttributes = textAttributes;
    }

    public String getAction() {
        return action;
    }

    public String getEncodedServerDataInfo() {
        return encodedServerDataInfo;
    }

    public String getDescription() {
        return description;
    }

    public boolean isThreadHasAudioOnlyCall() {
        return threadHasAudioOnlyCall;
    }

    public List<DirectItemActionLog.TextRange> getTextAttributes() {
        return textAttributes;
    }
}
