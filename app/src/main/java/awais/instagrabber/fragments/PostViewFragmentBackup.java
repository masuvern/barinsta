// package awais.instagrabber.fragments;
//
// import android.content.Context;
// import android.content.Intent;
// import android.graphics.drawable.Animatable;
// import android.net.Uri;
// import android.os.AsyncTask;
// import android.os.Bundle;
// import android.os.Handler;
// import android.text.SpannableString;
// import android.view.LayoutInflater;
// import android.view.View;
// import android.view.ViewGroup;
// import android.widget.LinearLayout;
// import android.widget.RelativeLayout;
// import android.widget.TextView;
// import android.widget.Toast;
//
// import androidx.annotation.NonNull;
// import androidx.annotation.Nullable;
// import androidx.appcompat.app.AppCompatActivity;
// import androidx.core.view.GestureDetectorCompat;
// import androidx.fragment.app.Fragment;
// import androidx.lifecycle.ViewModelProvider;
// import androidx.recyclerview.widget.LinearLayoutManager;
// import androidx.recyclerview.widget.RecyclerView;
//
// import com.facebook.drawee.backends.pipeline.Fresco;
// import com.facebook.drawee.controller.BaseControllerListener;
// import com.facebook.imagepipeline.image.ImageInfo;
// import com.facebook.imagepipeline.request.ImageRequest;
// import com.facebook.imagepipeline.request.ImageRequestBuilder;
// import com.google.android.exoplayer2.SimpleExoPlayer;
// import com.google.android.exoplayer2.source.MediaSource;
// import com.google.android.exoplayer2.source.MediaSourceEventListener;
// import com.google.android.exoplayer2.source.ProgressiveMediaSource;
// import com.google.android.exoplayer2.upstream.DataSource;
// import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
// import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
// import com.google.android.exoplayer2.upstream.cache.SimpleCache;
//
// import java.io.IOException;
// import java.util.List;
//
// import awais.instagrabber.R;
// import awais.instagrabber.adapters.PostsMediaAdapter;
// import awais.instagrabber.asyncs.PostFetcher;
// import awais.instagrabber.asyncs.ProfileFetcher;
// import awais.instagrabber.asyncs.i.iPostFetcher;
// import awais.instagrabber.customviews.CommentMentionClickSpan;
// import awais.instagrabber.customviews.helpers.SwipeGestureListener;
// import awais.instagrabber.databinding.FragmentPostViewBinding;
// import awais.instagrabber.fragments.main.viewmodels.BasePostViewModel;
// import awais.instagrabber.fragments.main.viewmodels.DiscoverItemViewModel;
// import awais.instagrabber.fragments.main.viewmodels.FeedViewModel;
// import awais.instagrabber.interfaces.FetchListener;
// import awais.instagrabber.interfaces.SwipeEvent;
// import awais.instagrabber.models.BasePostModel;
// import awais.instagrabber.models.PostModel;
// import awais.instagrabber.models.ProfileModel;
// import awais.instagrabber.models.ViewerPostModel;
// import awais.instagrabber.models.enums.MediaItemType;
// import awais.instagrabber.models.enums.PostItemType;
// import awais.instagrabber.utils.Constants;
// import awais.instagrabber.utils.Utils;
//
// import static awais.instagrabber.utils.Utils.settingsHelper;
//
// public class PostViewFragmentBackup extends Fragment {
//
//     private FragmentPostViewBinding binding;
//     private LinearLayout root;
//     private AppCompatActivity fragmentActivity;
//     private PostItemType postType;
//     private int postIndex;
//     private String postId;
//     private SimpleExoPlayer player;
//     private String postShortCode;
//     private BasePostViewModel<?> postViewModel;
//     private ViewerPostModel currentPost;
//     private List<? extends BasePostModel> postList;
//     private LinearLayout.LayoutParams containerLayoutParams;
//     private BasePostModel basePostModel;
//     private String prevUsername;
//     private ProfileModel profileModel;
//     private String postUserId;
//     private CharSequence postCaption;
//
//     private final View.OnClickListener onClickListener = new View.OnClickListener() {
//         @Override
//         public void onClick(final View v) {
//             // if (v == binding.topPanel.ivProfilePic) {
//             // new AlertDialog.Builder(requireContext()).setAdapter(profileDialogAdapter, profileDialogListener)
//             //.setNeutralButton(R.string.cancel, null).setTitle(viewerPostModel.getUsername()).show();
//             // return;
//             // }
//             if (v == binding.ivToggleFullScreen) {
//                 // toggleFullscreen();
//
//                 final LinearLayout topPanelRoot = binding.topPanel.getRoot();
//                 final int iconRes;
//
//                 if (containerLayoutParams.weight != 3.3f) {
//                     containerLayoutParams.weight = 3.3f;
//                     iconRes = R.drawable.ic_fullscreen_exit;
//                     topPanelRoot.setVisibility(View.GONE);
//                     binding.btnDownload.setVisibility(View.VISIBLE);
//                     binding.bottomPanel.tvPostDate.setVisibility(View.GONE);
//                 } else {
//                     containerLayoutParams.weight = (binding.mediaList.getVisibility() == View.VISIBLE) ? 1.35f : 1.9f;
//                     containerLayoutParams.weight += (Utils.isEmpty(settingsHelper.getString(Constants.COOKIE))) ? 0.3f : 0;
//                     iconRes = R.drawable.ic_fullscreen;
//                     topPanelRoot.setVisibility(View.VISIBLE);
//                     binding.btnDownload.setVisibility(View.GONE);
//                     binding.bottomPanel.tvPostDate.setVisibility(View.VISIBLE);
//                 }
//
//                 binding.ivToggleFullScreen.setImageResource(iconRes);
//                 binding.container.setLayoutParams(containerLayoutParams);
//
//             } else if (v == binding.bottomPanel.btnMute) {
//                 if (player != null) {
//                     final float intVol = player.getVolume() == 0f ? 1f : 0f;
//                     player.setVolume(intVol);
//                     binding.bottomPanel.btnMute.setImageResource(intVol == 0f ? R.drawable.ic_volume_off_24 : R.drawable.ic_volume_up_24);
//                     Utils.sessionVolumeFull = intVol == 1f;
//                 }
//             } else if (v == binding.btnLike) {
//                 // new PostViewer.PostAction().execute("likes");
//             } else if (v == binding.btnBookmark) {
//                 // new PostViewer.PostAction().execute("save");
//             } else {
//                 // final Object tag = v.getTag();
//                 // if (tag instanceof ViewerPostModel) {
//                 //     viewerPostModel = (ViewerPostModel) tag;
//                 //     slidePos = Math.max(0, viewerPostModel.getPosition());
//                 //     refreshPost();
//                 // }
//             }
//         }
//     };
//     private final PostsMediaAdapter mediaAdapter = new PostsMediaAdapter(null, onClickListener);
//     private final FetchListener<ViewerPostModel[]> pfl = result -> {
//         final Context context = getContext();
//         if (result == null || result.length < 1 && context != null) {
//             Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
//             return;
//         }
//         currentPost = result[0];
//         mediaAdapter.setData(result);
//         if (result.length > 1) {
//             binding.mediaList.setLayoutParams(new LinearLayout.LayoutParams(
//                     LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.55f
//             ));
//             containerLayoutParams.weight = 1.35f;
//             containerLayoutParams.weight += (Utils.isEmpty(settingsHelper.getString(Constants.COOKIE))) ? 0.3f : 0;
//             binding.container.setLayoutParams(containerLayoutParams);
//             binding.mediaList.setVisibility(View.VISIBLE);
//         }
//
//         // viewerCaptionParent.setOnTouchListener(gestureTouchListener);
//         // binding.playerView.setOnTouchListener(gestureTouchListener);
//         // binding.imageViewer.setOnSingleFlingListener((e1, e2, velocityX, velocityY) -> {
//         //     final float diffX = e2.getX() - e1.getX();
//         //     if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && Math.abs(diffX) > SwipeGestureListener.SWIPE_THRESHOLD
//         //             && Math.abs(velocityX) > SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD) {
//         //         swipeEvent.onSwipe(diffX > 0);
//         //         return true;
//         //     }
//         //     return false;
//         // });
//
//         final long commentsCount = currentPost.getCommentsCount();
//         binding.bottomPanel.commentsCount.setText(String.valueOf(commentsCount));
//         binding.bottomPanel.btnComments.setVisibility(View.VISIBLE);
//
//         // binding.bottomPanel.btnComments.setOnClickListener(v -> startActivityForResult(
//         // new Intent(requireContext(), CommentsViewer.class)
//         //         .putExtra(Constants.EXTRAS_SHORTCODE, postModel.getShortCode())
//         //         .putExtra(Constants.EXTRAS_POST, currentPost.getPostId())
//         //         .putExtra(Constants.EXTRAS_USER, postUserId), 6969)
//         binding.bottomPanel.btnComments.setClickable(true);
//         binding.bottomPanel.btnComments.setEnabled(true);
//
//         if (basePostModel instanceof PostModel) {
//             final PostModel postModel = (PostModel) basePostModel;
//             postModel.setPostId(currentPost.getPostId());
//             postModel.setTimestamp(currentPost.getTimestamp());
//             postModel.setPostCaption(currentPost.getPostCaption());
//             // if (!ok) {
//             //     liked = currentPost.getLike();
//             //     saved = currentPost.getBookmark();
//             // }
//         }
//         showCurrentPost();
//         // refreshPost();
//     };
//     private int lastSlidePos;
//     private int slidePos;
//     private String url;
//     private SwipeEvent swipeEvent;
//     private GestureDetectorCompat gestureDetector;
//
//     @Override
//     public void onCreate(@Nullable final Bundle savedInstanceState) {
//         super.onCreate(savedInstanceState);
//         fragmentActivity = (AppCompatActivity) requireActivity();
//         if (fragmentActivity.getSupportActionBar() != null) {
//             fragmentActivity.getSupportActionBar().setTitle("");
//         }
//         setHasOptionsMenu(true);
//     }
//
//     @Nullable
//     @Override
//     public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
//         if (root != null) {
//             return root;
//         }
//         binding = FragmentPostViewBinding.inflate(inflater, container, false);
//         root = binding.getRoot();
//         binding.mediaList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
//         binding.mediaList.setAdapter(mediaAdapter);
//         binding.mediaList.setVisibility(View.GONE);
//         return root;
//     }
//
//     @Override
//     public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
//         containerLayoutParams = (LinearLayout.LayoutParams) binding.container.getLayoutParams();
//         init();
//     }
//
//     @Override
//     public void onDestroy() {
//         super.onDestroy();
//     }
//
//     private void init() {
//         final Bundle arguments = getArguments();
//         if (arguments == null) return;
//         final PostViewFragmentArgs fragmentArgs = PostViewFragmentArgs.fromBundle(arguments);
//         postType = fragmentArgs.getPostType() == null ? null : PostItemType.valueOf(fragmentArgs.getPostType());
//         postIndex = fragmentArgs.getPostIndex();
//         postId = fragmentArgs.getPostId();
//         postShortCode = fragmentArgs.getPostShortCode();
//         setupSwipe();
//         if (postType != null && postIndex >= 0) {
//             basePostModel = getBasePostFromViewModel();
//             if (basePostModel != null) {
//                 if (basePostModel.getShortCode() != null) {
//                     fetchPostFromShortCode(basePostModel.getShortCode());
//                     return;
//                 }
//                 fetchPostFromPostId(basePostModel.getPostId());
//                 return;
//             }
//         }
//         // getStartPost();
//         // showCurrentPost();
//         // setupTop();
//         // setupPost();
//         // setupBottom();
//     }
//
//     private void setupSwipe() {
//         swipeEvent = isRight -> {
//             // final List<? extends BasePostModel> itemGetterItems;
//             // final boolean isMainSwipe;
//             //
//             // if (postType == PostItemType.SAVED && SavedViewer.itemGetter != null) {
//             //     itemGetterItems = SavedViewer.itemGetter.get(postType);
//             //     isMainSwipe = !(itemGetterItems.size() < 1 || postType == PostItemType.SAVED && isFromShare);
//             // } else if (postType != null && MainActivityBackup.itemGetter != null) {
//             //     itemGetterItems = MainActivityBackup.itemGetter.get(postType);
//             //     isMainSwipe = !(itemGetterItems.size() < 1 || postType == PostItemType.MAIN && isFromShare);
//             // } else {
//             //     itemGetterItems = null;
//             //     isMainSwipe = false;
//             // }
//             //
//             // final BasePostModel[] basePostModels = mediaAdapter.getPostModels();
//             // final int slides = basePostModels.length;
//             //
//             // int position = basePostModel.getPosition();
//             //
//             // if (isRight) {
//             //     --slidePos;
//             //     if (!isMainSwipe && slidePos < 0) slidePos = 0;
//             //     if (slides > 0 && slidePos >= 0) {
//             //         if (basePostModels[slidePos] instanceof ViewerPostModel) {
//             //             currentPost = (ViewerPostModel) basePostModels[slidePos];
//             //         }
//             //         showCurrentPost();
//             //         return;
//             //     }
//             //     if (isMainSwipe && --position < 0) position = itemGetterItems.size() - 1;
//             // } else {
//             //     ++slidePos;
//             //     if (!isMainSwipe && slidePos >= slides) slidePos = slides - 1;
//             //     if (slides > 0 && slidePos < slides) {
//             //         if (basePostModels[slidePos] instanceof ViewerPostModel) {
//             //             currentPost = (ViewerPostModel) basePostModels[slidePos];
//             //         }
//             //         showCurrentPost();
//             //         return;
//             //     }
//             //     if (isMainSwipe && ++position >= itemGetterItems.size()) position = 0;
//             // }
//             //
//             // if (isMainSwipe) {
//             //     slidePos = 0;
//             //     ok = false;
//             //     Log.d("AWAISKING_APP", "swipe left <<< post[" + position + "]: " + basePostModel + " -- " + slides);
//             //     basePostModel = itemGetterItems.get(position);
//             //     basePostModel.setPosition(position);
//             //     showCurrentPost();
//             // }
//         };
//         gestureDetector = new GestureDetectorCompat(requireContext(), new SwipeGestureListener(swipeEvent));
//     }
//
//     private void fetchPostFromPostId(final String postId) {
//         new iPostFetcher(postId, pfl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//     }
//
//     private void fetchPostFromShortCode(final String shortCode) {
//         new PostFetcher(shortCode, pfl)
//                 .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//     }
//
//     private BasePostModel getBasePostFromViewModel() {
//         switch (postType) {
//             case FEED:
//                 postViewModel = new ViewModelProvider(fragmentActivity).get(FeedViewModel.class);
//                 break;
//             case MAIN:
//                 // ???
//                 break;
//             case SAVED:
//                 // ???
//                 break;
//             case DISCOVER:
//                 postViewModel = new ViewModelProvider(fragmentActivity).get(DiscoverItemViewModel.class);
//         }
//         if (postViewModel == null) return null;
//         postList = postViewModel.getList().getValue();
//         if (postList == null) return null;
//         return postList.get(postIndex);
//     }
//
//     private void showCurrentPost() {
//         setupPostInfoBar("@" + currentPost.getUsername(),
//                 currentPost.getItemType(),
//                 currentPost.getLocationName(),
//                 currentPost.getLocation());
//         // postCaption = basePostModel.getPostCaption();
//         ((View) binding.bottomPanel.viewerCaption.getParent()).setVisibility(View.VISIBLE);
//         // binding.bottomPanel.btnDownload.setOnClickListener(downloadClickListener);
//         if (containerLayoutParams.weight != 3.3f) {
//             containerLayoutParams.weight = (binding.mediaList.getVisibility() == View.VISIBLE) ? 1.35f : 1.9f;
//             binding.container.setLayoutParams(containerLayoutParams);
//         }
//         if (binding.mediaList.getVisibility() == View.VISIBLE) {
//             ViewerPostModel item = mediaAdapter.getItemAt(lastSlidePos);
//             if (item != null) {
//                 item.setCurrentSlide(false);
//                 mediaAdapter.notifyItemChanged(lastSlidePos, item);
//             }
//
//             item = mediaAdapter.getItemAt(slidePos);
//             if (item != null) {
//                 item.setCurrentSlide(true);
//                 mediaAdapter.notifyItemChanged(slidePos, item);
//             }
//         }
//         lastSlidePos = slidePos;
//         postCaption = currentPost.getPostCaption();
//
//         if (Utils.hasMentions(postCaption)) {
//             binding.bottomPanel.viewerCaption.setText(Utils.getMentionText(postCaption), TextView.BufferType.SPANNABLE);
//             // binding.bottomPanel.viewerCaption.setMentionClickListener((view, text, isHashtag, isLocation) -> searchUsername(text));
//         } else {
//             binding.bottomPanel.viewerCaption.setMentionClickListener(null);
//             binding.bottomPanel.viewerCaption.setText(postCaption);
//         }
//
//         // setupPostInfoBar("@" + viewerPostModel.getUsername(), viewerPostModel.getItemType(),
//         //         viewerPostModel.getLocationName(), viewerPostModel.getLocation());
//
//         // if (postModel instanceof PostModel) {
//         //     final PostModel postModel = (PostModel) this.postModel;
//         //     postModel.setPostId(viewerPostModel.getPostId());
//         //     postModel.setTimestamp(viewerPostModel.getTimestamp());
//         //     postModel.setPostCaption(viewerPostModel.getPostCaption());
//         //     if (liked == true) {
//         //         binding.btnLike.setText(resources.getString(R.string.unlike, viewerPostModel.getLikes()
//         //                 + ((ok && viewerPostModel.getLike() != liked) ? (liked ? 1L : -1L) : 0L)));
//         //         binding.btnLike.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//         //                 getApplicationContext(), R.color.btn_pink_background)));
//         //     } else {
//         //         binding.btnLike.setText(resources.getString(R.string.like, viewerPostModel.getLikes()
//         //                 + ((ok && viewerPostModel.getLike() != liked) ? (liked ? 1L : -1L) : 0L)));
//         //         binding.btnLike.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//         //                 getApplicationContext(), R.color.btn_lightpink_background)));
//         //     }
//         //     if (saved == true) {
//         //         binding.btnBookmark.setText(R.string.unbookmark);
//         //         binding.btnBookmark.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//         //                 getApplicationContext(), R.color.btn_orange_background)));
//         //     } else {
//         //         binding.btnBookmark.setText(R.string.bookmark);
//         //         binding.btnBookmark.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//         //                 getApplicationContext(), R.color.btn_lightorange_background)));
//         //     }
//         // }
//
//         binding.bottomPanel.tvPostDate.setText(currentPost.getPostDate());
//         binding.bottomPanel.tvPostDate.setVisibility(containerLayoutParams.weight != 3.3f ? View.VISIBLE : View.GONE);
//         binding.bottomPanel.tvPostDate.setSelected(true);
//
//         url = currentPost.getDisplayUrl();
//         // releasePlayer();
//
//         binding.btnDownload.setVisibility(containerLayoutParams.weight == 3.3f ? View.VISIBLE : View.GONE);
//         if (currentPost.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) setupVideo();
//         else setupImage();
//     }
//
//     private void setupPostInfoBar(final String username,
//                                   final MediaItemType itemType,
//                                   final String locationName,
//                                   final String location) {
//         if (prevUsername == null || !prevUsername.equals(username)) {
//             binding.topPanel.ivProfilePic.setImageRequest(null);
//             if (!Utils.isEmpty(username) && username.charAt(0) == '@') {
//                 new ProfileFetcher(username.substring(1), result -> {
//                     profileModel = result;
//
//                     if (result != null) {
//                         // final String hdProfilePic = result.getHdProfilePic();
//                         // final String sdProfilePic = result.getSdProfilePic();
//                         postUserId = result.getId();
//                         // final boolean hdPicEmpty = Utils.isEmpty(hdProfilePic);
//                         binding.topPanel.ivProfilePic.setImageURI(profileModel.getSdProfilePic());
//                         binding.topPanel.viewStoryPost.setOnClickListener(v -> {
//                             if (result.isPrivate()) {
//                                 Toast.makeText(requireContext(), R.string.share_private_post, Toast.LENGTH_LONG).show();
//                             }
//                             Intent sharingIntent = new Intent(Intent.ACTION_SEND);
//                             sharingIntent.setType("text/plain");
//                             sharingIntent.putExtra(Intent.EXTRA_TEXT, "https://instagram.com/p/" + postShortCode);
//                             startActivity(Intent.createChooser(sharingIntent, result.isPrivate()
//                                     ? getString(R.string.share_private_post)
//                                     : getString(R.string.share_public_post)));
//                         });
//                     }
//                 }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//             }
//             prevUsername = username;
//         }
//
//         final String titlePrefix = getString(itemType == MediaItemType.MEDIA_TYPE_VIDEO ? R.string.post_viewer_video_post : R.string.post_viewer_image_post);
//         if (Utils.isEmpty(username)) binding.topPanel.title.setText(titlePrefix);
//         else {
//             final int titleLen = username.length();
//             final SpannableString spannableString = new SpannableString(username);
//             spannableString.setSpan(new CommentMentionClickSpan(), 0, titleLen, 0);
//             binding.topPanel.title.setText(spannableString);
//         }
//
//         if (location == null) {
//             binding.topPanel.location.setVisibility(View.GONE);
//             binding.topPanel.title.setLayoutParams(new RelativeLayout.LayoutParams(
//                     RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
//         } else {
//             binding.topPanel.location.setVisibility(View.VISIBLE);
//             binding.topPanel.location.setText(locationName);
//             // binding.topPanel.location.setOnClickListener(v -> searchUsername(location));
//             binding.topPanel.title.setLayoutParams(new RelativeLayout.LayoutParams(
//                     RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
//             ));
//         }
//     }
//
//     private void setupVideo() {
//         binding.playerView.setVisibility(View.VISIBLE);
//         binding.bottomPanel.btnDownload.setVisibility(View.VISIBLE);
//         binding.bottomPanel.btnMute.setVisibility(View.VISIBLE);
//         binding.progressView.setVisibility(View.GONE);
//         binding.imageViewer.setVisibility(View.GONE);
//         binding.imageViewer.setController(null);
//
//         if (currentPost.getVideoViews() > -1) {
//             binding.bottomPanel.videoViewsContainer.setVisibility(View.VISIBLE);
//             binding.bottomPanel.tvVideoViews.setText(String.valueOf(currentPost.getVideoViews()));
//         }
//
//         player = new SimpleExoPlayer.Builder(requireContext()).build();
//         binding.playerView.setPlayer(player);
//         float vol = Utils.settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
//         if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;
//
//         player.setVolume(vol);
//         player.setPlayWhenReady(Utils.settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));
//         final Context context = requireContext();
//         final DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, "instagram");
//         final SimpleCache simpleCache = Utils.getSimpleCacheInstance(context);
//         CacheDataSourceFactory cacheDataSourceFactory = null;
//         if (simpleCache != null) {
//             cacheDataSourceFactory = new CacheDataSourceFactory(simpleCache, dataSourceFactory);
//         }
//         final DataSource.Factory factory = cacheDataSourceFactory != null ? cacheDataSourceFactory : dataSourceFactory;
//         final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url));
//         mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
//             @Override
//             public void onLoadCompleted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
//                 binding.progressView.setVisibility(View.GONE);
//             }
//
//             @Override
//             public void onLoadStarted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
//                 binding.progressView.setVisibility(View.VISIBLE);
//             }
//
//             @Override
//             public void onLoadCanceled(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
//                 binding.progressView.setVisibility(View.GONE);
//             }
//
//             @Override
//             public void onLoadError(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
//                 binding.progressView.setVisibility(View.GONE);
//             }
//         });
//         player.prepare(mediaSource);
//         player.setVolume(vol);
//         binding.bottomPanel.btnMute.setImageResource(vol == 0f ? R.drawable.ic_volume_up_24 : R.drawable.ic_volume_off_24);
//         binding.bottomPanel.btnMute.setOnClickListener(onClickListener);
//     }
//
//     private void setupImage() {
//         binding.bottomPanel.videoViewsContainer.setVisibility(View.GONE);
//         binding.playerView.setVisibility(View.GONE);
//         binding.progressView.setVisibility(View.VISIBLE);
//         binding.bottomPanel.btnMute.setVisibility(View.GONE);
//         binding.bottomPanel.btnDownload.setVisibility(View.VISIBLE);
//         binding.imageViewer.setVisibility(View.VISIBLE);
//
//         final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
//                                                                .setLocalThumbnailPreviewsEnabled(true)
//                                                                .setProgressiveRenderingEnabled(true)
//                                                                .build();
//         binding.imageViewer.setController(
//                 Fresco.newDraweeControllerBuilder()
//                       .setImageRequest(requestBuilder)
//                       .setOldController(binding.imageViewer.getController())
//                       .setLowResImageRequest(ImageRequest.fromUri(url))
//                       .setControllerListener(new BaseControllerListener<ImageInfo>() {
//
//                           @Override
//                           public void onFailure(final String id, final Throwable throwable) {
//                               binding.progressView.setVisibility(View.GONE);
//                           }
//
//                           @Override
//                           public void onFinalImageSet(final String id, final ImageInfo imageInfo, final Animatable animatable) {
//                               binding.progressView.setVisibility(View.GONE);
//                           }
//                       })
//                       .build()
//         );
//     }
// }
