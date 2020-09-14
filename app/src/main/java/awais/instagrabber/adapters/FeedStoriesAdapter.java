package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.FeedStoryViewHolder;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.models.FeedStoryModel;

public final class FeedStoriesAdapter extends ListAdapter<FeedStoryModel, FeedStoryViewHolder> {
    private final OnFeedStoryClickListener listener;

    private static final DiffUtil.ItemCallback<FeedStoryModel> diffCallback = new DiffUtil.ItemCallback<FeedStoryModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final FeedStoryModel oldItem, @NonNull final FeedStoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final FeedStoryModel oldItem, @NonNull final FeedStoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId());
        }
    };

    public FeedStoriesAdapter(final OnFeedStoryClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedStoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemHighlightBinding binding = ItemHighlightBinding.inflate(layoutInflater, parent, false);
        return new FeedStoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final FeedStoryViewHolder holder, final int position) {
        final FeedStoryModel model = getItem(position);
        holder.bind(model, position, listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(FeedStoryModel model, int position);
    }
}
