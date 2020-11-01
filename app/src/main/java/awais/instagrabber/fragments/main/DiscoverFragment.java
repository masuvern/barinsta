package awais.instagrabber.fragments.main;

import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.DiscoverTopicsAdapter;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.databinding.FragmentDiscoverBinding;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.TopicClusterViewModel;
import awais.instagrabber.webservices.DiscoverService;
import awais.instagrabber.webservices.ServiceCallback;

public class DiscoverFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "DiscoverFragment";

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentDiscoverBinding binding;
    private ActionMode actionMode;
    private TopicClusterViewModel topicClusterViewModel;
    private boolean shouldRefresh = true;
    private DiscoverService discoverService;

    // private final FetchListener<DiscoverItemModel[]> postsFetchListener = new FetchListener<DiscoverItemModel[]>() {
    //     @Override
    //     public void doBefore() {}
    //
    //     @Override
    //     public void onResult(final DiscoverItemModel[] result) {
    //         if (result == null || result.length <= 0) {
    //             binding.swipeRefreshLayout.setRefreshing(false);
    //             final Context context = getContext();
    //             if (context == null) return;
    //             Toast.makeText(context, R.string.discover_empty, Toast.LENGTH_SHORT).show();
    //             return;
    //         }
    //         List<DiscoverItemModel> current = discoverItemViewModel.getList().getValue();
    //         final List<DiscoverItemModel> resultList = Arrays.asList(result);
    //         current = current == null ? new ArrayList<>() : new ArrayList<>(current); // copy to modifiable list
    //         if (isPullToRefresh) {
    //             current = resultList;
    //             isPullToRefresh = false;
    //         } else {
    //             current.addAll(resultList);
    //         }
    //         discoverItemViewModel.getList().postValue(current);
    //         binding.swipeRefreshLayout.setRefreshing(false);
    //         final DiscoverItemModel discoverItemModel = result[result.length - 1];
    //         if (discoverItemModel != null) {
    //             discoverEndMaxId = discoverItemModel.getNextMaxId();
    //             discoverHasMore = discoverItemModel.hasMore();
    //             discoverItemModel.setMore(false, null);
    //         }
    //     }
    // };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            remove();
            // if (discoverAdapter == null) return;
            // discoverAdapter.clearSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(final ActionMode mode) {
                    onBackPressedCallback.handleOnBackPressed();
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        // if (discoverAdapter == null) return false;
                        // final Context context = getContext();
                        // if (context == null) return false;
                        // DownloadUtils.batchDownload(context,
                        //                             null,
                        //                             DownloadMethod.DOWNLOAD_DISCOVER,
                        //                             discoverAdapter.getSelectedModels());
                        // checkAndResetAction();
                        return true;
                    }
                    return false;
                }
            });

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        discoverService = DiscoverService.getInstance();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentDiscoverBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        init();
        shouldRefresh = false;
    }

    private void init() {
        // setExitSharedElementCallback(new SharedElementCallback() {
        //     @Override
        //     public void onSharedElementsArrived(final List<String> sharedElementNames,
        //                                         final List<View> sharedElements,
        //                                         final OnSharedElementsReadyListener listener) {
        //         super.onSharedElementsArrived(sharedElementNames, sharedElements, listener);
        //         Log.d(TAG, "onSharedElementsArrived: sharedElementNames: " + sharedElementNames);
        //     }
        //
        //     @Override
        //     public void onSharedElementEnd(final List<String> sharedElementNames,
        //                                    final List<View> sharedElements,
        //                                    final List<View> sharedElementSnapshots) {
        //         super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
        //         Log.d(TAG, "onSharedElementEnd: sharedElementNames: " + sharedElementNames);
        //     }
        //
        //     @Override
        //     public void onSharedElementStart(final List<String> sharedElementNames,
        //                                      final List<View> sharedElements,
        //                                      final List<View> sharedElementSnapshots) {
        //         super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
        //         Log.d(TAG, "onSharedElementStart: sharedElementNames: " + sharedElementNames);
        //     }
        // });
        setupTopics();
        fetchTopics();
    }

    @Override
    public void onRefresh() {
        fetchTopics();
    }

    public void setupTopics() {
        topicClusterViewModel = new ViewModelProvider(fragmentActivity).get(TopicClusterViewModel.class);
        binding.topicsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(2)));
        final DiscoverTopicsAdapter adapter = new DiscoverTopicsAdapter((topicCluster, root, cover, title, titleColor, backgroundColor) -> {
            final FragmentNavigator.Extras.Builder builder = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(cover, "cover-" + topicCluster.getId());
            // .addSharedElement(title, "title-" + topicCluster.getId());
            final DiscoverFragmentDirections.ActionDiscoverFragmentToTopicPostsFragment action = DiscoverFragmentDirections
                    .actionDiscoverFragmentToTopicPostsFragment(topicCluster, titleColor, backgroundColor);
            NavHostFragment.findNavController(this).navigate(action, builder.build());
        });
        binding.topicsRecyclerView.setAdapter(adapter);
        topicClusterViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    private void setupExplore() {
        // discoverItemViewModel = new ViewModelProvider(fragmentActivity).get(DiscoverItemViewModel.class);
        // final Context context = getContext();
        // if (context == null) return;
        // final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(context, Utils.convertDpToPx(110));
        // binding.postsRecyclerView.setLayoutManager(layoutManager);
        // binding.postsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        // binding.discoverType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        //     @Override
        //     public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        //         if (topicIds == null || topicIds.length <= 0) return;
        //         currentTopic = topicIds[pos];
        //         onRefresh();
        //     }
        //
        //     @Override
        //     public void onNothingSelected(AdapterView<?> parent) {}
        // });
        // discoverAdapter = new DiscoverAdapter((model, position) -> {
        //     if (discoverAdapter.isSelecting()) {
        //         if (actionMode == null) return;
        //         final String title = getString(R.string.number_selected, discoverAdapter.getSelectedModels().size());
        //         actionMode.setTitle(title);
        //         return;
        //     }
        //     if (checkAndResetAction()) return;
        //     final List<DiscoverItemModel> discoverItemModels = discoverItemViewModel.getList().getValue();
        //     if (discoverItemModels == null || discoverItemModels.size() == 0) return;
        //     if (discoverItemModels.get(0) == null) return;
        //     final String postId = discoverItemModels.get(0).getPostId();
        //     final boolean isId = postId != null;
        //     final String[] idsOrShortCodes = new String[discoverItemModels.size()];
        //     for (int i = 0; i < discoverItemModels.size(); i++) {
        //         idsOrShortCodes[i] = isId ? discoverItemModels.get(i).getPostId()
        //                                   : discoverItemModels.get(i).getShortCode();
        //     }
        //     final NavDirections action = DiscoverFragmentDirections.actionGlobalPostViewFragment(
        //             position,
        //             idsOrShortCodes,
        //             isId);
        //     NavHostFragment.findNavController(this).navigate(action);
        // }, (model, position) -> {
        //     if (!discoverAdapter.isSelecting()) {
        //         checkAndResetAction();
        //         return true;
        //     }
        //     final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
        //     if (onBackPressedCallback.isEnabled()) {
        //         return true;
        //     }
        //     actionMode = fragmentActivity.startActionMode(multiSelectAction);
        //     final String title = getString(R.string.number_selected, 1);
        //     actionMode.setTitle(title);
        //     onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        //     return true;
        // });
        // binding.postsRecyclerView.setAdapter(discoverAdapter);
        // discoverItemViewModel.getList().observe(fragmentActivity, discoverAdapter::submitList);
        // lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
        //     if (discoverHasMore) {
        //         fetchPosts();
        //     }
        // }, 3);
        // binding.postsRecyclerView.addOnScrollListener(lazyLoader);
        // binding.postsRecyclerView.setViewModelStoreOwner(this)
        //                          .setLifeCycleOwner(this)
        //                          .setPostFetchService(new DiscoverPostFetchService())
        //                          .setLayoutPreferences(PostsLayoutPreferences.fromJson(settingsHelper.getString(Constants.PREF_PROFILE_POSTS_LAYOUT)))
        //                          .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
        //                          .setFeedItemCallback(feedItemCallback)
        //                          .init();
        // binding.swipeRefreshLayout.setRefreshing(true);
    }

    private void fetchTopics() {
        // new iTopicFetcher(topicFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        binding.swipeRefreshLayout.setRefreshing(true);
        discoverService.topicalExplore(new DiscoverService.TopicalExploreRequest(), new ServiceCallback<DiscoverService.TopicalExploreResponse>() {
            @Override
            public void onSuccess(final DiscoverService.TopicalExploreResponse result) {
                topicClusterViewModel.getList().postValue(result.getClusters());
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure", t);
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    // private void fetchPosts() {
    // binding.swipeRefreshLayout.setRefreshing(true);
    // new DiscoverFetcher(currentTopic, discoverEndMaxId, rankToken, postsFetchListener, false)
    //         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    // }

    private boolean checkAndResetAction() {
        if (!onBackPressedCallback.isEnabled() && actionMode == null) {
            return false;
        }
        if (onBackPressedCallback.isEnabled()) {
            onBackPressedCallback.setEnabled(false);
            onBackPressedCallback.remove();
        }
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        return true;
    }
}
