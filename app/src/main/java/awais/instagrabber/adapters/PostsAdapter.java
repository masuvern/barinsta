package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import awais.instagrabber.adapters.viewholder.PostViewHolder;
import awais.instagrabber.databinding.ItemPostBinding;
import awais.instagrabber.models.PostModel;

public final class PostsAdapter extends MultiSelectListAdapter<PostModel, PostViewHolder> {

    private static final DiffUtil.ItemCallback<PostModel> diffCallback = new DiffUtil.ItemCallback<PostModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final PostModel oldItem, @NonNull final PostModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final PostModel oldItem, @NonNull final PostModel newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }
    };

    public PostsAdapter(final OnItemClickListener<PostModel> clickListener,
                        final OnItemLongClickListener<PostModel> longClickListener) {
        super(diffCallback, clickListener, longClickListener);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemPostBinding binding = ItemPostBinding.inflate(layoutInflater, parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final PostViewHolder holder, final int position) {
        final PostModel postModel = getItem(position);
        holder.bind(postModel, position, internalOnItemClickListener, internalOnLongItemClickListener);
    }
}
