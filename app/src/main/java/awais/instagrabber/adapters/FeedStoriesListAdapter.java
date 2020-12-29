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
import awais.instagrabber.utils.Constants;
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
        holder.bind(model, listener);
    }

    @Override
    public void submitList(@Nullable final List<FeedStoryModel> list, @Nullable final Runnable commitCallback) {
        if (list == null) {
            super.submitList(null, commitCallback);
            return;
        }
        super.submitList(sort(list), commitCallback);
    }

    @Override
    public void submitList(@Nullable final List<FeedStoryModel> list) {
        if (list == null) {
            super.submitList(null);
            return;
        }
        super.submitList(sort(list));
    }

    private List<FeedStoryModel> sort(final List<FeedStoryModel> list) {
        final List<FeedStoryModel> listCopy = new ArrayList<>(list);
        Collections.sort(listCopy, (o1, o2) -> {
            int result;
            switch (Utils.settingsHelper.getString(Constants.STORY_SORT)) {
                case "1":
                    result = o1.getTimestamp() > o2.getTimestamp() ? -1 : (o1.getTimestamp() == o2.getTimestamp() ? 0 : 1);
                    break;
                case "2":
                    result = o1.getTimestamp() > o2.getTimestamp() ? 1 : (o1.getTimestamp() == o2.getTimestamp() ? 0 : -1);
                    break;
                default:
                    result = 0;
            }
            return result;
        });
        return listCopy;
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(final FeedStoryModel model);

        void onProfileClick(final String username);
    }
}
