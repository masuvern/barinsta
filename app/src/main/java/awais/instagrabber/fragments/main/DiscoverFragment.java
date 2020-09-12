package awais.instagrabber.fragments.main;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.DiscoverAdapter;
import awais.instagrabber.asyncs.DiscoverFetcher;
import awais.instagrabber.asyncs.i.iTopicFetcher;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentDiscoverBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.DiscoverItemModel;
import awais.instagrabber.models.DiscoverTopicModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.DiscoverItemViewModel;

public class DiscoverFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentDiscoverBinding binding;
    private DiscoverAdapter discoverAdapter;
    private RecyclerLazyLoader lazyLoader;
    private boolean discoverHasMore = false;
    private String[] topicIds;
    private String rankToken;
    private String currentTopic;
    private String discoverEndMaxId;
    private ActionMode actionMode;
    private DiscoverItemViewModel discoverItemViewModel;
    private boolean shouldRefresh = true;
    private boolean isPullToRefresh;

    private final FetchListener<DiscoverTopicModel> topicFetchListener = new FetchListener<DiscoverTopicModel>() {
        @Override
        public void doBefore() {}

        @Override
        public void onResult(final DiscoverTopicModel result) {
            if (result != null) {
                topicIds = result.getIds();
                rankToken = result.getToken();
                final Context context = getContext();
                if (context == null) return;
                final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        result.getNames()
                );
                binding.discoverType.setAdapter(spinnerArrayAdapter);
            }
        }
    };
    private final FetchListener<DiscoverItemModel[]> postsFetchListener = new FetchListener<DiscoverItemModel[]>() {
        @Override
        public void doBefore() {}

        @Override
        public void onResult(final DiscoverItemModel[] result) {
            if (result == null || result.length <= 0) {
                binding.discoverSwipeRefreshLayout.setRefreshing(false);
                final Context context = getContext();
                if (context == null) return;
                Toast.makeText(context, R.string.discover_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            List<DiscoverItemModel> current = discoverItemViewModel.getList().getValue();
            final List<DiscoverItemModel> resultList = Arrays.asList(result);
            current = current == null ? new ArrayList<>() : new ArrayList<>(current); // copy to modifiable list
            if (isPullToRefresh) {
                current = resultList;
                isPullToRefresh = false;
            } else {
                current.addAll(resultList);
            }
            discoverItemViewModel.getList().postValue(current);
            binding.discoverSwipeRefreshLayout.setRefreshing(false);
            final DiscoverItemModel discoverItemModel = result[result.length - 1];
            if (discoverItemModel != null) {
                discoverEndMaxId = discoverItemModel.getNextMaxId();
                discoverHasMore = discoverItemModel.hasMore();
                discoverItemModel.setMore(false, null);
            }
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            remove();
            if (discoverAdapter == null) return;
            discoverAdapter.clearSelection();
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
                        if (discoverAdapter == null) return false;
                        final Context context = getContext();
                        if (context == null) return false;
                        DownloadUtils.batchDownload(context,
                                                    null,
                                                    DownloadMethod.DOWNLOAD_DISCOVER,
                                                    discoverAdapter.getSelectedModels());
                        checkAndResetAction();
                        return true;
                    }
                    return false;
                }
            });

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
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
        binding.discoverSwipeRefreshLayout.setOnRefreshListener(this);
        setupExplore();
        shouldRefresh = false;
    }

    @Override
    public void onRefresh() {
        isPullToRefresh = true;
        discoverEndMaxId = null;
        lazyLoader.resetState();
        fetchPosts();
    }

    private void setupExplore() {
        discoverItemViewModel = new ViewModelProvider(fragmentActivity).get(DiscoverItemViewModel.class);
        final Context context = getContext();
        if (context == null) return;
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(context, Utils.convertDpToPx(110));
        binding.discoverPosts.setLayoutManager(layoutManager);
        binding.discoverPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        binding.discoverType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (topicIds == null || topicIds.length <= 0) return;
                currentTopic = topicIds[pos];
                onRefresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        discoverAdapter = new DiscoverAdapter((model, position) -> {
            if (discoverAdapter.isSelecting()) {
                if (actionMode == null) return;
                final String title = getString(R.string.number_selected, discoverAdapter.getSelectedModels().size());
                actionMode.setTitle(title);
                return;
            }
            if (checkAndResetAction()) return;
            final List<DiscoverItemModel> discoverItemModels = discoverItemViewModel.getList().getValue();
            if (discoverItemModels == null || discoverItemModels.size() == 0) return;
            if (discoverItemModels.get(0) == null) return;
            final String postId = discoverItemModels.get(0).getPostId();
            final boolean isId = postId != null;
            final String[] idsOrShortCodes = new String[discoverItemModels.size()];
            for (int i = 0; i < discoverItemModels.size(); i++) {
                idsOrShortCodes[i] = isId ? discoverItemModels.get(i).getPostId()
                                          : discoverItemModels.get(i).getShortCode();
            }
            final NavDirections action = DiscoverFragmentDirections.actionGlobalPostViewFragment(
                    position,
                    idsOrShortCodes,
                    isId);
            NavHostFragment.findNavController(this).navigate(action);
        }, (model, position) -> {
            if (!discoverAdapter.isSelecting()) {
                checkAndResetAction();
                return true;
            }
            final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
            if (onBackPressedCallback.isEnabled()) {
                return true;
            }
            actionMode = fragmentActivity.startActionMode(multiSelectAction);
            final String title = getString(R.string.number_selected, 1);
            actionMode.setTitle(title);
            onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
            return true;
        });
        binding.discoverPosts.setAdapter(discoverAdapter);
        discoverItemViewModel.getList().observe(fragmentActivity, discoverAdapter::submitList);
        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (discoverHasMore) {
                fetchPosts();
            }
        }, 3);
        binding.discoverPosts.addOnScrollListener(lazyLoader);
        fetchTopics();
    }

    private void fetchTopics() {
        new iTopicFetcher(topicFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fetchPosts() {
        binding.discoverSwipeRefreshLayout.setRefreshing(true);
        new DiscoverFetcher(currentTopic, discoverEndMaxId, rankToken, postsFetchListener, false)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

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
