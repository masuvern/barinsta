package awais.instagrabber.fragments.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.activities.CommentsViewer;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.activities.PostViewer;
import awais.instagrabber.adapters.FeedAdapter;
import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.adapters.viewholder.feed.FeedItemViewHolder;
import awais.instagrabber.asyncs.FeedFetcher;
import awais.instagrabber.customviews.RamboTextView;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.customviews.helpers.VideoAwareRecyclerScroller;
import awais.instagrabber.databinding.FragmentFeedBinding;
import awais.instagrabber.fragments.main.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.fragments.main.viewmodels.FeedViewModel;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.services.ServiceCallback;
import awais.instagrabber.services.StoriesService;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    private static final double MAX_VIDEO_HEIGHT = 0.9 * Utils.displayMetrics.heightPixels;
    private static final int RESIZED_VIDEO_HEIGHT = (int) (0.8 * Utils.displayMetrics.heightPixels);
    public static final boolean SHOULD_AUTO_PLAY = settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS);

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentFeedBinding binding;
    private StoriesService storiesService;
    private boolean feedHasNextPage = false;
    private String feedEndCursor = null;
    private FeedViewModel feedViewModel;
    private VideoAwareRecyclerScroller videoAwareRecyclerScroller;
    private boolean shouldRefresh = true;

    private final FetchListener<FeedModel[]> feedFetchListener = new FetchListener<FeedModel[]>() {
        @Override
        public void doBefore() {
            binding.feedSwipeRefreshLayout.post(() -> binding.feedSwipeRefreshLayout.setRefreshing(true));
        }

        @Override
        public void onResult(final FeedModel[] result) {
            if (result == null) return;
            final List<FeedModel> currentFeedModelList = feedViewModel.getList().getValue();
            final Map<String, FeedModel> thumbToFeedMap = new HashMap<>();
            for (final FeedModel feedModel : result) {
                thumbToFeedMap.put(feedModel.getThumbnailUrl(), feedModel);
            }
            final BaseDataSubscriber<Void> subscriber = new BaseDataSubscriber<Void>() {
                int success = 0;
                int failed = 0;

                @Override
                protected void onNewResultImpl(@NonNull final DataSource<Void> dataSource) {
                    final Map<String, Object> extras = dataSource.getExtras();
                    if (extras == null) return;
                    final Uri thumbUri = (Uri) extras.get("uri_source");
                    if (thumbUri == null) return;
                    final Integer encodedWidth = (Integer) extras.get("encoded_width");
                    final Integer encodedHeight = (Integer) extras.get("encoded_height");
                    if (encodedWidth == null || encodedHeight == null) return;
                    final FeedModel feedModel = thumbToFeedMap.get(thumbUri.toString());
                    if (feedModel == null) return;
                    int requiredWidth = Utils.displayMetrics.widthPixels;
                    int resultingHeight = Utils.getResultingHeight(requiredWidth, encodedHeight, encodedWidth);
                    if (feedModel.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO && resultingHeight >= MAX_VIDEO_HEIGHT) {
                        // If its a video and the height is too large, need to reduce the height,
                        // so that entire video fits on screen
                        resultingHeight = RESIZED_VIDEO_HEIGHT;
                        requiredWidth = Utils.getResultingWidth(RESIZED_VIDEO_HEIGHT, resultingHeight, requiredWidth);
                    }
                    feedModel.setImageWidth(requiredWidth);
                    feedModel.setImageHeight(resultingHeight);
                    success++;
                    updateAdapter();
                }

                @Override
                protected void onFailureImpl(@NonNull final DataSource<Void> dataSource) {
                    failed++;
                    updateAdapter();
                }

                public void updateAdapter() {
                    if (failed + success != result.length) return;
                    final List<FeedModel> finalList = currentFeedModelList == null || currentFeedModelList.isEmpty() ? new ArrayList<>() : new ArrayList<>(currentFeedModelList);
                    finalList.addAll(Arrays.asList(result));
                    feedViewModel.getList().postValue(finalList);
                    final PostModel feedPostModel = result[result.length - 1];
                    if (feedPostModel != null) {
                        feedEndCursor = feedPostModel.getEndCursor();
                        feedHasNextPage = feedPostModel.hasNextPage();
                        feedPostModel.setPageCursor(false, null);
                    }
                    binding.feedSwipeRefreshLayout.setRefreshing(false);
                }
            };

            for (final FeedModel feedModel : result) {
                final DataSource<Void> ds = Fresco.getImagePipeline().prefetchToBitmapCache(ImageRequest.fromUri(feedModel.getThumbnailUrl()), null);
                ds.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
            }
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        storiesService = StoriesService.getInstance();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        // setupActionBar();
        setupFeedStories();
        setupFeed();
        shouldRefresh = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoAwareRecyclerScroller != null) {
            videoAwareRecyclerScroller.stopPlaying();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoAwareRecyclerScroller != null && SHOULD_AUTO_PLAY) {
            videoAwareRecyclerScroller.startPlaying();
        }
    }

    final MentionClickListener mentionClickListener = (view, text, isHashtag) -> {
        // final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
        //         .setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
        //         .setTitle(text)
        //         .setNegativeButton(R.string.cancel, null)
        //         .setPositiveButton(R.string.ok, (dialog, which) -> {
        //             // if (MainActivityBackup.scanHack != null) {
        //             //     mainActivity.mainBinding.drawerLayout.closeDrawers();
        //             //     MainActivityBackup.scanHack.onResult(text);
        //             // }
        //         });
        // builder.show();
        if (isHashtag) {
            final NavDirections action = FeedFragmentDirections.actionFeedFragmentToHashTagFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        final NavDirections action = FeedFragmentDirections.actionFeedFragmentToProfileFragment("@" + text);
        NavHostFragment.findNavController(this).navigate(action);
    };

    private final View.OnClickListener postViewClickListener = v -> {
        final Object tag = v.getTag();
        if (!(tag instanceof FeedModel)) return;

        final FeedModel feedModel = (FeedModel) tag;
        if (v instanceof RamboTextView) {
            if (feedModel.isMentionClicked())
                feedModel.toggleCaption();
            feedModel.setMentionClicked(false);
            if (!FeedItemViewHolder.expandCollapseTextView((RamboTextView) v, feedModel.getPostCaption()))
                feedModel.toggleCaption();
            return;
        }

        final int id = v.getId();
        switch (id) {
            case R.id.btnComments:
                startActivity(new Intent(requireContext(), CommentsViewer.class)
                        .putExtra(Constants.EXTRAS_SHORTCODE, feedModel.getShortCode())
                        .putExtra(Constants.EXTRAS_POST, feedModel.getPostId())
                        .putExtra(Constants.EXTRAS_USER, feedModel.getProfileModel().getId()));
                break;

            case R.id.viewStoryPost:
                startActivity(new Intent(requireContext(), PostViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, feedModel.getPosition())
                        .putExtra(Constants.EXTRAS_POST, new PostModel(feedModel.getShortCode(), false))
                        .putExtra(Constants.EXTRAS_TYPE, ItemGetType.FEED_ITEMS));
                break;

            case R.id.btnDownload:
                ProfileModel profileModel = feedModel.getProfileModel();
                final String username = profileModel != null ? profileModel.getUsername() : null;

                final ViewerPostModel[] sliderItems = feedModel.getSliderItems();

                if (feedModel.getItemType() != MediaItemType.MEDIA_TYPE_SLIDER || sliderItems == null || sliderItems.length == 1)
                    Utils.batchDownload(requireContext(), username, DownloadMethod.DOWNLOAD_FEED, Collections.singletonList(feedModel));
                else {
                    final ArrayList<BasePostModel> postModels = new ArrayList<>();
                    final DialogInterface.OnClickListener clickListener1 = (dialog, which) -> {
                        postModels.clear();

                        final boolean breakWhenFoundSelected = which == DialogInterface.BUTTON_POSITIVE;

                        for (final ViewerPostModel sliderItem : sliderItems) {
                            if (sliderItem != null) {
                                if (!breakWhenFoundSelected)
                                    postModels.add(sliderItem);
                                else if (sliderItem.isSelected()) {
                                    postModels.add(sliderItem);
                                    break;
                                }
                            }
                        }

                        // shows 0 items on first item of viewpager cause onPageSelected hasn't been called yet
                        if (breakWhenFoundSelected && postModels.size() == 0)
                            postModels.add(sliderItems[0]);

                        if (postModels.size() > 0)
                            Utils.batchDownload(requireContext(), username, DownloadMethod.DOWNLOAD_FEED, postModels);
                    };

                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.post_viewer_download_dialog_title)
                            .setPositiveButton(R.string.post_viewer_download_current, clickListener1)
                            .setNegativeButton(R.string.post_viewer_download_album, clickListener1)
                            .show();
                }
                break;

            case R.id.ivProfilePic:
                profileModel = feedModel.getProfileModel();
                if (profileModel != null)
                    mentionClickListener.onClick(null, profileModel.getUsername(), false);
                break;
        }
    };

    private void setupFeed() {
        feedViewModel = new ViewModelProvider(fragmentActivity).get(FeedViewModel.class);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.feedRecyclerView.setLayoutManager(layoutManager);
        binding.feedRecyclerView.setHasFixedSize(true);
        final FeedAdapter feedAdapter = new FeedAdapter(postViewClickListener, mentionClickListener);
        feedAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        binding.feedRecyclerView.setAdapter(feedAdapter);
        feedViewModel.getList().observe(fragmentActivity, feedAdapter::submitList);
        final RecyclerLazyLoader lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (feedHasNextPage) {
                fetchFeed();
            }
        });
        if (SHOULD_AUTO_PLAY) {
            videoAwareRecyclerScroller = new VideoAwareRecyclerScroller();
            binding.feedRecyclerView.addOnScrollListener(videoAwareRecyclerScroller);
        }
        binding.feedRecyclerView.addOnScrollListener(lazyLoader);
        fetchFeed();
    }

    private void fetchFeed() {
        binding.feedSwipeRefreshLayout.setRefreshing(true);
        new FeedFetcher(feedEndCursor, feedFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        feedEndCursor = null;
    }

    private void setupFeedStories() {
        final FeedStoriesViewModel feedStoriesViewModel = new ViewModelProvider(fragmentActivity).get(FeedStoriesViewModel.class);
        final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter((model, position) -> {
            final NavDirections action = FeedFragmentDirections.actionFeedFragmentToStoryViewerFragment(
                    position,
                    null,
                    false);
            NavHostFragment.findNavController(this).navigate(action);
        });
        binding.feedStoriesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.feedStoriesRecyclerView.setAdapter(feedStoriesAdapter);
        feedStoriesViewModel.getList().observe(fragmentActivity, feedStoriesAdapter::submitList);
        storiesService.getFeedStories(new ServiceCallback<List<FeedStoryModel>>() {
            @Override
            public void onSuccess(final List<FeedStoryModel> result) {
                feedStoriesViewModel.getList().postValue(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "failed", t);
            }
        });
    }
}
