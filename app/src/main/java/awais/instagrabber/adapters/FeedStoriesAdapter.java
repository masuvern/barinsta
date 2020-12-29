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

import awais.instagrabber.adapters.viewholder.FeedStoryViewHolder;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class FeedStoriesAdapter extends ListAdapter<FeedStoryModel, FeedStoryViewHolder> {
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
        void onFeedStoryClick(FeedStoryModel model, int position);

        void onFeedStoryLongClick(FeedStoryModel model, int position);
    }
}
