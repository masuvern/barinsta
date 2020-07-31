package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.NotificationViewHolder;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;

public final class NotificationsAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
    private final View.OnClickListener onClickListener;
    private final MentionClickListener mentionClickListener;
    private final NotificationModel[] notificationModels;
    private LayoutInflater layoutInflater;

    public NotificationsAdapter(final NotificationModel[] notificationModels, final View.OnClickListener onClickListener,
                                final MentionClickListener mentionClickListener) {
        this.notificationModels = notificationModels;
        this.onClickListener = onClickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final Context context = parent.getContext();
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(context);
        return new NotificationViewHolder(layoutInflater.inflate(R.layout.item_notification,
                parent, false), onClickListener, mentionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotificationViewHolder holder, final int position) {
        final NotificationModel notificationModel = notificationModels[position];
        if (notificationModel != null) {
            holder.setNotificationModel(notificationModel);

            int text = -1;
            CharSequence subtext = null;
            switch (notificationModel.getType()) {
                case LIKE:
                    text = R.string.liked_notif;
                    break;
                case COMMENT:
                    text = R.string.comment_notif;
                    subtext = notificationModel.getText();
                    break;
                case MENTION:
                    text = R.string.mention_notif;
                    subtext = notificationModel.getText();
                    break;
                case FOLLOW:
                    text = R.string.follow_notif;
                    break;
            }

            holder.setCommment(text);
            holder.setSubCommment(subtext);
            holder.setDate(notificationModel.getDateTime());

            holder.setUsername(notificationModel.getUsername());

            final RequestManager rm = Glide.with(layoutInflater.getContext())
                    .applyDefaultRequestOptions(new RequestOptions().skipMemoryCache(true));

            rm.load(notificationModel.getProfilePic()).into(holder.getProfilePicView());
            rm.load(notificationModel.getPreviewPic()).into(holder.getPreviewPicView());
        }
    }

    @Override
    public int getItemCount() {
        return notificationModels == null ? 0 : notificationModels.length;
    }
}