package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import awais.instagrabber.adapters.FeedStoriesListAdapter;
import awais.instagrabber.adapters.FeedStoriesListAdapter.OnFeedStoryClickListener;
import awais.instagrabber.databinding.FragmentStoryListViewerBinding;
import awais.instagrabber.fragments.settings.MorePreferencesFragmentDirections;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.viewmodels.StoriesViewModel;
import awais.instagrabber.webservices.StoriesService;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class StoryListViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "StoryListViewerFragment";

    private FragmentStoryListViewerBinding binding;
    private SwipeRefreshLayout root;
    private boolean shouldRefresh = true;
    private StoriesViewModel storiesViewModel;
    private StoriesService storiesService;
    private Context context;
    private String type;

    private final OnFeedStoryClickListener clickListener = new OnFeedStoryClickListener() {
        @Override
        public void onFeedStoryClick(final FeedStoryModel model) {
            if (model == null) return;
//            final NavDirections action = StoryListNavGraphDirections.actionStoryListFragmentToStoryViewerFragment(position, null, false, false, null, null);
//            NavHostFragment.findNavController(StoryListViewerFragment.this).navigate(action);
        }

        @Override
        public void onProfileClick(final String username) {
            openProfile(username);
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        if (context == null) return;
        storiesService = StoriesService.getInstance();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentStoryListViewerBinding.inflate(getLayoutInflater());
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    private void init() {
        final Context context = getContext();
        if (getArguments() == null) return;
        final StoryListViewerFragmentArgs fragmentArgs = StoryListViewerFragmentArgs.fromBundle(getArguments());
        type = fragmentArgs.getType();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        storiesViewModel = new ViewModelProvider(this).get(StoriesViewModel.class);
//        final NotificationsAdapter adapter = new NotificationsAdapter(clickListener, mentionClickListener);
        binding.rvStories.setLayoutManager(new LinearLayoutManager(context));
//        binding.rvStories.setAdapter(adapter);
//        storiesViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        binding.swipeRefreshLayout.setRefreshing(true);

            binding.swipeRefreshLayout.setRefreshing(false);
//            storiesViewModel.getList().postValue();
    }

    private void openProfile(final String username) {
        final NavDirections action = MorePreferencesFragmentDirections
                .actionGlobalProfileFragment("@" + username);
        NavHostFragment.findNavController(this).navigate(action);
    }
}