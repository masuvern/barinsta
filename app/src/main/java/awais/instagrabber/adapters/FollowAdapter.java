package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.FollowsViewHolder;
import awais.instagrabber.interfaces.OnGroupClickListener;
import awais.instagrabber.models.FollowModel;
import awais.instagrabber.utils.Utils;
import thoughtbot.expandableadapter.ExpandableGroup;
import thoughtbot.expandableadapter.ExpandableList;
import thoughtbot.expandableadapter.ExpandableListPosition;
import thoughtbot.expandableadapter.GroupViewHolder;

// thanks to ThoughtBot's ExpandableRecyclerViewAdapter
//   https://github.com/thoughtbot/expandable-recycler-view
public final class FollowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnGroupClickListener, Filterable {
    private final Filter filter = new Filter() {
        @Nullable
        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            if (expandableList.groups != null) {
                final boolean isFilterEmpty = Utils.isEmpty(filter);
                final String query = isFilterEmpty ? null : filter.toString().toLowerCase();

                for (int x = 0; x < expandableList.groups.size(); ++x) {
                    final ExpandableGroup expandableGroup = expandableList.groups.get(x);
                    final List<FollowModel> items = expandableGroup.getItems(false);
                    final int itemCount = expandableGroup.getItemCount(false);

                    for (int i = 0; i < itemCount; ++i) {
                        final FollowModel followModel = items.get(i);

                        if (isFilterEmpty) followModel.setShown(true);
                        else followModel.setShown(Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()));
                    }
                }
            }
            return null;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            notifyDataSetChanged();
        }
    };
    private final View.OnClickListener onClickListener;
    private final LayoutInflater layoutInflater;
    private final ExpandableList expandableList;
    private final boolean hasManyGroups;

    public FollowAdapter(final Context context, final View.OnClickListener onClickListener, @NonNull final ArrayList<ExpandableGroup> groups) {
        this.layoutInflater = LayoutInflater.from(context);
        this.expandableList = new ExpandableList(groups);
        this.onClickListener = onClickListener;
        this.hasManyGroups = groups.size() > 1;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final boolean isGroup = hasManyGroups && viewType == ExpandableListPosition.GROUP;

        final View view = layoutInflater.inflate(isGroup ? R.layout.header_follow : R.layout.item_follow, parent, false);

        return isGroup ? new GroupViewHolder(view, this) : new FollowsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final ExpandableListPosition listPos = expandableList.getUnflattenedPosition(position);
        final ExpandableGroup group = expandableList.getExpandableGroup(listPos);

        if (hasManyGroups && listPos.type == ExpandableListPosition.GROUP) {
            final GroupViewHolder gvh = (GroupViewHolder) holder;
            gvh.setTitle(group.getTitle());
            gvh.toggle(isGroupExpanded(group));

        } else {
            final FollowModel model = group.getItems(true).get(hasManyGroups ? listPos.childPos : position);

            final FollowsViewHolder followHolder = (FollowsViewHolder) holder;
            if (model != null) {
                followHolder.itemView.setTag(model);
                followHolder.itemView.setOnClickListener(onClickListener);

                followHolder.tvUsername.setText(model.getUsername());
                followHolder.tvFullName.setText(model.getFullName());

                Glide.with(layoutInflater.getContext()).load(model.getProfilePicUrl()).into(followHolder.profileImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return expandableList.getVisibleItemCount() - (hasManyGroups ? 0 : 1);
    }

    @Override
    public int getItemViewType(final int position) {
        return !hasManyGroups ? 0 : expandableList.getUnflattenedPosition(position).type;
    }

    @Override
    public void toggleGroup(final int flatPos) {
        final ExpandableListPosition listPosition = expandableList.getUnflattenedPosition(flatPos);

        final int groupPos = listPosition.groupPos;
        final int positionStart = expandableList.getFlattenedGroupIndex(listPosition) + 1;
        final int positionEnd = expandableList.groups.get(groupPos).getItemCount(true);

        final boolean isExpanded = expandableList.expandedGroupIndexes[groupPos];
        expandableList.expandedGroupIndexes[groupPos] = !isExpanded;
        notifyItemChanged(positionStart - 1);
        if (positionEnd > 0) {
            if (isExpanded) notifyItemRangeRemoved(positionStart, positionEnd);
            else notifyItemRangeInserted(positionStart, positionEnd);
        }
    }

    public boolean isGroupExpanded(final ExpandableGroup group) {
        return expandableList.expandedGroupIndexes[expandableList.groups.indexOf(group)];
    }
}