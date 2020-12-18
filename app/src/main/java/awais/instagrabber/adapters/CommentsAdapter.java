package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.adapters.viewholder.comments.ChildCommentViewHolder;
import awais.instagrabber.adapters.viewholder.comments.ParentCommentViewHolder;
import awais.instagrabber.databinding.ItemCommentBinding;
import awais.instagrabber.databinding.ItemCommentSmallBinding;
import awais.instagrabber.models.CommentModel;

public final class CommentsAdapter extends ListAdapter<CommentModel, RecyclerView.ViewHolder> {
    private static final int TYPE_PARENT = 1;
    private static final int TYPE_CHILD = 2;

    private final Map<Integer, Integer> positionTypeMap = new HashMap<>();

    // private final Filter filter = new Filter() {
    //     @NonNull
    //     @Override
    //     protected FilterResults performFiltering(final CharSequence filter) {
    //         final FilterResults results = new FilterResults();
    //         results.values = commentModels;
    //
    //         final int commentsLen = commentModels == null ? 0 : commentModels.size();
    //         if (commentModels != null && commentsLen > 0 && !TextUtils.isEmpty(filter)) {
    //             final String query = filter.toString().toLowerCase();
    //             final ArrayList<CommentModel> filterList = new ArrayList<>(commentsLen);
    //
    //             for (final CommentModel commentModel : commentModels) {
    //                 final String commentText = commentModel.getText().toString().toLowerCase();
    //
    //                 if (commentText.contains(query)) filterList.add(commentModel);
    //                 else {
    //                     final List<CommentModel> childCommentModels = commentModel.getChildCommentModels();
    //                     if (childCommentModels != null) {
    //                         for (final CommentModel childCommentModel : childCommentModels) {
    //                             final String childCommentText = childCommentModel.getText().toString().toLowerCase();
    //                             if (childCommentText.contains(query)) filterList.add(commentModel);
    //                         }
    //                     }
    //                 }
    //             }
    //             filterList.trimToSize();
    //             results.values = filterList.toArray(new CommentModel[0]);
    //         }
    //
    //         return results;
    //     }
    //
    //     @Override
    //     protected void publishResults(final CharSequence constraint, @NonNull final FilterResults results) {
    //         if (results.values instanceof List) {
    //             //noinspection unchecked
    //             filteredCommentModels = (List<CommentModel>) results.values;
    //             notifyDataSetChanged();
    //         }
    //     }
    // };

    private static final DiffUtil.ItemCallback<CommentModel> DIFF_CALLBACK = new DiffUtil.ItemCallback<CommentModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final CommentModel oldItem, @NonNull final CommentModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final CommentModel oldItem, @NonNull final CommentModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };
    private final CommentCallback commentCallback;
    private CommentModel selected, toChangeLike;
    private int selectedIndex, likedIndex;

    public CommentsAdapter(final CommentCallback commentCallback) {
        super(DIFF_CALLBACK);
        this.commentCallback = commentCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final Context context = parent.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        if (type == TYPE_PARENT) {
            final ItemCommentBinding binding = ItemCommentBinding.inflate(layoutInflater, parent, false);
            return new ParentCommentViewHolder(binding);
        }
        final ItemCommentSmallBinding binding = ItemCommentSmallBinding.inflate(layoutInflater, parent, false);
        return new ChildCommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        CommentModel commentModel = getItem(position);
        if (commentModel == null) return;
        final int type = getItemViewType(position);
        final boolean selected = this.selected != null && this.selected.getId().equals(commentModel.getId());
        final boolean toLike = this.toChangeLike != null && this.toChangeLike.getId().equals(commentModel.getId());
        if (toLike) commentModel = this.toChangeLike;
        if (type == TYPE_PARENT) {
            final ParentCommentViewHolder viewHolder = (ParentCommentViewHolder) holder;
            viewHolder.bind(commentModel, selected, commentCallback);
            return;
        }
        final ChildCommentViewHolder viewHolder = (ChildCommentViewHolder) holder;
        viewHolder.bind(commentModel, selected, commentCallback);
    }

    @Override
    public void submitList(@Nullable final List<CommentModel> list) {
        final List<CommentModel> flatList = flattenList(list);
        super.submitList(flatList);
    }

    @Override
    public void submitList(@Nullable final List<CommentModel> list, @Nullable final Runnable commitCallback) {
        final List<CommentModel> flatList = flattenList(list);
        super.submitList(flatList, commitCallback);
    }

    private List<CommentModel> flattenList(final List<CommentModel> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        final List<CommentModel> flatList = new ArrayList<>();
        int lastCommentIndex = -1;
        for (final CommentModel parent : list) {
            lastCommentIndex++;
            flatList.add(parent);
            positionTypeMap.put(lastCommentIndex, TYPE_PARENT);
            final List<CommentModel> children = parent.getChildCommentModels();
            if (children != null) {
                for (final CommentModel child : children) {
                    lastCommentIndex++;
                    flatList.add(child);
                    positionTypeMap.put(lastCommentIndex, TYPE_CHILD);
                }
            }
        }
        return flatList;
    }


    @Override
    public int getItemViewType(final int position) {
        final Integer type = positionTypeMap.get(position);
        if (type == null) {
            return TYPE_PARENT;
        }
        return type;
    }

    public void setSelected(final CommentModel commentModel) {
        this.selected = commentModel;
        selectedIndex = getCurrentList().indexOf(commentModel);
        notifyItemChanged(selectedIndex);
    }

    public void clearSelection() {
        this.selected = null;
        notifyItemChanged(selectedIndex);
    }

    public void setLiked(final CommentModel commentModel, final boolean liked) {
        likedIndex = getCurrentList().indexOf(commentModel);
        CommentModel newCommentModel = commentModel;
        newCommentModel.setLiked(liked);
        this.toChangeLike = newCommentModel;
        notifyItemChanged(likedIndex);
    }

    public CommentModel getSelected() {
        return selected;
    }

    public interface CommentCallback {
        void onClick(final CommentModel comment);

        void onHashtagClick(final String hashtag);

        void onMentionClick(final String mention);

        void onURLClick(final String url);

        void onEmailClick(final String emailAddress);
    }
}