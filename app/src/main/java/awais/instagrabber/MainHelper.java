// package awais.instagrabber;
//
// import android.content.Context;
// import android.content.DialogInterface;
// import android.content.Intent;
// import android.content.res.ColorStateList;
// import android.content.res.Resources;
// import android.graphics.Typeface;
// import android.net.Uri;
// import android.os.AsyncTask;
// import android.os.Bundle;
// import android.text.SpannableStringBuilder;
// import android.text.method.LinkMovementMethod;
// import android.text.style.RelativeSizeSpan;
// import android.text.style.StyleSpan;
// import android.util.Log;
// import android.util.TypedValue;
// import android.view.View;
// import android.view.ViewGroup;
// import android.widget.AdapterView;
// import android.widget.ArrayAdapter;
// import android.widget.ImageView;
// import android.widget.LinearLayout;
// import android.widget.TextView;
// import android.widget.Toast;
//
// import androidx.annotation.NonNull;
// import androidx.appcompat.app.AlertDialog;
// import androidx.core.content.ContextCompat;
// import androidx.core.view.GravityCompat;
// import androidx.core.widget.ImageViewCompat;
// import androidx.recyclerview.widget.LinearLayoutManager;
// import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
// import com.bumptech.glide.Glide;
// import com.bumptech.glide.RequestManager;
// import com.facebook.common.executors.UiThreadImmediateExecutorService;
// import com.facebook.datasource.BaseDataSubscriber;
// import com.facebook.datasource.DataSource;
// import com.facebook.drawee.backends.pipeline.Fresco;
// import com.facebook.imagepipeline.request.ImageRequest;
//
// import java.io.DataOutputStream;
// import java.net.HttpURLConnection;
// import java.net.URL;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Map;
//
// import awais.instagrabber.activities.CommentsViewerFragment;
// import awais.instagrabber.activities.MainActivityBackup;
// import awais.instagrabber.activities.PostViewer;
// import awais.instagrabber.adapters.DiscoverAdapter;
// import awais.instagrabber.adapters.FeedAdapter;
// import awais.instagrabber.adapters.PostsAdapter;
// import awais.instagrabber.adapters.viewholder.feed.FeedItemViewHolder;
// import awais.instagrabber.asyncs.DiscoverFetcher;
// import awais.instagrabber.asyncs.FeedFetcher;
// import awais.instagrabber.asyncs.FeedStoriesFetcher;
// import awais.instagrabber.asyncs.HashtagFetcher;
// import awais.instagrabber.asyncs.HighlightsFetcher;
// import awais.instagrabber.asyncs.LocationFetcher;
// import awais.instagrabber.asyncs.PostsFetcher;
// import awais.instagrabber.asyncs.ProfileFetcher;
// import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
// import awais.instagrabber.asyncs.i.iTopicFetcher;
// import awais.instagrabber.customviews.MouseDrawer;
// import awais.instagrabber.customviews.RamboTextView;
// import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
// import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
// import awais.instagrabber.customviews.helpers.PauseGlideOnFlingScrollListener;
// import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
// import awais.instagrabber.customviews.helpers.VideoAwareRecyclerScroller;
// import awais.instagrabber.fragments.SavedViewerFragment;
// import awais.instagrabber.interfaces.FetchListener;
// import awais.instagrabber.interfaces.MentionClickListener;
// import awais.instagrabber.models.BasePostModel;
// import awais.instagrabber.models.DiscoverItemModel;
// import awais.instagrabber.models.DiscoverTopicModel;
// import awais.instagrabber.models.FeedModel;
// import awais.instagrabber.models.FeedStoryModel;
// import awais.instagrabber.models.IntentModel;
// import awais.instagrabber.models.PostModel;
// import awais.instagrabber.models.ProfileModel;
// import awais.instagrabber.models.ViewerPostModel;
// import awais.instagrabber.models.enums.DownloadMethod;
// import awais.instagrabber.models.enums.IntentModelType;
// import awais.instagrabber.models.enums.MediaItemType;
// import awais.instagrabber.models.enums.PostItemType;
// import awais.instagrabber.utils.Constants;
// import awais.instagrabber.utils.DataBox;
// import awais.instagrabber.utils.Utils;
// import awaisomereport.LogCollector;
//
// import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
// import static awais.instagrabber.utils.Constants.BOTTOM_TOOLBAR;
// import static awais.instagrabber.utils.Utils.logCollector;
// import static awais.instagrabber.utils.Utils.settingsHelper;
//
// @Deprecated
// public final class MainHelper implements SwipeRefreshLayout.OnRefreshListener {
//     private static final String TAG = "MainHelper";
//     private static final double MAX_VIDEO_HEIGHT = 0.9 * Utils.displayMetrics.heightPixels;
//     private static final int RESIZED_VIDEO_HEIGHT = (int) (0.8 * Utils.displayMetrics.heightPixels);
//     public static final boolean SHOULD_AUTO_PLAY = settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS);
//
//     private static AsyncTask<?, ?, ?> currentlyExecuting;
//     private AsyncTask<Void, Void, FeedStoryModel[]> prevStoriesFetcher;
//     private FeedStoryModel[] stories;
//     private boolean hasNextPage = false;
//     private boolean feedHasNextPage = false;
//     private boolean discoverHasMore = false;
//     private String endCursor = null;
//     private String feedEndCursor = null;
//     private String discoverEndMaxId = null;
//     private String topic = null;
//     private String rankToken = null;
//     private String[] topicIds = null;
//
//     private final boolean autoloadPosts;
//     private final FetchListener<PostModel[]> postsFetchListener = new FetchListener<PostModel[]>() {
//         @Override
//         public void onResult(final PostModel[] result) {
//             if (result != null) {
//                 final int oldSize = mainActivity.allItems.size();
//                 mainActivity.allItems.addAll(Arrays.asList(result));
//
//                 postsAdapter.notifyItemRangeInserted(oldSize, result.length);
//
//                 mainActivity.mainBinding.profileView.mainPosts.post(() -> {
//                     mainActivity.mainBinding.profileView.mainPosts.setNestedScrollingEnabled(true);
//                     mainActivity.mainBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
//                 });
//
//                 if (isHashtag)
//                     mainActivity.mainBinding.toolbar.toolbar.setTitle(mainActivity.userQuery);
//                 else if (isLocation)
//                     mainActivity.mainBinding.toolbar.toolbar.setTitle(mainActivity.locationModel.getName());
//                 else
//                     mainActivity.mainBinding.toolbar.toolbar.setTitle("@" + mainActivity.profileModel.getUsername());
//
//                 final PostModel model = result[result.length - 1];
//                 if (model != null) {
//                     endCursor = model.getEndCursor();
//                     hasNextPage = model.hasNextPage();
//                     if (autoloadPosts && hasNextPage)
//                         currentlyExecuting = new PostsFetcher(
//                                 mainActivity.profileModel != null ? mainActivity.profileModel.getId()
//                                                                   : (mainActivity.hashtagModel != null
//                                                                      ? mainActivity.userQuery
//                                                                      : mainActivity.locationModel.getId()),
//                                 mainActivity.profileModel != null
//                                 ? PostItemType.MAIN
//                                 : (mainActivity.hashtagModel != null ? PostItemType.HASHTAG : PostItemType.LOCATION),
//                                 endCursor,
//                                 this)
//                                 .setUsername((isLocation || isHashtag) ? null : mainActivity.profileModel.getUsername())
//                                 .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                     else {
//                         mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                     }
//                     model.setPageCursor(false, null);
//                 }
//             } else {
//                 mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                 mainActivity.mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
//                 mainActivity.mainBinding.profileView.privatePage2.setText(R.string.empty_acc);
//                 mainActivity.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
//             }
//         }
//     };
//     private final FetchListener<FeedModel[]> feedFetchListener = new FetchListener<FeedModel[]>() {
//         @Override
//         public void doBefore() {
//             mainActivity.mainBinding.feedView.feedSwipeRefreshLayout
//                     .post(() -> mainActivity.mainBinding.feedView.feedSwipeRefreshLayout.setRefreshing(true));
//         }
//
//         @Override
//         public void onResult(final FeedModel[] result) {
//             if (result == null) {
//                 return;
//             }
//             final int oldSize = mainActivity.feedItems.size();
//             final HashMap<String, FeedModel> thumbToFeedMap = new HashMap<>();
//             for (final FeedModel feedModel : result) {
//                 thumbToFeedMap.put(feedModel.getThumbnailUrl(), feedModel);
//             }
//             final BaseDataSubscriber<Void> subscriber = new BaseDataSubscriber<Void>() {
//                 int success = 0;
//                 int failed = 0;
//
//                 @Override
//                 protected void onNewResultImpl(@NonNull final DataSource<Void> dataSource) {
//                     // dataSource
//                     final Map<String, Object> extras = dataSource.getExtras();
//                     if (extras == null) {
//                         return;
//                     }
//                     // Log.d(TAG, "extras: " + extras);
//                     final Uri thumbUri = (Uri) extras.get("uri_source");
//                     if (thumbUri == null) {
//                         return;
//                     }
//                     final Integer encodedWidth = (Integer) extras.get("encoded_width");
//                     final Integer encodedHeight = (Integer) extras.get("encoded_height");
//                     if (encodedWidth == null || encodedHeight == null) {
//                         return;
//                     }
//                     final FeedModel feedModel = thumbToFeedMap.get(thumbUri.toString());
//                     if (feedModel == null) {
//                         return;
//                     }
//                     int requiredWidth = Utils.displayMetrics.widthPixels;
//                     int resultingHeight = Utils.getResultingHeight(requiredWidth, encodedHeight, encodedWidth);
//                     if (feedModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO && resultingHeight >= MAX_VIDEO_HEIGHT) {
//                         // If its a video and the height is too large, need to reduce the height,
//                         // so that entire video fits on screen
//                         resultingHeight = RESIZED_VIDEO_HEIGHT;
//                         requiredWidth = Utils.getResultingWidth(RESIZED_VIDEO_HEIGHT, resultingHeight, requiredWidth);
//                     }
//                     feedModel.setImageWidth(requiredWidth);
//                     feedModel.setImageHeight(resultingHeight);
//                     success++;
//                     updateAdapter();
//                 }
//
//                 @Override
//                 protected void onFailureImpl(@NonNull final DataSource<Void> dataSource) {
//                     failed++;
//                     updateAdapter();
//                 }
//
//                 public void updateAdapter() {
//                     if (failed + success != result.length) return;
//                     mainActivity.feedItems.addAll(Arrays.asList(result));
//                     feedAdapter.submitList(mainActivity.feedItems);
//                     feedAdapter.notifyItemRangeInserted(oldSize, result.length);
//
//                     mainActivity.mainBinding.feedView.feedPosts
//                             .post(() -> mainActivity.mainBinding.feedView.feedPosts.setNestedScrollingEnabled(true));
//
//                     final PostModel feedPostModel = result[result.length - 1];
//                     if (feedPostModel != null) {
//                         feedEndCursor = feedPostModel.getEndCursor();
//                         feedHasNextPage = feedPostModel.hasNextPage();
//                         feedPostModel.setPageCursor(false, null);
//                     }
//                     mainActivity.mainBinding.feedView.feedSwipeRefreshLayout.setRefreshing(false);
//                 }
//             };
//             for (final FeedModel feedModel : result) {
//                 final DataSource<Void> ds = Fresco.getImagePipeline().prefetchToBitmapCache(ImageRequest.fromUri(feedModel.getThumbnailUrl()), null);
//                 ds.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
//             }
//         }
//     };
//     private final FetchListener<DiscoverItemModel[]> discoverFetchListener = new FetchListener<DiscoverItemModel[]>() {
//         @Override
//         public void doBefore() {
//             mainActivity.mainBinding.discoverSwipeRefreshLayout.setRefreshing(true);
//         }
//
//         @Override
//         public void onResult(final DiscoverItemModel[] result) {
//             if (result == null || result.length == 0) {
//                 Toast.makeText(mainActivity, R.string.discover_empty, Toast.LENGTH_SHORT).show();
//             } else {
//                 final int oldSize = mainActivity.discoverItems.size();
//                 mainActivity.discoverItems.addAll(Arrays.asList(result));
//                 discoverAdapter.notifyItemRangeInserted(oldSize, result.length);
//
//                 final DiscoverItemModel discoverItemModel = result[result.length - 1];
//                 if (discoverItemModel != null) {
//                     discoverEndMaxId = discoverItemModel.getNextMaxId();
//                     discoverHasMore = discoverItemModel.hasMore();
//                     discoverItemModel.setMore(false, null);
//                 }
//             }
//
//             mainActivity.mainBinding.discoverSwipeRefreshLayout.setRefreshing(false);
//         }
//     };
//     private final FetchListener<DiscoverTopicModel> topicFetchListener = new FetchListener<DiscoverTopicModel>() {
//         @Override
//         public void doBefore() {}
//
//         @Override
//         public void onResult(final DiscoverTopicModel result) {
//             if (result != null) {
//                 topicIds = result.getIds();
//                 rankToken = result.getToken();
//                 ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
//                         mainActivity, android.R.layout.simple_spinner_dropdown_item, result.getNames());
//                 mainActivity.mainBinding.discoverType.setAdapter(spinnerArrayAdapter);
//             }
//         }
//     };
//     private final FetchListener<FeedStoryModel[]> feedStoriesListener = new FetchListener<FeedStoryModel[]>() {
//         @Override
//         public void doBefore() {
//             mainActivity.mainBinding.feedView.feedStories.setVisibility(View.GONE);
//         }
//
//         @Override
//         public void onResult(final FeedStoryModel[] result) {
//             // feedStoriesAdapter.setData(result);
//             if (result != null && result.length > 0) {
//                 mainActivity.mainBinding.feedView.feedStories.setVisibility(View.VISIBLE);
//                 stories = result;
//             }
//         }
//     };
//     private final MentionClickListener mentionClickListener = new MentionClickListener() {
//         @Override
//         public void onClick(final RamboTextView view, final String text, final boolean isHashtag, final boolean isLocation) {
//             new AlertDialog.Builder(mainActivity)
//                     .setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
//                     .setTitle(text).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok, (dialog, which) -> {
//                 if (MainActivityBackup.scanHack != null) MainActivityBackup.scanHack.onResult(text);
//             }).show();
//         }
//     };
//     // private final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter(null, new View.OnClickListener() {
//     //     @Override
//     //     public void onClick(final View v) {
//     //         final Object tag = v.getTag();
//     //         if (tag instanceof FeedStoryModel) {
//     //             final FeedStoryModel feedStoryModel = (FeedStoryModel) tag;
//     //             final int index = indexOfIntArray(stories, feedStoryModel);
//     //             new iStoryStatusFetcher(feedStoryModel.getStoryMediaId(), null, false, false, false, false, result -> {
//     //                 if (result != null && result.length > 0)
//     //                     mainActivity.startActivity(new Intent(mainActivity, StoryViewer.class)
//     //                             .putExtra(Constants.EXTRAS_STORIES, result)
//     //                             .putExtra(Constants.EXTRAS_USERNAME, feedStoryModel.getProfileModel().getUsername())
//     //                             .putExtra(Constants.FEED, stories)
//     //                             .putExtra(Constants.FEED_ORDER, index)
//     //                     );
//     //                 else
//     //                     Toast.makeText(mainActivity, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
//     //             }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//     //         }
//     //     }
//     // });
//     private MainActivityBackup mainActivity;
//     private Resources resources;
//     private final View collapsingToolbar;
//     private final RecyclerLazyLoader lazyLoader;
//     private boolean isHashtag;
//     private boolean isLocation;
//     private PostsAdapter postsAdapter;
//     private FeedAdapter feedAdapter;
//     private RecyclerLazyLoader feedLazyLoader, discoverLazyLoader;
//     private DiscoverAdapter discoverAdapter;
//     private String cookie = settingsHelper.getString(Constants.COOKIE);
//     private boolean isLoggedIn;
//     private RequestManager glide;
//     private VideoAwareRecyclerScroller videoAwareRecyclerScroller;
//
//     public MainHelper(@NonNull final MainActivityBackup mainActivity) {
//         stopCurrentExecutor();
//
//         this.mainActivity = mainActivity;
//         this.resources = mainActivity.getResources();
//         this.autoloadPosts = settingsHelper.getBoolean(AUTOLOAD_POSTS);
//         glide = Glide.with(mainActivity);
//
//         mainActivity.mainBinding.profileView.swipeRefreshLayout.setOnRefreshListener(this);
//         mainActivity.mainBinding.profileView.mainUrl.setMovementMethod(new LinkMovementMethod());
//
//         final LinearLayout iconSlider = mainActivity.findViewById(R.id.iconSlider);
//         final ImageView iconFeed = (ImageView) iconSlider.getChildAt(0);
//         final ImageView iconProfile = (ImageView) iconSlider.getChildAt(1);
//         final ImageView iconDiscover = (ImageView) iconSlider.getChildAt(2);
//
//         final boolean isBottomToolbar = settingsHelper.getBoolean(BOTTOM_TOOLBAR);
//         isLoggedIn = !Utils.isEmpty(cookie) && Utils.getUserIdFromCookie(cookie) != null;
//         if (!isLoggedIn) {
//             mainActivity.mainBinding.drawerLayout.removeView(mainActivity.mainBinding.feedView.feedLayout);
//             mainActivity.mainBinding.drawerLayout.removeView(mainActivity.mainBinding.discoverLayout);
//             iconFeed.setAlpha(0.4f);
//             iconDiscover.setAlpha(0.4f);
//         } else {
//             iconFeed.setAlpha(1f);
//             iconDiscover.setAlpha(1f);
//
//             setupExplore();
//
//             setupFeed();
//
//             final TypedValue resolvedAttr = new TypedValue();
//             mainActivity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, resolvedAttr, true);
//
//             final int selectedItem = ContextCompat.getColor(mainActivity, resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data);
//             final ColorStateList colorStateList = ColorStateList.valueOf(selectedItem);
//
//             mainActivity.mainBinding.toolbar.toolbar.measure(0, -1);
//             final int toolbarMeasuredHeight = mainActivity.mainBinding.toolbar.toolbar.getMeasuredHeight();
//
//             final ViewGroup.LayoutParams layoutParams = mainActivity.mainBinding.toolbar.toolbar.getLayoutParams();
//             final MouseDrawer.DrawerListener simpleDrawerListener = new MouseDrawer.DrawerListener() {
//                 private final String titleDiscover = resources.getString(R.string.title_discover);
//
//                 @Override
//                 public void onDrawerSlide(final View drawerView, @MouseDrawer.EdgeGravity final int gravity, final float slideOffset) {
//                     final int currentIconAlpha = (int) Math.max(100, 255 - 255 * slideOffset);
//                     final int otherIconAlpha = (int) Math.max(100, 255 * slideOffset);
//
//                     ImageViewCompat.setImageTintList(iconProfile, colorStateList.withAlpha(currentIconAlpha));
//
//                     final boolean drawerOpening = slideOffset > 0.0f;
//                     final int alpha;
//                     final ColorStateList imageTintList;
//
//                     if (gravity == GravityCompat.START) {
//                         // this helps hide the toolbar when opening feed
//
//                         final int roundedToolbarHeight;
//                         final float toolbarHeight;
//
//                         if (isBottomToolbar) {
//                             toolbarHeight = toolbarMeasuredHeight * slideOffset;
//                             roundedToolbarHeight = -Math.round(toolbarHeight);
//                         } else {
//                             toolbarHeight = -toolbarMeasuredHeight * slideOffset;
//                             roundedToolbarHeight = Math.round(toolbarHeight);
//                         }
//
//                         layoutParams.height = Math.max(0, Math.min(toolbarMeasuredHeight, toolbarMeasuredHeight + roundedToolbarHeight));
//                         mainActivity.mainBinding.toolbar.toolbar.setLayoutParams(layoutParams);
//                         mainActivity.mainBinding.toolbar.toolbar.setTranslationY(toolbarHeight);
//
//                         imageTintList = ImageViewCompat.getImageTintList(iconDiscover);
//                         alpha = imageTintList != null ? (imageTintList.getDefaultColor() & 0xFF_000000) >> 24 : 0;
//
//                         if (drawerOpening && alpha > 100)
//                             ImageViewCompat.setImageTintList(iconDiscover, colorStateList.withAlpha(currentIconAlpha));
//
//                         ImageViewCompat.setImageTintList(iconFeed, colorStateList.withAlpha(otherIconAlpha));
//                     } else {
//                         // this changes toolbar title
//                         mainActivity.mainBinding.toolbar.toolbar.setTitle(slideOffset >= 0.466 ? titleDiscover :
//                                                                           (mainActivity.userQuery == null
//                                                                            ? resources.getString(R.string.app_name)
//                                                                            : mainActivity.userQuery));
//
//                         imageTintList = ImageViewCompat.getImageTintList(iconFeed);
//                         alpha = imageTintList != null ? (imageTintList.getDefaultColor() & 0xFF_000000) >> 24 : 0;
//
//                         if (drawerOpening && alpha > 100)
//                             ImageViewCompat.setImageTintList(iconFeed, colorStateList.withAlpha(currentIconAlpha));
//
//                         ImageViewCompat.setImageTintList(iconDiscover, colorStateList.withAlpha(otherIconAlpha));
//                     }
//                 }
//
//                 @Override
//                 public void onDrawerOpened(@NonNull final View drawerView, @MouseDrawer.EdgeGravity final int gravity) {
//                     if (gravity == GravityCompat.START || drawerView == mainActivity.mainBinding.feedView.feedLayout) {
//                         if (videoAwareRecyclerScroller != null) {
//                             if (SHOULD_AUTO_PLAY) {
//                                 videoAwareRecyclerScroller.startPlaying();
//                             }
//                         }
//                     } else {
//                         // clear selection
//                         isSelectionCleared();
//                     }
//                 }
//
//                 @Override
//                 public void onDrawerClosed(@NonNull final View drawerView, @MouseDrawer.EdgeGravity final int gravity) {
//                     if (gravity == GravityCompat.START || drawerView == mainActivity.mainBinding.feedView.feedLayout) {
//                         if (videoAwareRecyclerScroller != null) {
//                             videoAwareRecyclerScroller.stopPlaying();
//                         }
//                     } else {
//                         // clear selection
//                         isSelectionCleared();
//                     }
//                 }
//             };
//
//             ImageViewCompat.setImageTintList(iconFeed, colorStateList.withAlpha(100)); // to change colors when created
//             ImageViewCompat.setImageTintList(iconProfile, colorStateList.withAlpha(255)); // to change colors when created
//             ImageViewCompat.setImageTintList(iconDiscover, colorStateList.withAlpha(100)); // to change colors when created
//
//             mainActivity.mainBinding.drawerLayout.addDrawerListener(simpleDrawerListener);
//         }
//
//         collapsingToolbar = mainActivity.mainBinding.profileView.appBarLayout.getChildAt(0);
//
//         mainActivity.mainBinding.profileView.mainPosts.setNestedScrollingEnabled(false);
//         mainActivity.mainBinding.profileView.highlightsList
//                 .setLayoutManager(new LinearLayoutManager(mainActivity, LinearLayoutManager.HORIZONTAL, false));
//         mainActivity.mainBinding.profileView.highlightsList.setAdapter(mainActivity.highlightsAdapter);
//
//         // int color = -1;
//         // final Drawable background = main.mainBinding.profileView.appBarLayout.getBackground();
//         // if (background instanceof MaterialShapeDrawable) {
//         //     final MaterialShapeDrawable drawable = (MaterialShapeDrawable) background;
//         //     final ColorStateList fillColor = drawable.getFillColor();
//         //     if (fillColor != null) color = fillColor.getDefaultColor();
//         // } else {
//         //     final Bitmap bitmap = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);
//         //     final Canvas canvas = new Canvas();
//         //     canvas.setBitmap(bitmap);
//         //     background.draw(canvas);
//         //     color = bitmap.getPixel(4, 4);
//         //     if (!bitmap.isRecycled()) bitmap.recycle();
//         // }
//         // if (color == -1 || color == 0) color = resources.getBoolean(R.bool.isNight) ? 0xff212121 : 0xfff5f5f5;
//         // main.mainBinding.profileView.profileInfo.setBackgroundColor(color);
//         // if (!isBottomToolbar) main.mainBinding.toolbar.toolbar.setBackgroundColor(color);
//
//         // main.mainBinding.profileView.appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
//         //     private int height;
//         //
//         //     @Override
//         //     public void onOffsetChanged(final AppBarLayout appBarLayout, final int verticalOffset) {
//         //         if (height == 0) {
//         //             height = main.mainBinding.profileView.profileInfo.getHeight();
//         //             collapsingToolbar.setMinimumHeight(height);
//         //         }
//         //         main.mainBinding.profileView.profileInfo.setTranslationY(-Math.min(0, verticalOffset));
//         //     }
//         // });
//
//         mainActivity.setSupportActionBar(mainActivity.mainBinding.toolbar.toolbar);
//         if (isBottomToolbar) {
//             final LinearLayout linearLayout = (LinearLayout) mainActivity.mainBinding.toolbar.toolbar.getParent();
//             linearLayout.removeView(mainActivity.mainBinding.toolbar.toolbar);
//             linearLayout.addView(mainActivity.mainBinding.toolbar.toolbar, linearLayout.getChildCount());
//         }
//
//         // change the next number to adjust grid
//         final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(mainActivity, Utils.convertDpToPx(110));
//         mainActivity.mainBinding.profileView.mainPosts.setLayoutManager(layoutManager);
//         mainActivity.mainBinding.profileView.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
//         // mainActivity.mainBinding.profileView.mainPosts.setAdapter(postsAdapter = new PostsAdapter(/*mainActivity.allItems,*/ v -> {
//         //     final Object tag = v.getTag();
//         //     if (tag instanceof PostModel) {
//         //         final PostModel postModel = (PostModel) tag;
//         //
//         //         if (postsAdapter.isSelecting) toggleSelection(postModel);
//         //         else mainActivity.startActivity(new Intent(mainActivity, PostViewer.class)
//         //                 .putExtra(Constants.EXTRAS_INDEX, postModel.getPosition())
//         //                 .putExtra(Constants.EXTRAS_POST, postModel)
//         //                 .putExtra(Constants.EXTRAS_USER, mainActivity.userQuery)
//         //                 .putExtra(Constants.EXTRAS_TYPE, ItemGetType.MAIN_ITEMS));
//         //     }
//         // }, v -> { // long click listener
//         //     // final Object tag = v.getTag();
//         //     // if (tag instanceof PostModel) {
//         //     //     postsAdapter.isSelecting = true;
//         //     //     toggleSelection((PostModel) tag);
//         //     // }
//         //     // return true;
//         //     return false;
//         // }));
//
//         this.lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
//             if ((!autoloadPosts || isHashtag) && hasNextPage) {
//                 mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(true);
//                 stopCurrentExecutor();
//                 currentlyExecuting = new PostsFetcher(mainActivity.profileModel != null ? mainActivity.profileModel.getId()
//                                                                                         : (mainActivity.hashtagModel != null
//                                                                                            ? mainActivity.userQuery
//                                                                                            : mainActivity.locationModel.getId()),
//                                                       mainActivity.profileModel != null
//                                                       ? PostItemType.MAIN
//                                                       : (mainActivity.hashtagModel != null ? PostItemType.HASHTAG : PostItemType.LOCATION),
//                                                       endCursor,
//                                                       postsFetchListener)
//                         .setUsername((isHashtag || isLocation) ? null : mainActivity.profileModel.getUsername())
//                         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                 endCursor = null;
//             }
//         });
//         mainActivity.mainBinding.profileView.mainPosts.addOnScrollListener(lazyLoader);
//     }
//
//     private final View.OnClickListener clickListener = v -> {
//         if (mainActivity == null) {
//             return;
//         }
//         final Object tag = v.getTag();
//         final Context context = v.getContext();
//
//         if (tag instanceof FeedModel) {
//             final FeedModel feedModel = (FeedModel) tag;
//
//             if (v instanceof RamboTextView) {
//                 if (feedModel.isMentionClicked())
//                     feedModel.toggleCaption();
//                 feedModel.setMentionClicked(false);
//                 if (!FeedItemViewHolder.expandCollapseTextView((RamboTextView) v, feedModel.getPostCaption()))
//                     feedModel.toggleCaption();
//
//             } else {
//                 final int id = v.getId();
//                 switch (id) {
//                     case R.id.btnComments:
//                         mainActivity.startActivityForResult(new Intent(mainActivity, CommentsViewerFragment.class)
//                                                                     .putExtra(Constants.EXTRAS_SHORTCODE, feedModel.getShortCode())
//                                                                     .putExtra(Constants.EXTRAS_POST, feedModel.getPostId())
//                                                                     .putExtra(Constants.EXTRAS_USER, feedModel.getProfileModel().getId()), 6969);
//                         break;
//
//                     case R.id.viewStoryPost:
//                         mainActivity.startActivity(new Intent(mainActivity, PostViewer.class)
//                                                            .putExtra(Constants.EXTRAS_INDEX, feedModel.getPosition())
//                                                            .putExtra(Constants.EXTRAS_POST, new PostModel(feedModel.getShortCode(), false))
//                                                            .putExtra(Constants.EXTRAS_TYPE, PostItemType.FEED));
//                         break;
//
//                     case R.id.btnDownload:
//                         ProfileModel profileModel = feedModel.getProfileModel();
//                         final String username = profileModel != null ? profileModel.getUsername() : null;
//
//                         final ViewerPostModel[] sliderItems = feedModel.getSliderItems();
//
//                         if (feedModel.getItemType() != MediaItemType.MEDIA_TYPE_SLIDER || sliderItems == null || sliderItems.length == 1)
//                             Utils.batchDownload(context, username, DownloadMethod.DOWNLOAD_FEED, Collections.singletonList(feedModel));
//                         else {
//                             final ArrayList<BasePostModel> postModels = new ArrayList<>();
//                             final DialogInterface.OnClickListener clickListener1 = (dialog, which) -> {
//                                 postModels.clear();
//
//                                 final boolean breakWhenFoundSelected = which == DialogInterface.BUTTON_POSITIVE;
//
//                                 for (final ViewerPostModel sliderItem : sliderItems) {
//                                     if (sliderItem != null) {
//                                         if (!breakWhenFoundSelected)
//                                             postModels.add(sliderItem);
//                                         else if (sliderItem.isSelected()) {
//                                             postModels.add(sliderItem);
//                                             break;
//                                         }
//                                     }
//                                 }
//
//                                 // shows 0 items on first item of viewpager cause onPageSelected hasn't been called yet
//                                 if (breakWhenFoundSelected && postModels.size() == 0)
//                                     postModels.add(sliderItems[0]);
//
//                                 if (postModels.size() > 0)
//                                     Utils.batchDownload(context, username, DownloadMethod.DOWNLOAD_FEED, postModels);
//                             };
//
//                             new AlertDialog.Builder(context).setTitle(R.string.post_viewer_download_dialog_title)
//                                                             .setPositiveButton(R.string.post_viewer_download_current, clickListener1)
//                                                             .setNegativeButton(R.string.post_viewer_download_album, clickListener1).show();
//                         }
//                         break;
//
//                     case R.id.ivProfilePic:
//                         profileModel = feedModel.getProfileModel();
//                         if (profileModel != null)
//                             mentionClickListener.onClick(null, profileModel.getUsername(), false, false);
//                         break;
//                 }
//             }
//         }
//     };
//
//     private void setupFeed() {
//         mainActivity.mainBinding.feedView.feedStories.setLayoutManager(new LinearLayoutManager(mainActivity, LinearLayoutManager.HORIZONTAL, false));
//         // mainActivity.mainBinding.feedView.feedStories.setAdapter(feedStoriesAdapter);
//         refreshFeedStories();
//
//         final LinearLayoutManager layoutManager = new LinearLayoutManager(mainActivity);
//         mainActivity.mainBinding.feedView.feedPosts.setHasFixedSize(true);
//         mainActivity.mainBinding.feedView.feedPosts.setLayoutManager(layoutManager);
//         mainActivity.mainBinding.feedView.feedPosts.setAdapter(feedAdapter = new FeedAdapter(clickListener, (view, text, isHashtag, isLocation) ->
//                 new AlertDialog.Builder(mainActivity)
//                         .setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
//                         .setTitle(text).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok, (dialog, which) -> {
//                     if (MainActivityBackup.scanHack != null) {
//                         mainActivity.mainBinding.drawerLayout.closeDrawers();
//                         MainActivityBackup.scanHack.onResult(text);
//                     }
//                 }).show()));
//
//         mainActivity.mainBinding.feedView.feedSwipeRefreshLayout.setOnRefreshListener(() -> {
//             refreshFeedStories();
//
//             if (feedLazyLoader != null) feedLazyLoader.resetState();
//             mainActivity.feedItems.clear();
//             if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
//             new FeedFetcher(feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//         });
//
//         mainActivity.mainBinding.feedView.feedPosts
//                 .addOnScrollListener(feedLazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
//                     if (feedHasNextPage) {
//                         mainActivity.mainBinding.feedView.feedSwipeRefreshLayout.setRefreshing(true);
//                         new FeedFetcher(feedEndCursor, feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                         feedEndCursor = null;
//                     }
//                 }));
//
//         if (SHOULD_AUTO_PLAY) {
//             videoAwareRecyclerScroller = new VideoAwareRecyclerScroller();
//             mainActivity.mainBinding.feedView.feedPosts.addOnScrollListener(videoAwareRecyclerScroller);
//         }
//         mainActivity.mainBinding.feedView.feedPosts.addOnScrollListener(new PauseGlideOnFlingScrollListener(glide));
//
//         new FeedFetcher(feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//     }
//
//     private void refreshFeedStories() {
//         // todo setup feed stories
//         if (prevStoriesFetcher != null) {
//             try {
//                 prevStoriesFetcher.cancel(true);
//             } catch (final Exception e) {
//                 // ignore
//             }
//         }
//         prevStoriesFetcher = new FeedStoriesFetcher(feedStoriesListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//     }
//
//     private void setupExplore() {
//         final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(mainActivity, Utils.convertDpToPx(110));
//         mainActivity.mainBinding.discoverPosts.setLayoutManager(layoutManager);
//         mainActivity.mainBinding.discoverPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
//
//         new iTopicFetcher(topicFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//         mainActivity.mainBinding.discoverType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//             @Override
//             public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                 if (topicIds != null) {
//                     topic = topicIds[pos];
//                     mainActivity.mainBinding.discoverSwipeRefreshLayout.setRefreshing(true);
//                     if (discoverLazyLoader != null) discoverLazyLoader.resetState();
//                     mainActivity.discoverItems.clear();
//                     if (discoverAdapter != null) discoverAdapter.notifyDataSetChanged();
//                     new DiscoverFetcher(topic, null, rankToken, discoverFetchListener, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                 }
//             }
//
//             @Override
//             public void onNothingSelected(AdapterView<?> parent) {
//             }
//         });
//
//         mainActivity.mainBinding.discoverSwipeRefreshLayout.setOnRefreshListener(() -> {
//             if (discoverLazyLoader != null) discoverLazyLoader.resetState();
//             mainActivity.discoverItems.clear();
//             if (discoverAdapter != null) discoverAdapter.notifyDataSetChanged();
//             new DiscoverFetcher(topic, null, rankToken, discoverFetchListener, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//         });
//
//         // mainActivity.mainBinding.discoverPosts.setAdapter(discoverAdapter = new DiscoverAdapter(mainActivity.discoverItems, v -> {
//         //     final Object tag = v.getTag();
//         //     if (tag instanceof DiscoverItemModel) {
//         //         final DiscoverItemModel itemModel = (DiscoverItemModel) tag;
//         //
//         //         if (discoverAdapter.isSelecting) toggleDiscoverSelection(itemModel);
//         //         else mainActivity.startActivity(new Intent(mainActivity, PostViewer.class)
//         //                 .putExtra(Constants.EXTRAS_INDEX, itemModel.getPosition())
//         //                 .putExtra(Constants.EXTRAS_TYPE, ItemGetType.DISCOVER_ITEMS)
//         //                 .putExtra(Constants.EXTRAS_POST, new PostModel(itemModel.getShortCode(), false)));
//         //     }
//         // }, v -> {
//         //     final Object tag = v.getTag();
//         //     if (tag instanceof DiscoverItemModel) {
//         //         discoverAdapter.isSelecting = true;
//         //         toggleDiscoverSelection((DiscoverItemModel) tag);
//         //     }
//         //     return true;
//         // }));
//
//         mainActivity.mainBinding.discoverPosts
//                 .addOnScrollListener(discoverLazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
//                     if (discoverHasMore) {
//                         mainActivity.mainBinding.discoverSwipeRefreshLayout.setRefreshing(true);
//                         new DiscoverFetcher(topic, discoverEndMaxId, rankToken, discoverFetchListener, false)
//                                 .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                         discoverEndMaxId = null;
//                     }
//                 }));
//     }
//
//     public void onIntent(final Intent intent) {
//         if (intent != null) {
//             final String action = intent.getAction();
//             if (!Utils.isEmpty(action) && !Intent.ACTION_MAIN.equals(action)) {
//                 intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//                 boolean error = true;
//
//                 String data = null;
//                 final Bundle extras = intent.getExtras();
//                 if (extras != null) {
//                     final Object extraData = extras.get(Intent.EXTRA_TEXT);
//                     if (extraData != null) {
//                         error = false;
//                         data = extraData.toString();
//                     }
//                 }
//
//                 if (error) {
//                     final Uri intentData = intent.getData();
//                     if (intentData != null) data = intentData.toString();
//                 }
//
//                 if (data != null && !Utils.isEmpty(data)) {
//                     if (data.indexOf('\n') > 0) data = data.substring(data.lastIndexOf('\n') + 1);
//
//                     final IntentModel model = Utils.stripString(data);
//                     if (model != null) {
//                         final String modelText = model.getText();
//                         final IntentModelType modelType = model.getType();
//
//                         if (modelType == IntentModelType.POST) {
//                             mainActivity.startActivityForResult(new Intent(mainActivity, PostViewer.class)
//                                                                         .putExtra(Constants.EXTRAS_USER, mainActivity.userQuery)
//                                                                         .putExtra(Constants.EXTRAS_POST, new PostModel(modelText, false)), 9629);
//                         } else {
//                             mainActivity.addToStack();
//                             mainActivity.userQuery = modelType == IntentModelType.HASHTAG ? ('#' + modelText) :
//                                                      (modelType == IntentModelType.LOCATION ? modelText : ('@' + modelText));
//                             onRefresh();
//                         }
//                     }
//                 }
//             }
//         }
//     }
//
//     @Override
//     public void onRefresh() {
//         mainActivity.mainBinding.drawerLayout.closeDrawers();
//         if (lazyLoader != null) lazyLoader.resetState();
//         stopCurrentExecutor();
//         mainActivity.allItems.clear();
//         mainActivity.selectedItems.clear();
//         if (postsAdapter != null) {
//             // postsAdapter.isSelecting = false;
//             postsAdapter.notifyDataSetChanged();
//         }
//         mainActivity.mainBinding.profileView.appBarLayout.setExpanded(true, true);
//         mainActivity.mainBinding.profileView.privatePage.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.privatePage2.setTextSize(28);
//         // mainActivity.mainBinding.profileView.mainProfileImage.setImageBitmap(null);
//         // mainActivity.mainBinding.profileView.mainHashtagImage.setImageBitmap(null);
//         // mainActivity.mainBinding.profileView.mainLocationImage.setImageBitmap(null);
//         mainActivity.mainBinding.profileView.mainUrl.setText(null);
//         mainActivity.mainBinding.profileView.locationUrl.setText(null);
//         mainActivity.mainBinding.profileView.mainFullName.setText(null);
//         mainActivity.mainBinding.profileView.locationFullName.setText(null);
//         mainActivity.mainBinding.profileView.mainPostCount.setText(null);
//         mainActivity.mainBinding.profileView.mainLocPostCount.setText(null);
//         mainActivity.mainBinding.profileView.mainTagPostCount.setText(null);
//         mainActivity.mainBinding.profileView.mainFollowers.setText(null);
//         mainActivity.mainBinding.profileView.mainFollowing.setText(null);
//         mainActivity.mainBinding.profileView.mainBiography.setText(null);
//         mainActivity.mainBinding.profileView.locationBiography.setText(null);
//         mainActivity.mainBinding.profileView.mainBiography.setEnabled(false);
//         mainActivity.mainBinding.profileView.locationBiography.setEnabled(false);
//         mainActivity.mainBinding.profileView.mainProfileImage.setEnabled(false);
//         mainActivity.mainBinding.profileView.mainLocationImage.setEnabled(false);
//         mainActivity.mainBinding.profileView.mainHashtagImage.setEnabled(false);
//         mainActivity.mainBinding.profileView.mainBiography.setMentionClickListener(null);
//         mainActivity.mainBinding.profileView.locationBiography.setMentionClickListener(null);
//         mainActivity.mainBinding.profileView.mainUrl.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.locationUrl.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.isVerified.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnFollow.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnRestrict.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnBlock.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnSaved.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnLiked.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnTagged.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.btnMap.setVisibility(View.GONE);
//
//         mainActivity.mainBinding.profileView.btnFollow.setOnClickListener(profileActionListener);
//         mainActivity.mainBinding.profileView.btnRestrict.setOnClickListener(profileActionListener);
//         mainActivity.mainBinding.profileView.btnBlock.setOnClickListener(profileActionListener);
//         mainActivity.mainBinding.profileView.btnSaved.setOnClickListener(profileActionListener);
//         mainActivity.mainBinding.profileView.btnLiked.setOnClickListener(profileActionListener);
//         mainActivity.mainBinding.profileView.btnTagged.setOnClickListener(profileActionListener);
//         mainActivity.mainBinding.profileView.btnFollowTag.setOnClickListener(profileActionListener);
//
//         mainActivity.mainBinding.profileView.infoContainer.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.tagInfoContainer.setVisibility(View.GONE);
//         mainActivity.mainBinding.profileView.locInfoContainer.setVisibility(View.GONE);
//
//         mainActivity.mainBinding.profileView.mainPosts.setNestedScrollingEnabled(false);
//         mainActivity.mainBinding.profileView.highlightsList.setVisibility(View.GONE);
//         collapsingToolbar.setVisibility(View.GONE);
//         mainActivity.highlightsAdapter.setData(null);
//
//         mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(mainActivity.userQuery != null);
//         if (mainActivity.userQuery == null) {
//             mainActivity.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
//             return;
//         }
//
//         isHashtag = mainActivity.userQuery.charAt(0) == '#';
//         final boolean isUser = mainActivity.userQuery.charAt(0) == '@';
//         isLocation = mainActivity.userQuery.contains("/");
//         collapsingToolbar.setVisibility(isUser ? View.VISIBLE : View.GONE);
//
//         if (isHashtag) {
//             mainActivity.profileModel = null;
//             mainActivity.locationModel = null;
//             mainActivity.mainBinding.toolbar.toolbar.setTitle(mainActivity.userQuery);
//             mainActivity.mainBinding.profileView.tagInfoContainer.setVisibility(View.VISIBLE);
//             mainActivity.mainBinding.profileView.btnFollowTag.setVisibility(View.GONE);
//
//             currentlyExecuting = new HashtagFetcher(mainActivity.userQuery.substring(1), hashtagModel -> {
//                 mainActivity.hashtagModel = hashtagModel;
//
//                 if (hashtagModel == null) {
//                     mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                     Toast.makeText(mainActivity, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
//                     mainActivity.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
//                     return;
//                 }
//
//                 currentlyExecuting = new PostsFetcher(mainActivity.userQuery, PostItemType.HASHTAG, null, postsFetchListener)
//                         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//                 mainActivity.mainBinding.profileView.btnFollowTag.setVisibility(View.VISIBLE);
//
//                 if (isLoggedIn) {
//                     new iStoryStatusFetcher(hashtagModel.getName(), null, false, true, false, false, result -> {
//                         mainActivity.storyModels = result;
//                         if (result != null && result.length > 0)
//                             mainActivity.mainBinding.profileView.mainHashtagImage.setStoriesBorder();
//                     }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//                     if (hashtagModel.getFollowing()) {
//                         mainActivity.mainBinding.profileView.btnFollowTag.setText(R.string.unfollow);
//                         mainActivity.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_purple_background)));
//                     } else {
//                         mainActivity.mainBinding.profileView.btnFollowTag.setText(R.string.follow);
//                         mainActivity.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_pink_background)));
//                     }
//                 } else {
//                     if (Utils.dataBox.getFavorite(mainActivity.userQuery) != null) {
//                         mainActivity.mainBinding.profileView.btnFollowTag.setText(R.string.unfavorite_short);
//                         mainActivity.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_purple_background)));
//                     } else {
//                         mainActivity.mainBinding.profileView.btnFollowTag.setText(R.string.favorite_short);
//                         mainActivity.mainBinding.profileView.btnFollowTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_pink_background)));
//                     }
//                 }
//
//                 mainActivity.mainBinding.profileView.mainHashtagImage.setEnabled(false);
//                 // new MyTask().execute();
//                 mainActivity.mainBinding.profileView.mainHashtagImage.setImageURI(hashtagModel.getSdProfilePic());
//                 mainActivity.mainBinding.profileView.mainHashtagImage.setEnabled(true);
//
//                 final String postCount = String.valueOf(hashtagModel.getPostCount());
//
//                 SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
//                 span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
//                 span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
//                 mainActivity.mainBinding.profileView.mainTagPostCount.setText(span);
//                 mainActivity.mainBinding.profileView.mainTagPostCount.setVisibility(View.VISIBLE);
//             }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//         } else if (isUser) {
//             mainActivity.hashtagModel = null;
//             mainActivity.locationModel = null;
//             mainActivity.mainBinding.toolbar.toolbar.setTitle(mainActivity.userQuery);
//             mainActivity.mainBinding.profileView.infoContainer.setVisibility(View.VISIBLE);
//             mainActivity.mainBinding.profileView.btnFollowTag.setVisibility(View.GONE);
//
//             currentlyExecuting = new ProfileFetcher(mainActivity.userQuery.substring(1), profileModel -> {
//                 mainActivity.profileModel = profileModel;
//
//                 if (profileModel == null) {
//                     mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                     Toast.makeText(mainActivity, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
//                     mainActivity.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
//                     return;
//                 }
//
//                 mainActivity.mainBinding.profileView.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
//                 final String profileId = profileModel.getId();
//
//                 if (isLoggedIn || settingsHelper.getBoolean(Constants.STORIESIG)) {
//                     new iStoryStatusFetcher(profileId, profileModel.getUsername(), false, false,
//                                             (!isLoggedIn && settingsHelper.getBoolean(Constants.STORIESIG)), false,
//                                             result -> {
//                                                 mainActivity.storyModels = result;
//                                                 if (result != null && result.length > 0)
//                                                     mainActivity.mainBinding.profileView.mainProfileImage.setStoriesBorder();
//                                             }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//                     new HighlightsFetcher(profileId, (!isLoggedIn && settingsHelper.getBoolean(Constants.STORIESIG)), result -> {
//                         if (result != null && result.length > 0) {
//                             mainActivity.mainBinding.profileView.highlightsList.setVisibility(View.VISIBLE);
//                             mainActivity.highlightsAdapter.setData(result);
//                         } else
//                             mainActivity.mainBinding.profileView.highlightsList.setVisibility(View.GONE);
//                     }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                 }
//
//                 if (isLoggedIn) {
//                     final String myId = Utils.getUserIdFromCookie(cookie);
//                     if (!profileId.equals(myId)) {
//                         mainActivity.mainBinding.profileView.btnTagged.setVisibility(View.GONE);
//                         mainActivity.mainBinding.profileView.btnSaved.setVisibility(View.GONE);
//                         mainActivity.mainBinding.profileView.btnLiked.setVisibility(View.GONE);
//                         mainActivity.mainBinding.profileView.btnFollow.setVisibility(View.VISIBLE);
//                         if (profileModel.getFollowing() == true) {
//                             mainActivity.mainBinding.profileView.btnFollow.setText(R.string.unfollow);
//                             mainActivity.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                     mainActivity, R.color.btn_purple_background)));
//                         } else if (profileModel.getRequested() == true) {
//                             mainActivity.mainBinding.profileView.btnFollow.setText(R.string.cancel);
//                             mainActivity.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                     mainActivity, R.color.btn_purple_background)));
//                         } else {
//                             mainActivity.mainBinding.profileView.btnFollow.setText(R.string.follow);
//                             mainActivity.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                     mainActivity, R.color.btn_pink_background)));
//                         }
//                         mainActivity.mainBinding.profileView.btnRestrict.setVisibility(View.VISIBLE);
//                         if (profileModel.getRestricted() == true) {
//                             mainActivity.mainBinding.profileView.btnRestrict.setText(R.string.unrestrict);
//                             mainActivity.mainBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                     mainActivity, R.color.btn_green_background)));
//                         } else {
//                             mainActivity.mainBinding.profileView.btnRestrict.setText(R.string.restrict);
//                             mainActivity.mainBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                     mainActivity, R.color.btn_orange_background)));
//                         }
//                         if (profileModel.isReallyPrivate()) {
//                             mainActivity.mainBinding.profileView.btnBlock.setVisibility(View.VISIBLE);
//                             mainActivity.mainBinding.profileView.btnTagged.setVisibility(View.GONE);
//                             if (profileModel.getBlocked() == true) {
//                                 mainActivity.mainBinding.profileView.btnBlock.setText(R.string.unblock);
//                                 mainActivity.mainBinding.profileView.btnBlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                         mainActivity, R.color.btn_green_background)));
//                             } else {
//                                 mainActivity.mainBinding.profileView.btnBlock.setText(R.string.block);
//                                 mainActivity.mainBinding.profileView.btnBlock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                         mainActivity, R.color.btn_red_background)));
//                             }
//                         } else {
//                             mainActivity.mainBinding.profileView.btnBlock.setVisibility(View.GONE);
//                             mainActivity.mainBinding.profileView.btnSaved.setVisibility(View.VISIBLE);
//                             mainActivity.mainBinding.profileView.btnTagged.setVisibility(View.VISIBLE);
//                             if (profileModel.getBlocked() == true) {
//                                 mainActivity.mainBinding.profileView.btnSaved.setText(R.string.unblock);
//                                 mainActivity.mainBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                         mainActivity, R.color.btn_green_background)));
//                             } else {
//                                 mainActivity.mainBinding.profileView.btnSaved.setText(R.string.block);
//                                 mainActivity.mainBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                         mainActivity, R.color.btn_red_background)));
//                             }
//                         }
//                     } else {
//                         mainActivity.mainBinding.profileView.btnTagged.setVisibility(View.VISIBLE);
//                         mainActivity.mainBinding.profileView.btnSaved.setVisibility(View.VISIBLE);
//                         mainActivity.mainBinding.profileView.btnLiked.setVisibility(View.VISIBLE);
//                         mainActivity.mainBinding.profileView.btnSaved.setText(R.string.saved);
//                         mainActivity.mainBinding.profileView.btnSaved.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_orange_background)));
//                     }
//                 } else {
//                     if (Utils.dataBox.getFavorite(mainActivity.userQuery) != null) {
//                         mainActivity.mainBinding.profileView.btnFollow.setText(R.string.unfavorite_short);
//                         mainActivity.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_purple_background)));
//                     } else {
//                         mainActivity.mainBinding.profileView.btnFollow.setText(R.string.favorite_short);
//                         mainActivity.mainBinding.profileView.btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_pink_background)));
//                     }
//                     mainActivity.mainBinding.profileView.btnFollow.setVisibility(View.VISIBLE);
//                     if (!profileModel.isReallyPrivate()) {
//                         mainActivity.mainBinding.profileView.btnRestrict.setVisibility(View.VISIBLE);
//                         mainActivity.mainBinding.profileView.btnRestrict.setText(R.string.tagged);
//                         mainActivity.mainBinding.profileView.btnRestrict.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
//                                 mainActivity, R.color.btn_blue_background)));
//                     }
//                 }
//
//                 // mainActivity.mainBinding.profileView.mainProfileImage.setEnabled(false);
//                 // new MyTask().execute();
//                 mainActivity.mainBinding.profileView.mainProfileImage.setImageURI(profileModel.getSdProfilePic());
//                 // mainActivity.mainBinding.profileView.mainProfileImage.setEnabled(true);
//
//                 final long followersCount = profileModel.getFollowersCount();
//                 final long followingCount = profileModel.getFollowingCount();
//
//                 final String postCount = String.valueOf(profileModel.getPostCount());
//
//                 SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
//                 span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
//                 span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
//                 mainActivity.mainBinding.profileView.mainPostCount.setText(span);
//
//                 final String followersCountStr = String.valueOf(followersCount);
//                 final int followersCountStrLen = followersCountStr.length();
//                 span = new SpannableStringBuilder(resources.getString(R.string.main_posts_followers, followersCountStr));
//                 span.setSpan(new RelativeSizeSpan(1.2f), 0, followersCountStrLen, 0);
//                 span.setSpan(new StyleSpan(Typeface.BOLD), 0, followersCountStrLen, 0);
//                 mainActivity.mainBinding.profileView.mainFollowers.setText(span);
//
//                 final String followingCountStr = String.valueOf(followingCount);
//                 final int followingCountStrLen = followingCountStr.length();
//                 span = new SpannableStringBuilder(resources.getString(R.string.main_posts_following, followingCountStr));
//                 span.setSpan(new RelativeSizeSpan(1.2f), 0, followingCountStrLen, 0);
//                 span.setSpan(new StyleSpan(Typeface.BOLD), 0, followingCountStrLen, 0);
//                 mainActivity.mainBinding.profileView.mainFollowing.setText(span);
//
//                 mainActivity.mainBinding.profileView.mainFullName
//                         .setText(Utils.isEmpty(profileModel.getName()) ? profileModel.getUsername() : profileModel.getName());
//
//                 CharSequence biography = profileModel.getBiography();
//                 mainActivity.mainBinding.profileView.mainBiography.setCaptionIsExpandable(true);
//                 mainActivity.mainBinding.profileView.mainBiography.setCaptionIsExpanded(true);
//                 if (Utils.hasMentions(biography)) {
//                     biography = Utils.getMentionText(biography);
//                     mainActivity.mainBinding.profileView.mainBiography.setText(biography, TextView.BufferType.SPANNABLE);
//                     mainActivity.mainBinding.profileView.mainBiography.setMentionClickListener(mentionClickListener);
//                 } else {
//                     mainActivity.mainBinding.profileView.mainBiography.setText(biography);
//                     mainActivity.mainBinding.profileView.mainBiography.setMentionClickListener(null);
//                 }
//
//                 final String url = profileModel.getUrl();
//                 if (Utils.isEmpty(url)) {
//                     mainActivity.mainBinding.profileView.mainUrl.setVisibility(View.GONE);
//                 } else {
//                     mainActivity.mainBinding.profileView.mainUrl.setVisibility(View.VISIBLE);
//                     mainActivity.mainBinding.profileView.mainUrl.setText(Utils.getSpannableUrl(url));
//                 }
//
//                 mainActivity.mainBinding.profileView.mainFullName.setSelected(true);
//                 mainActivity.mainBinding.profileView.mainBiography.setEnabled(true);
//
//                 if (!profileModel.isReallyPrivate()) {
//                     mainActivity.mainBinding.profileView.mainFollowing.setClickable(true);
//                     mainActivity.mainBinding.profileView.mainFollowers.setClickable(true);
//
//                     if (isLoggedIn) {
//                         // final View.OnClickListener followClickListener = v -> mainActivity.startActivity(new Intent(mainActivity, FollowViewerFragment.class)
//                         //                                                                                          .putExtra(Constants.EXTRAS_FOLLOWERS,
//                         //                                                                                                    v == mainActivity.mainBinding.profileView.mainFollowers)
//                         //                                                                                          .putExtra(Constants.EXTRAS_NAME,
//                         //                                                                                                    profileModel.getUsername())
//                         //                                                                                          .putExtra(Constants.EXTRAS_ID,
//                         //                                                                                                    profileId));
//                         //
//                         // mainActivity.mainBinding.profileView.mainFollowers.setOnClickListener(followersCount > 0 ? followClickListener : null);
//                         // mainActivity.mainBinding.profileView.mainFollowing.setOnClickListener(followingCount > 0 ? followClickListener : null);
//                     }
//
//                     if (profileModel.getPostCount() == 0) {
//                         mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                         mainActivity.mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
//                         mainActivity.mainBinding.profileView.privatePage2.setText(R.string.empty_acc);
//                         mainActivity.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
//                     } else {
//                         mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(true);
//                         mainActivity.mainBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
//                         currentlyExecuting = new PostsFetcher(profileId, PostItemType.MAIN, null, postsFetchListener)
//                                 .setUsername(profileModel.getUsername())
//                                 .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                     }
//                 } else {
//                     mainActivity.mainBinding.profileView.mainFollowers.setClickable(false);
//                     mainActivity.mainBinding.profileView.mainFollowing.setClickable(false);
//                     mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                     // error
//                     mainActivity.mainBinding.profileView.privatePage1.setImageResource(R.drawable.lock);
//                     mainActivity.mainBinding.profileView.privatePage2.setText(R.string.priv_acc);
//                     mainActivity.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
//                     mainActivity.mainBinding.profileView.mainPosts.setVisibility(View.GONE);
//                 }
//             }
//             ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//         } else if (isLocation) {
//             mainActivity.profileModel = null;
//             mainActivity.hashtagModel = null;
//             mainActivity.mainBinding.toolbar.toolbar.setTitle(mainActivity.userQuery);
//             mainActivity.mainBinding.profileView.locInfoContainer.setVisibility(View.VISIBLE);
//
//             currentlyExecuting = new LocationFetcher(mainActivity.userQuery.split("/")[0], locationModel -> {
//                 mainActivity.locationModel = locationModel;
//
//                 if (locationModel == null) {
//                     mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                     Toast.makeText(mainActivity, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
//                     mainActivity.mainBinding.toolbar.toolbar.setTitle(R.string.app_name);
//                     return;
//                 }
//                 mainActivity.mainBinding.toolbar.toolbar.setTitle(locationModel.getName());
//
//                 final String profileId = locationModel.getId();
//
//                 if (isLoggedIn) {
//                     new iStoryStatusFetcher(profileId.split("/")[0], null, true, false, false, false, result -> {
//                         mainActivity.storyModels = result;
//                         if (result != null && result.length > 0)
//                             mainActivity.mainBinding.profileView.mainLocationImage.setStoriesBorder();
//                     }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                 }
//
//                 // mainActivity.mainBinding.profileView.mainLocationImage.setEnabled(false);
//                 // new MyTask().execute();
//                 mainActivity.mainBinding.profileView.mainLocationImage.setImageURI(locationModel.getSdProfilePic());
//                 // mainActivity.mainBinding.profileView.mainLocationImage.setEnabled(true);
//
//                 final String postCount = String.valueOf(locationModel.getPostCount());
//
//                 SpannableStringBuilder span = new SpannableStringBuilder(resources.getString(R.string.main_posts_count, postCount));
//                 span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
//                 span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
//                 mainActivity.mainBinding.profileView.mainLocPostCount.setText(span);
//
//                 mainActivity.mainBinding.profileView.locationFullName.setText(locationModel.getName());
//
//                 CharSequence biography = locationModel.getBio();
//                 mainActivity.mainBinding.profileView.locationBiography.setCaptionIsExpandable(true);
//                 mainActivity.mainBinding.profileView.locationBiography.setCaptionIsExpanded(true);
//
//                 if (Utils.isEmpty(biography)) {
//                     mainActivity.mainBinding.profileView.locationBiography.setVisibility(View.GONE);
//                 } else if (Utils.hasMentions(biography)) {
//                     mainActivity.mainBinding.profileView.locationBiography.setVisibility(View.VISIBLE);
//                     biography = Utils.getMentionText(biography);
//                     mainActivity.mainBinding.profileView.locationBiography.setText(biography, TextView.BufferType.SPANNABLE);
//                     mainActivity.mainBinding.profileView.locationBiography.setMentionClickListener(mentionClickListener);
//                 } else {
//                     mainActivity.mainBinding.profileView.locationBiography.setVisibility(View.VISIBLE);
//                     mainActivity.mainBinding.profileView.locationBiography.setText(biography);
//                     mainActivity.mainBinding.profileView.locationBiography.setMentionClickListener(null);
//                 }
//
//                 if (!locationModel.getGeo().startsWith("geo:0.0,0.0?z=17")) {
//                     mainActivity.mainBinding.profileView.btnMap.setVisibility(View.VISIBLE);
//                     mainActivity.mainBinding.profileView.btnMap.setOnClickListener(v -> {
//                         final Intent intent = new Intent(Intent.ACTION_VIEW);
//                         intent.setData(Uri.parse(locationModel.getGeo()));
//                         mainActivity.startActivity(intent);
//                     });
//                 } else {
//                     mainActivity.mainBinding.profileView.btnMap.setVisibility(View.GONE);
//                     mainActivity.mainBinding.profileView.btnMap.setOnClickListener(null);
//                 }
//
//                 final String url = locationModel.getUrl();
//                 if (Utils.isEmpty(url)) {
//                     mainActivity.mainBinding.profileView.locationUrl.setVisibility(View.GONE);
//                 } else if (!url.startsWith("http")) {
//                     mainActivity.mainBinding.profileView.locationUrl.setVisibility(View.VISIBLE);
//                     mainActivity.mainBinding.profileView.locationUrl.setText(Utils.getSpannableUrl("http://" + url));
//                 } else {
//                     mainActivity.mainBinding.profileView.locationUrl.setVisibility(View.VISIBLE);
//                     mainActivity.mainBinding.profileView.locationUrl.setText(Utils.getSpannableUrl(url));
//                 }
//
//                 mainActivity.mainBinding.profileView.locationFullName.setSelected(true);
//                 mainActivity.mainBinding.profileView.locationBiography.setEnabled(true);
//
//                 if (locationModel.getPostCount() == 0) {
//                     mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(false);
//                     mainActivity.mainBinding.profileView.privatePage1.setImageResource(R.drawable.ic_cancel);
//                     mainActivity.mainBinding.profileView.privatePage2.setText(R.string.empty_acc);
//                     mainActivity.mainBinding.profileView.privatePage.setVisibility(View.VISIBLE);
//                 } else {
//                     mainActivity.mainBinding.profileView.swipeRefreshLayout.setRefreshing(true);
//                     mainActivity.mainBinding.profileView.mainPosts.setVisibility(View.VISIBLE);
//                     currentlyExecuting = new PostsFetcher(profileId, PostItemType.LOCATION, null, postsFetchListener)
//                             .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                 }
//             }
//             ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//         }
//     }
//
//     public static void stopCurrentExecutor() {
//         if (currentlyExecuting != null) {
//             try {
//                 currentlyExecuting.cancel(true);
//             } catch (final Exception e) {
//                 if (logCollector != null)
//                     logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
//                 if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
//             }
//         }
//     }
//
//     private void toggleSelection(final PostModel postModel) {
//         if (postModel != null && postsAdapter != null && mainActivity.selectedItems.size() >= 100) {
//             Toast.makeText(mainActivity, R.string.downloader_too_many, Toast.LENGTH_SHORT);
//         } else if (postModel != null && postsAdapter != null) {
//             if (postModel.isSelected()) mainActivity.selectedItems.remove(postModel);
//             else if (mainActivity.selectedItems.size() >= 100) {
//                 Toast.makeText(mainActivity, R.string.downloader_too_many, Toast.LENGTH_SHORT);
//                 return;
//             } else mainActivity.selectedItems.add(postModel);
//             postModel.setSelected(!postModel.isSelected());
//             notifyAdapter(postModel);
//         }
//     }
//
//     private void notifyAdapter(final PostModel postModel) {
//         // if (mainActivity.selectedItems.size() < 1) postsAdapter.isSelecting = false;
//         // if (postModel.getPosition() < 0) postsAdapter.notifyDataSetChanged();
//         // else postsAdapter.notifyItemChanged(postModel.getPosition(), postModel);
//         //
//         // if (mainActivity.downloadAction != null)
//         //     mainActivity.downloadAction.setVisible(postsAdapter.isSelecting);
//     }
//
//     private void toggleDiscoverSelection(final DiscoverItemModel itemModel) {
//         if (itemModel != null && discoverAdapter != null) {
//             if (itemModel.isSelected()) mainActivity.selectedDiscoverItems.remove(itemModel);
//             else mainActivity.selectedDiscoverItems.add(itemModel);
//             itemModel.setSelected(!itemModel.isSelected());
//             notifyDiscoverAdapter(itemModel);
//         }
//     }
//
//     private void notifyDiscoverAdapter(final DiscoverItemModel itemModel) {
//         // if (mainActivity.selectedDiscoverItems.size() < 1) discoverAdapter.isSelecting = false;
//         // if (itemModel.getPosition() < 0) discoverAdapter.notifyDataSetChanged();
//         // else discoverAdapter.notifyItemChanged(itemModel.getPosition(), itemModel);
//         //
//         // if (mainActivity.downloadAction != null)
//         //     mainActivity.downloadAction.setVisible(discoverAdapter.isSelecting);
//     }
//
//     public boolean isSelectionCleared() {
//         // if (postsAdapter != null && postsAdapter.isSelecting()) {
//         //     for (final PostModel postModel : mainActivity.selectedItems)
//         //         postModel.setSelected(false);
//         //     mainActivity.selectedItems.clear();
//         //     // postsAdapter.isSelecting = false;
//         //     postsAdapter.notifyDataSetChanged();
//         //     if (mainActivity.downloadAction != null) mainActivity.downloadAction.setVisible(false);
//         //     return false;
//         // } else if (discoverAdapter != null && discoverAdapter.isSelecting) {
//         //     for (final DiscoverItemModel itemModel : mainActivity.selectedDiscoverItems)
//         //         itemModel.setSelected(false);
//         //     mainActivity.selectedDiscoverItems.clear();
//         //     discoverAdapter.isSelecting = false;
//         //     discoverAdapter.notifyDataSetChanged();
//         //     if (mainActivity.downloadAction != null) mainActivity.downloadAction.setVisible(false);
//         //     return false;
//         // }
//         // return true;
//         return false;
//     }
//
//     public void deselectSelection(final BasePostModel postModel) {
//         if (postModel instanceof PostModel) {
//             mainActivity.selectedItems.remove(postModel);
//             postModel.setSelected(false);
//             if (postsAdapter != null) notifyAdapter((PostModel) postModel);
//         } else if (postModel instanceof DiscoverItemModel) {
//             mainActivity.selectedDiscoverItems.remove(postModel);
//             postModel.setSelected(false);
//             if (discoverAdapter != null) notifyDiscoverAdapter((DiscoverItemModel) postModel);
//         }
//     }
//
//     public void onPause() {
//         if (videoAwareRecyclerScroller != null) {
//             videoAwareRecyclerScroller.stopPlaying();
//         }
//     }
//
//     public void onResume() {
//         if (videoAwareRecyclerScroller != null && SHOULD_AUTO_PLAY) {
//             videoAwareRecyclerScroller.startPlaying();
//         }
//     }
//
//     public static int indexOfIntArray(Object[] array, Object key) {
//         int returnvalue = -1;
//         for (int i = 0; i < array.length; ++i) {
//             if (key == array[i]) {
//                 returnvalue = i;
//                 break;
//             }
//         }
//         return returnvalue;
//     }
//
//     private final View.OnClickListener profileActionListener = new View.OnClickListener() {
//         @Override
//         public void onClick(final View v) {
//             final String userIdFromCookie = Utils.getUserIdFromCookie(MainHelper.this.cookie);
//             final boolean isSelf = (isLoggedIn && mainActivity.profileModel != null) && userIdFromCookie != null && userIdFromCookie
//                     .equals(mainActivity.profileModel.getId());
//             if (!isLoggedIn
//                     && Utils.dataBox.getFavorite(mainActivity.userQuery) != null
//                     && v == mainActivity.mainBinding.profileView.btnFollow) {
//                 Utils.dataBox.delFavorite(new DataBox.FavoriteModel(mainActivity.userQuery,
//                                                                     Long.parseLong(Utils.dataBox.getFavorite(mainActivity.userQuery).split("/")[1]),
//                                                                     mainActivity.locationModel != null
//                                                                     ? mainActivity.locationModel.getName()
//                                                                     : mainActivity.userQuery.replaceAll("^@", "")));
//                 onRefresh();
//             } else if (!isLoggedIn
//                     && (v == mainActivity.mainBinding.profileView.btnFollow || v == mainActivity.mainBinding.profileView.btnFollowTag)) {
//                 Utils.dataBox.addFavorite(new DataBox.FavoriteModel(mainActivity.userQuery, System.currentTimeMillis(),
//                                                                     mainActivity.locationModel != null
//                                                                     ? mainActivity.locationModel.getName()
//                                                                     : mainActivity.userQuery.replaceAll("^@", "")));
//                 onRefresh();
//             } else if (v == mainActivity.mainBinding.profileView.btnFollow) {
//                 if (mainActivity.profileModel.isPrivate() && mainActivity.profileModel.getFollowing()) {
//                     new AlertDialog.Builder(mainActivity)
//                             .setTitle(R.string.priv_acc)
//                             .setMessage(R.string.priv_acc_confirm)
//                             .setNegativeButton(R.string.no, null)
//                             .setPositiveButton(R.string.yes, (dialog, which) -> new ProfileAction().execute("follow"))
//                             .show();
//                 } else new ProfileAction().execute("follow");
//             } else if (v == mainActivity.mainBinding.profileView.btnRestrict && isLoggedIn) {
//                 new ProfileAction().execute("restrict");
//             } else if (v == mainActivity.mainBinding.profileView.btnSaved && !isSelf) {
//                 new ProfileAction().execute("block");
//             } else if (v == mainActivity.mainBinding.profileView.btnFollowTag) {
//                 new ProfileAction().execute("followtag");
//             } else if (v == mainActivity.mainBinding.profileView.btnTagged || v == mainActivity.mainBinding.profileView.btnRestrict) {
//                 mainActivity.startActivity(new Intent(mainActivity, SavedViewerFragment.class)
//                                                    .putExtra(Constants.EXTRAS_INDEX, "%" + mainActivity.profileModel.getId())
//                                                    .putExtra(Constants.EXTRAS_USER, "@" + mainActivity.profileModel.getUsername())
//                 );
//             } else if (v == mainActivity.mainBinding.profileView.btnSaved) {
//                 mainActivity.startActivity(new Intent(mainActivity, SavedViewerFragment.class)
//                                                    .putExtra(Constants.EXTRAS_INDEX, "$" + mainActivity.profileModel.getId())
//                                                    .putExtra(Constants.EXTRAS_USER, "@" + mainActivity.profileModel.getUsername())
//                 );
//             } else if (v == mainActivity.mainBinding.profileView.btnLiked) {
//                 mainActivity.startActivity(new Intent(mainActivity, SavedViewerFragment.class)
//                                                    .putExtra(Constants.EXTRAS_INDEX, "^" + mainActivity.profileModel.getId())
//                                                    .putExtra(Constants.EXTRAS_USER, "@" + mainActivity.profileModel.getUsername())
//                 );
//             }
//         }
//     };
//
//     class ProfileAction extends AsyncTask<String, Void, Void> {
//         boolean ok = false;
//         String action;
//
//         protected Void doInBackground(String... rawAction) {
//             action = rawAction[0];
//             final String url = "https://www.instagram.com/web/" + (action.equals("followtag") && mainActivity.hashtagModel != null
//                                                                    ? "tags/" + (mainActivity.hashtagModel.getFollowing()
//                                                                                 ? "unfollow/"
//                                                                                 : "follow/") + mainActivity.hashtagModel.getName() + "/"
//                                                                    : (action.equals("restrict") && mainActivity.profileModel != null
//                                                                       ? "restrict_action"
//                                                                       : "friendships/" + mainActivity.profileModel.getId()) + "/" + (action.equals(
//                                                                            "follow") ?
//                                                                                                                                      mainActivity.profileModel
//                                                                                                                                              .getFollowing() || mainActivity.profileModel
//                                                                                                                                              .getRequested()
//                                                                                                                                      ? "unfollow/"
//                                                                                                                                      : "follow/" :
//                                                                                                                                      action.equals(
//                                                                                                                                              "restrict")
//                                                                                                                                      ?
//                                                                                                                                      mainActivity.profileModel
//                                                                                                                                              .getRestricted()
//                                                                                                                                      ? "unrestrict/"
//                                                                                                                                      : "restrict/"
//                                                                                                                                      :
//                                                                                                                                      mainActivity.profileModel
//                                                                                                                                              .getBlocked()
//                                                                                                                                      ? "unblock/"
//                                                                                                                                      : "block/"));
//             try {
//                 final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
//                 urlConnection.setRequestMethod("POST");
//                 urlConnection.setUseCaches(false);
//                 urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
//                 urlConnection.setRequestProperty("x-csrftoken", cookie.split("csrftoken=")[1].split(";")[0]);
//                 if (action == "restrict") {
//                     final String urlParameters = "target_user_id=" + mainActivity.profileModel.getId();
//                     urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//                     urlConnection.setRequestProperty("Content-Length", "" +
//                             urlParameters.getBytes().length);
//                     urlConnection.setDoOutput(true);
//                     DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
//                     wr.writeBytes(urlParameters);
//                     wr.flush();
//                     wr.close();
//                 } else urlConnection.connect();
//                 if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                     ok = true;
//                 } else
//                     Toast.makeText(mainActivity, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
//                 urlConnection.disconnect();
//             } catch (Throwable ex) {
//                 Log.e("austin_debug", action + ": " + ex);
//             }
//             return null;
//         }
//
//         @Override
//         protected void onPostExecute(Void result) {
//             if (ok == true) {
//                 onRefresh();
//             }
//         }
//     }
// }
