// package awais.instagrabber.adapters;
//
// import android.view.LayoutInflater;
// import android.view.View;
// import android.view.ViewGroup;
//
// import androidx.annotation.NonNull;
// import androidx.recyclerview.widget.RecyclerView;
//
// import awais.instagrabber.R;
// import awais.instagrabber.adapters.viewholder.PostMediaViewHolder;
// import awais.instagrabber.databinding.ItemChildPostBinding;
// import awais.instagrabber.models.BasePostModel;
// import awais.instagrabber.models.ViewerPostModel;
//
// public final class PostsMediaAdapter extends RecyclerView.Adapter<PostMediaViewHolder> {
//     private final View.OnClickListener clickListener;
//     private ViewerPostModel[] postModels;
//
//     public PostsMediaAdapter(final ViewerPostModel[] postModels, final View.OnClickListener clickListener) {
//         this.postModels = postModels;
//         this.clickListener = clickListener;
//     }
//
//     @NonNull
//     @Override
//     public PostMediaViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
//         final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
//         layoutInflater.inflate(R.layout.item_child_post, parent, false);
//         final ItemChildPostBinding binding = ItemChildPostBinding.inflate(layoutInflater, parent, false);
//         return new PostMediaViewHolder(binding);
//     }
//
//     @Override
//     public void onBindViewHolder(@NonNull final PostMediaViewHolder holder, final int position) {
//         final ViewerPostModel postModel = postModels[position];
//         holder.bind(postModel, position, clickListener);
//     }
//
//     public void setData(final ViewerPostModel[] postModels) {
//         this.postModels = postModels;
//         notifyDataSetChanged();
//     }
//
//     public ViewerPostModel getItemAt(final int position) {
//         return postModels == null ? null : postModels[position];
//     }
//
//     @Override
//     public int getItemCount() {
//         return postModels == null ? 0 : postModels.length;
//     }
//
//     public BasePostModel[] getPostModels() {
//         return postModels;
//     }
// }