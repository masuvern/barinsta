package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.feed.FeedItemViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedPhotoViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedSliderViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedVideoViewHolder;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.databinding.ItemFeedPhotoBinding;
import awais.instagrabber.databinding.ItemFeedSliderBinding;
import awais.instagrabber.databinding.ItemFeedVideoBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Utils;

public final class FeedAdapter extends ListAdapter<FeedModel, FeedItemViewHolder> {
    private static final String TAG = "FeedAdapter";
    private final View.OnClickListener clickListener;
    private final MentionClickListener mentionClickListener;
    private final View.OnLongClickListener longClickListener = v -> {
        final Object tag;
        if (v instanceof RamboTextView && (tag = v.getTag()) instanceof FeedModel)
            Utils.copyText(v.getContext(), ((FeedModel) tag).getPostCaption());
        return true;
    };

    private static final DiffUtil.ItemCallback<FeedModel> diffCallback = new DiffUtil.ItemCallback<FeedModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final FeedModel oldItem, @NonNull final FeedModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final FeedModel oldItem, @NonNull final FeedModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }
    };

    public FeedAdapter(final View.OnClickListener clickListener,
                       final MentionClickListener mentionClickListener) {
        super(diffCallback);
        // private final static String ellipsize = "… more";
        this.clickListener = clickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public FeedItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final Context context = parent.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        final MediaItemType type = MediaItemType.valueOf(viewType);
        switch (type) {
            case MEDIA_TYPE_VIDEO: {
                final ItemFeedVideoBinding binding = ItemFeedVideoBinding.inflate(layoutInflater, parent, false);
                return new FeedVideoViewHolder(binding, mentionClickListener, clickListener, longClickListener);
            }
            case MEDIA_TYPE_SLIDER: {
                final ItemFeedSliderBinding binding = ItemFeedSliderBinding.inflate(layoutInflater, parent, false);
                return new FeedSliderViewHolder(binding, mentionClickListener, clickListener, longClickListener);
            }
            default:
            case MEDIA_TYPE_IMAGE: {
                final ItemFeedPhotoBinding binding = ItemFeedPhotoBinding.inflate(layoutInflater, parent, false);
                return new FeedPhotoViewHolder(binding, mentionClickListener, clickListener, longClickListener);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final FeedItemViewHolder viewHolder, final int position) {
        final FeedModel feedModel = getItem(position);
        if (feedModel == null) {
            return;
        }
        feedModel.setPosition(position);
        viewHolder.bind(feedModel);
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position).getItemType().getId();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull final FeedItemViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Log.d(TAG, "attached holder: " + holder);
        if (!(holder instanceof FeedSliderViewHolder)) return;
        final FeedSliderViewHolder feedSliderViewHolder = (FeedSliderViewHolder) holder;
        feedSliderViewHolder.startPlayingVideo();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull final FeedItemViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        // Log.d(TAG, "detached holder: " + holder);
        if (!(holder instanceof FeedSliderViewHolder)) return;
        final FeedSliderViewHolder feedSliderViewHolder = (FeedSliderViewHolder) holder;
        feedSliderViewHolder.stopPlayingVideo();
    }
}