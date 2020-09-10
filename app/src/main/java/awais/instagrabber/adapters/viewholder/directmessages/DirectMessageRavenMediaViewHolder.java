package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.RavenExpiringMediaType;
import awais.instagrabber.models.enums.RavenMediaViewType;
import awais.instagrabber.utils.TextUtils;

public class DirectMessageRavenMediaViewHolder extends DirectMessageItemViewHolder {

    private final LayoutDmRavenMediaBinding binding;

    public DirectMessageRavenMediaViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                             @NonNull final LayoutDmRavenMediaBinding binding,
                                             final View.OnClickListener onClickListener) {
        super(baseBinding, onClickListener);
        this.binding = binding;
        binding.tvMessage.setVisibility(View.GONE);
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItemModel directItemModel) {
        final Context context = itemView.getContext();
        final DirectItemModel.DirectItemRavenMediaModel ravenMediaModel = directItemModel.getRavenMediaModel();
        DirectItemModel.DirectItemMediaModel mediaModel = directItemModel.getMediaModel();
        final boolean isExpired = ravenMediaModel == null || (mediaModel = ravenMediaModel.getMedia()) == null ||
                TextUtils.isEmpty(mediaModel.getThumbUrl()) && mediaModel.getPk() < 1;

        DirectItemModel.RavenExpiringMediaActionSummaryModel mediaActionSummary = null;
        if (ravenMediaModel != null) {
            mediaActionSummary = ravenMediaModel.getExpiringMediaActionSummary();
        }
        binding.mediaExpiredIcon.setVisibility(isExpired ? View.VISIBLE : View.GONE);

        int textRes = R.string.dms_inbox_raven_media_unknown;
        if (isExpired) textRes = R.string.dms_inbox_raven_media_expired;

        if (!isExpired) {
            if (mediaActionSummary != null) {
                final RavenExpiringMediaType expiringMediaType = mediaActionSummary.getType();

                if (expiringMediaType == RavenExpiringMediaType.RAVEN_DELIVERED)
                    textRes = R.string.dms_inbox_raven_media_delivered;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SENT)
                    textRes = R.string.dms_inbox_raven_media_sent;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_OPENED)
                    textRes = R.string.dms_inbox_raven_media_opened;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_REPLAYED)
                    textRes = R.string.dms_inbox_raven_media_replayed;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SENDING)
                    textRes = R.string.dms_inbox_raven_media_sending;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_BLOCKED)
                    textRes = R.string.dms_inbox_raven_media_blocked;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SUGGESTED)
                    textRes = R.string.dms_inbox_raven_media_suggested;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_SCREENSHOT)
                    textRes = R.string.dms_inbox_raven_media_screenshot;
                else if (expiringMediaType == RavenExpiringMediaType.RAVEN_CANNOT_DELIVER)
                    textRes = R.string.dms_inbox_raven_media_cant_deliver;
            }

            final RavenMediaViewType ravenMediaViewType = ravenMediaModel.getViewType();
            if (ravenMediaViewType == RavenMediaViewType.PERMANENT || ravenMediaViewType == RavenMediaViewType.REPLAYABLE) {
                final MediaItemType mediaType = mediaModel.getMediaType();
                textRes = -1;
                binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ||
                        mediaType == MediaItemType.MEDIA_TYPE_SLIDER ? View.VISIBLE : View.GONE);
                getGlideRequestManager().load(mediaModel.getThumbUrl()).into(binding.ivMediaPreview);
            }
        }
        if (textRes != -1) {
            binding.tvMessage.setText(context.getText(textRes));
            binding.tvMessage.setVisibility(View.VISIBLE);
        }
    }
}
