package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.customviews.masoudss_waveform.WaveformSeekBar;
import awais.instagrabber.interfaces.MentionClickListener;

public final class TextMessageViewHolder extends RecyclerView.ViewHolder {
    public final CardView rootCardView;
    public final TextView tvUsername;
    public final ImageView ivProfilePic;
    // text message
    public final RamboTextView tvMessage;
    // expired message icon
    public final View mediaExpiredIcon;
    // media message
    public final View mediaMessageContainer;
    public final ImageView ivMediaPreview, mediaTypeIcon;
    // profile messag
    public final View profileMessageContainer, isVerified, btnOpenProfile;
    public final TextView tvProfileUsername, tvProfileName;
    public final ImageView ivMessageProfilePic;
    // animated message
    public final ImageView ivAnimatedMessage;
    // link message
    public final View linkMessageContainer;
    public final ImageView ivLinkPreview;
    public final TextView tvLinkTitle, tvLinkSummary;
    // voice message
    public final View voiceMessageContainer, btnPlayVoice;
    public final WaveformSeekBar waveformSeekBar;
    public final TextView tvVoiceDuration;

    public TextMessageViewHolder(@NonNull final View itemView, final View.OnClickListener clickListener,
                                 final MentionClickListener mentionClickListener) {
        super(itemView);

        if (clickListener != null) itemView.setOnClickListener(clickListener);

        tvUsername = itemView.findViewById(R.id.tvUsername);
        ivProfilePic = itemView.findViewById(R.id.ivProfilePic);

        // text message
        tvMessage = itemView.findViewById(R.id.tvMessage);
        tvMessage.setCaptionIsExpandable(true);
        tvMessage.setCaptionIsExpanded(true);
        if (mentionClickListener != null) tvMessage.setMentionClickListener(mentionClickListener);

        // root view
        rootCardView = (CardView) tvMessage.getParent().getParent();

        // expired message icon
        mediaExpiredIcon = itemView.findViewById(R.id.mediaExpiredIcon);

        // media message
        ivMediaPreview = itemView.findViewById(R.id.ivMediaPreview);
        mediaMessageContainer = (View) ivMediaPreview.getParent();
        mediaTypeIcon = mediaMessageContainer.findViewById(R.id.typeIcon);

        // profile message
        btnOpenProfile = itemView.findViewById(R.id.btnInfo);
        ivMessageProfilePic = itemView.findViewById(R.id.profileInfo);
        profileMessageContainer = (View) ivMessageProfilePic.getParent();
        isVerified = profileMessageContainer.findViewById(R.id.isVerified);
        tvProfileName = profileMessageContainer.findViewById(R.id.tvFullName);
        tvProfileUsername = profileMessageContainer.findViewById(R.id.profileInfoText);

        // animated message
        ivAnimatedMessage = itemView.findViewById(R.id.ivAnimatedMessage);

        // link message
        ivLinkPreview = itemView.findViewById(R.id.ivLinkPreview);
        linkMessageContainer = (View) ivLinkPreview.getParent();
        tvLinkTitle = linkMessageContainer.findViewById(R.id.tvLinkTitle);
        tvLinkSummary = linkMessageContainer.findViewById(R.id.tvLinkSummary);

        // voice message
        waveformSeekBar = itemView.findViewById(R.id.waveformSeekBar);
        voiceMessageContainer = (View) waveformSeekBar.getParent();
        btnPlayVoice = voiceMessageContainer.findViewById(R.id.btnPlayVoice);
        tvVoiceDuration = voiceMessageContainer.findViewById(R.id.tvVoiceDuration);
    }
}