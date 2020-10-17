// package awais.instagrabber.adapters;
//
// import android.view.LayoutInflater;
// import android.view.View;
// import android.view.ViewGroup;
//
// import androidx.annotation.NonNull;
// import androidx.recyclerview.widget.DiffUtil;
// import androidx.recyclerview.widget.ListAdapter;
//
// import awais.instagrabber.adapters.viewholder.PostViewerViewHolder;
// import awais.instagrabber.databinding.ItemFullPostViewBinding;
// import awais.instagrabber.interfaces.MentionClickListener;
// import awais.instagrabber.models.ViewerPostModelWrapper;
//
// public class PostViewAdapter extends ListAdapter<ViewerPostModelWrapper, PostViewerViewHolder> {
//     private final OnPostViewChildViewClickListener clickListener;
//     private final OnPostCaptionLongClickListener longClickListener;
//     private final MentionClickListener mentionClickListener;
//
//     private static final DiffUtil.ItemCallback<ViewerPostModelWrapper> diffCallback = new DiffUtil.ItemCallback<ViewerPostModelWrapper>() {
//         @Override
//         public boolean areItemsTheSame(@NonNull final ViewerPostModelWrapper oldItem,
//                                        @NonNull final ViewerPostModelWrapper newItem) {
//             return oldItem.getPosition() == newItem.getPosition();
//         }
//
//         @Override
//         public boolean areContentsTheSame(@NonNull final ViewerPostModelWrapper oldItem,
//                                           @NonNull final ViewerPostModelWrapper newItem) {
//             return oldItem.getViewerPostModels().equals(newItem.getViewerPostModels());
//         }
//     };
//
//     public PostViewAdapter(final OnPostViewChildViewClickListener clickListener,
//                            final OnPostCaptionLongClickListener longClickListener,
//                            final MentionClickListener mentionClickListener) {
//         super(diffCallback);
//         this.clickListener = clickListener;
//         this.longClickListener = longClickListener;
//         this.mentionClickListener = mentionClickListener;
//     }
//
//     @Override
//     public void onViewDetachedFromWindow(@NonNull final PostViewerViewHolder holder) {
//         holder.stopPlayingVideo();
//     }
//
//     @NonNull
//     @Override
//     public PostViewerViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
//                                                    final int viewType) {
//         final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
//         final ItemFullPostViewBinding binding = ItemFullPostViewBinding
//                 .inflate(layoutInflater, parent, false);
//         return new PostViewerViewHolder(binding);
//     }
//
//     @Override
//     public void onBindViewHolder(@NonNull final PostViewerViewHolder holder, final int position) {
//         final ViewerPostModelWrapper item = getItem(position);
//         holder.bind(item, position, clickListener, longClickListener, mentionClickListener);
//     }
//
//     public interface OnPostViewChildViewClickListener {
//         void onClick(View v,
//                      ViewerPostModelWrapper viewerPostModelWrapper,
//                      int postPosition,
//                      int childPosition);
//     }
//
//     public interface OnPostCaptionLongClickListener {
//         void onLongClick(String text);
//     }
// }
