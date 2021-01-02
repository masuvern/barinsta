package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.adapters.viewholder.StoryListViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.utils.Utils;

public final class FeedStoriesListAdapter extends ListAdapter<FeedStoryModel, StoryListViewHolder> {
    private final OnFeedStoryClickListener listener;

    private static final DiffUtil.ItemCallback<FeedStoryModel> diffCallback = new DiffUtil.ItemCallback<FeedStoryModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final FeedStoryModel oldItem, @NonNull final FeedStoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final FeedStoryModel oldItem, @NonNull final FeedStoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId()) && oldItem.isFullyRead().equals(newItem.isFullyRead());
        }
    };

    public FeedStoriesListAdapter(final OnFeedStoryClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryListViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemNotificationBinding binding = ItemNotificationBinding.inflate(layoutInflater, parent, false);
        return new StoryListViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final StoryListViewHolder holder, final int position) {
        final FeedStoryModel model = getItem(position);
        holder.bind(model, position, listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(final FeedStoryModel model, final int position);

        void onProfileClick(final String username);
    }
}
