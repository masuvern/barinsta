package awais.instagrabber.fragments.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.DiscoverTopicsAdapter;
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
    private TopicClusterViewModel topicClusterViewModel;
    private boolean shouldRefresh = true;
    private DiscoverService discoverService;

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
            final DiscoverFragmentDirections.ActionDiscoverFragmentToTopicPostsFragment action = DiscoverFragmentDirections
                    .actionDiscoverFragmentToTopicPostsFragment(topicCluster, titleColor, backgroundColor);
            NavHostFragment.findNavController(this).navigate(action, builder.build());
        });
        binding.topicsRecyclerView.setAdapter(adapter);
        topicClusterViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    private void fetchTopics() {
        binding.swipeRefreshLayout.setRefreshing(true);
        discoverService.topicalExplore(new DiscoverService.TopicalExploreRequest(), new ServiceCallback<DiscoverService.TopicalExploreResponse>() {
            @Override
            public void onSuccess(final DiscoverService.TopicalExploreResponse result) {
                if (result == null) return;
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
}
