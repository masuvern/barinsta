package awais.instagrabber.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import awais.instagrabber.adapters.viewholder.directmessages.DirectItemActionLogViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemAnimatedMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemDefaultViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemLikeViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemLinkViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemMediaShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemPlaceholderViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemProfileViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemRavenMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemReelShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemStoryShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemTextViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemVideoCallEventViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemVoiceMediaViewHolder;
import awais.instagrabber.databinding.LayoutDmActionLogBinding;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmHeaderBinding;
import awais.instagrabber.databinding.LayoutDmLikeBinding;
import awais.instagrabber.databinding.LayoutDmLinkBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.databinding.LayoutDmProfileBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.databinding.LayoutDmReelShareBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.databinding.LayoutDmVoiceMediaBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.DateUtils;

public final class DirectItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = DirectItemsAdapter.class.getSimpleName();

    private final ProfileModel currentUser;
    private DirectThread thread;
    private final AsyncListDiffer<DirectItemOrHeader> differ;

    private static final DiffUtil.ItemCallback<DirectItemOrHeader> diffCallback = new DiffUtil.ItemCallback<DirectItemOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull final DirectItemOrHeader oldItem, @NonNull final DirectItemOrHeader newItem) {
            final boolean bothHeaders = oldItem.isHeader() && newItem.isHeader();
            final boolean bothItems = !oldItem.isHeader() && !newItem.isHeader();
            boolean areSameType = bothHeaders || bothItems;
            if (!areSameType) return false;
            if (bothHeaders) {
                return oldItem.date.equals(newItem.date);
            }
            if (oldItem.item != null && newItem.item != null) {
                return oldItem.item.getClientContext().equals(newItem.item.getClientContext());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull final DirectItemOrHeader oldItem, @NonNull final DirectItemOrHeader newItem) {
            final boolean bothHeaders = oldItem.isHeader() && newItem.isHeader();
            final boolean bothItems = !oldItem.isHeader() && !newItem.isHeader();
            boolean areSameType = bothHeaders || bothItems;
            if (!areSameType) return false;
            if (bothHeaders) {
                return oldItem.date.equals(newItem.date);
            }
            return oldItem.item.getTimestamp() == newItem.item.getTimestamp()
                    && oldItem.item.isPending() == newItem.item.isPending(); // todo need to be more specific
        }
    };

    public DirectItemsAdapter(@NonNull final ProfileModel currentUser) {
        this.currentUser = currentUser;
        differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(diffCallback).build());
        // this.onClickListener = onClickListener;
        // this.mentionClickListener = mentionClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (type == -1) {
            // header
            return new HeaderViewHolder(LayoutDmHeaderBinding.inflate(layoutInflater, parent, false));
        }
        final LayoutDmBaseBinding baseBinding = LayoutDmBaseBinding.inflate(layoutInflater, parent, false);
        final DirectItemType directItemType = DirectItemType.valueOf(type);
        return getItemViewHolder(layoutInflater, baseBinding, directItemType);
    }

    private DirectItemViewHolder getItemViewHolder(final LayoutInflater layoutInflater,
                                                   final LayoutDmBaseBinding baseBinding,
                                                   final DirectItemType directItemType) {
        switch (directItemType) {
            case TEXT: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemTextViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case LIKE: {
                final LayoutDmLikeBinding binding = LayoutDmLikeBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemLikeViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case LINK: {
                final LayoutDmLinkBinding binding = LayoutDmLinkBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemLinkViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case ACTION_LOG: {
                final LayoutDmActionLogBinding binding = LayoutDmActionLogBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemActionLogViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case VIDEO_CALL_EVENT: {
                final LayoutDmActionLogBinding binding = LayoutDmActionLogBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemVideoCallEventViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case PLACEHOLDER: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemPlaceholderViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case ANIMATED_MEDIA: {
                final LayoutDmAnimatedMediaBinding binding = LayoutDmAnimatedMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemAnimatedMediaViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case VOICE_MEDIA: {
                final LayoutDmVoiceMediaBinding binding = LayoutDmVoiceMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemVoiceMediaViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case LOCATION:
            case PROFILE: {
                final LayoutDmProfileBinding binding = LayoutDmProfileBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemProfileViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case MEDIA: {
                final LayoutDmMediaBinding binding = LayoutDmMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemMediaViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case CLIP:
            case FELIX_SHARE:
            case MEDIA_SHARE: {
                final LayoutDmMediaShareBinding binding = LayoutDmMediaShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemMediaShareViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case STORY_SHARE: {
                final LayoutDmStoryShareBinding binding = LayoutDmStoryShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemStoryShareViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case REEL_SHARE: {
                final LayoutDmReelShareBinding binding = LayoutDmReelShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemReelShareViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            case RAVEN_MEDIA: {
                final LayoutDmRavenMediaBinding binding = LayoutDmRavenMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemRavenMediaViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
            default: {
                final LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemDefaultViewHolder(baseBinding, binding, currentUser, thread, null, null);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final DirectItemOrHeader itemOrHeader = getItem(position);
        if (itemOrHeader.isHeader()) {
            ((HeaderViewHolder) holder).bind(itemOrHeader.date);
            return;
        }
        if (thread == null) return;
        ((DirectItemViewHolder) holder).bind(itemOrHeader.item);
    }

    protected DirectItemOrHeader getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    @Override
    public int getItemViewType(final int position) {
        final DirectItemOrHeader itemOrHeader = getItem(position);
        if (itemOrHeader.isHeader()) {
            return -1;
        }
        return itemOrHeader.item.getItemType().getId();
    }

    @Override
    public long getItemId(final int position) {
        final DirectItemOrHeader itemOrHeader = getItem(position);
        if (itemOrHeader.isHeader()) {
            return itemOrHeader.date.hashCode();
        }
        return itemOrHeader.item.getClientContext().hashCode();
    }

    public void setThread(final DirectThread thread) {
        if (thread == null) return;
        this.thread = thread;
    }

    public void submitList(@Nullable final List<DirectItem> list) {
        if (list == null) {
            differ.submitList(null);
            return;
        }
        differ.submitList(sectionAndSort(list));
    }

    public void submitList(@Nullable final List<DirectItem> list, @Nullable final Runnable commitCallback) {
        if (list == null) {
            differ.submitList(null, commitCallback);
            return;
        }
        differ.submitList(sectionAndSort(list), commitCallback);
    }

    private List<DirectItemOrHeader> sectionAndSort(final List<DirectItem> list) {
        final List<DirectItemOrHeader> itemOrHeaders = new ArrayList<>();
        Date prevSectionDate = null;
        for (int i = 0; i < list.size(); i++) {
            final DirectItem item = list.get(i);
            if (item == null) continue;
            final DirectItemOrHeader prev = itemOrHeaders.isEmpty() ? null : itemOrHeaders.get(itemOrHeaders.size() - 1);
            if (prev != null && prev.item != null && DateUtils.isSameDay(prev.item.getDate(), item.getDate())) {
                // just add item
                final DirectItemOrHeader itemOrHeader = new DirectItemOrHeader();
                itemOrHeader.item = item;
                itemOrHeaders.add(itemOrHeader);
                if (i == list.size() - 1) {
                    // add header
                    final DirectItemOrHeader itemOrHeader2 = new DirectItemOrHeader();
                    itemOrHeader2.date = prevSectionDate;
                    itemOrHeaders.add(itemOrHeader2);
                }
                continue;
            }
            if (prevSectionDate != null) {
                // add header
                final DirectItemOrHeader itemOrHeader = new DirectItemOrHeader();
                itemOrHeader.date = prevSectionDate;
                itemOrHeaders.add(itemOrHeader);
            }
            // Add item
            final DirectItemOrHeader itemOrHeader = new DirectItemOrHeader();
            itemOrHeader.item = item;
            itemOrHeaders.add(itemOrHeader);
            prevSectionDate = DateUtils.dateAtZeroHours(item.getDate());
        }
        return itemOrHeaders;
    }

    public List<DirectItemOrHeader> getList() {
        return differ.getCurrentList();
    }

    @Override
    public void onViewRecycled(@NonNull final RecyclerView.ViewHolder holder) {
        if (holder instanceof DirectItemViewHolder) {
            ((DirectItemViewHolder) holder).cleanup();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull final RecyclerView.ViewHolder holder) {
        if (holder instanceof DirectItemViewHolder) {
            ((DirectItemViewHolder) holder).cleanup();
        }
    }

    public static class DirectItemOrHeader {
        Date date;
        DirectItem item;

        public boolean isHeader() {
            return date != null;
        }

        @NonNull
        @Override
        public String toString() {
            return "DirectItemOrHeader{" +
                    "date=" + date +
                    ", item=" + (item != null ? item.getItemType() : null) +
                    '}';
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final LayoutDmHeaderBinding binding;

        public HeaderViewHolder(@NonNull final LayoutDmHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Date date) {
            if (date == null) {
                binding.header.setText("");
                return;
            }
            binding.header.setText(DateFormat.getDateFormat(itemView.getContext()).format(date));
        }
    }
}