package awais.instagrabber.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.activities.PostViewer;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.HashtagFetcher;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.NestedCoordinatorLayout;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentHashtagBinding;
import awais.instagrabber.fragments.main.viewmodels.PostsViewModel;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.HashtagModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class HashTagFragment extends Fragment {
    private static final String TAG = "HashTagFragment";

    private MainActivity fragmentActivity;
    private FragmentHashtagBinding binding;
    private NestedCoordinatorLayout root;
    private boolean shouldRefresh = true;
    private String hashtag;
    private HashtagModel hashtagModel;
    private PostsViewModel postsViewModel;
    private PostsAdapter postsAdapter;
    private ActionMode actionMode;
    private boolean hasNextPage;
    private String endCursor;
    private AsyncTask<?, ?, ?> currentlyExecuting;
    private boolean isLoggedIn;
    private StoryModel[] storyModels;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (postsAdapter == null) {
                setEnabled(false);
                remove();
                return;
            }
            postsAdapter.clearSelection();
            setEnabled(false);
            remove();
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
                        if (postsAdapter == null || hashtag == null) {
                            return false;
                        }
                        Utils.batchDownload(requireContext(),
                                hashtag,
                                DownloadMethod.DOWNLOAD_MAIN,
                                postsAdapter.getSelectedModels());
                        checkAndResetAction();
                        return true;
                    }
                    return false;
                }
            });
    private final FetchListener<PostModel[]> postsFetchListener = new FetchListener<PostModel[]>() {
        @Override
        public void onResult(final PostModel[] result) {
            binding.swipeRefreshLayout.setRefreshing(false);
            if (result == null) return;
            binding.mainPosts.post(() -> binding.mainPosts.setVisibility(View.VISIBLE));
            final List<PostModel> postModels = postsViewModel.getList().getValue();
            final List<PostModel> finalList = postModels == null || postModels.isEmpty() ? new ArrayList<>() : new ArrayList<>(postModels);
            finalList.addAll(Arrays.asList(result));
            postsViewModel.getList().postValue(finalList);
            PostModel model = null;
            if (result.length != 0) {
                model = result[result.length - 1];
            }
            if (model == null) return;
            endCursor = model.getEndCursor();
            hasNextPage = model.hasNextPage();
            model.setPageCursor(false, null);
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentHashtagBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (postsViewModel != null) {
            postsViewModel.getList().postValue(Collections.emptyList());
        }
    }

    private void init() {
        if (getArguments() == null) return;
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !Utils.isEmpty(cookie) && Utils.getUserIdFromCookie(cookie) != null;
        final HashTagFragmentArgs fragmentArgs = HashTagFragmentArgs.fromBundle(getArguments());
        hashtag = fragmentArgs.getHashtag();
        setTitle();
        setupPosts();
        fetchHashtagModel();
    }

    private void setupPosts() {
        postsViewModel = new ViewModelProvider(this).get(PostsViewModel.class);
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(requireContext(), Utils.convertDpToPx(110));
        binding.mainPosts.setLayoutManager(layoutManager);
        binding.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        postsAdapter = new PostsAdapter((postModel, position) -> {
            if (postsAdapter.isSelecting()) {
                if (actionMode == null) return;
                final String title = getString(R.string.number_selected, postsAdapter.getSelectedModels().size());
                actionMode.setTitle(title);
                return;
            }
            if (checkAndResetAction()) return;
            startActivity(new Intent(requireContext(), PostViewer.class)
                    .putExtra(Constants.EXTRAS_INDEX, position)
                    .putExtra(Constants.EXTRAS_POST, postModel)
                    .putExtra(Constants.EXTRAS_USER, hashtag)
                    .putExtra(Constants.EXTRAS_TYPE, ItemGetType.MAIN_ITEMS));

        }, (model, position) -> {
            if (!postsAdapter.isSelecting()) {
                checkAndResetAction();
                return true;
            }
            if (onBackPressedCallback.isEnabled()) {
                return true;
            }
            final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
            onBackPressedCallback.setEnabled(true);
            actionMode = fragmentActivity.startActionMode(multiSelectAction);
            final String title = getString(R.string.number_selected, 1);
            actionMode.setTitle(title);
            onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
            return true;
        });
        postsViewModel.getList().observe(fragmentActivity, postsAdapter::submitList);
        binding.mainPosts.setAdapter(postsAdapter);
        final RecyclerLazyLoader lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!hasNextPage) return;
            binding.swipeRefreshLayout.setRefreshing(true);
            fetchPosts();
            endCursor = null;
        });
        binding.mainPosts.addOnScrollListener(lazyLoader);
    }

    private void fetchHashtagModel() {
        stopCurrentExecutor();
        binding.swipeRefreshLayout.setRefreshing(true);
        currentlyExecuting = new HashtagFetcher(hashtag.substring(1), result -> {
            hashtagModel = result;
            binding.swipeRefreshLayout.setRefreshing(false);
            if (hashtagModel == null) {
                Toast.makeText(requireContext(), R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            fetchPosts();
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fetchPosts() {
        stopCurrentExecutor();
        binding.btnFollowTag.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setRefreshing(true);
        currentlyExecuting = new PostsFetcher(hashtag, endCursor, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if (isLoggedIn) {
            new iStoryStatusFetcher(hashtagModel.getName(), null, false, true, false, false, stories -> {
                storyModels = stories;
                if (stories != null && stories.length > 0) {
                    binding.mainHashtagImage.setStoriesBorder();
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            binding.btnFollowTag.setText(hashtagModel.getFollowing() ? R.string.unfollow : R.string.follow);
            ViewCompat.setBackgroundTintList(binding.btnFollowTag, ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), hashtagModel.getFollowing()
                            ? R.color.btn_purple_background
                            : R.color.btn_pink_background)));
        } else {
            binding.btnFollowTag.setText(Utils.dataBox.getFavorite(hashtag) != null
                    ? R.string.unfavorite_short
                    : R.string.favorite_short);
            ViewCompat.setBackgroundTintList(binding.btnFollowTag, ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), Utils.dataBox.getFavorite(hashtag) != null
                            ? R.color.btn_purple_background
                            : R.color.btn_pink_background)));
        }
        binding.mainHashtagImage.setImageURI(hashtagModel.getSdProfilePic());
        final String postCount = String.valueOf(hashtagModel.getPostCount());
        final SpannableStringBuilder span = new SpannableStringBuilder(getString(R.string.main_posts_count, postCount));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
        binding.mainTagPostCount.setText(span);
        binding.mainTagPostCount.setVisibility(View.VISIBLE);
    }

    public void stopCurrentExecutor() {
        if (currentlyExecuting != null) {
            try {
                currentlyExecuting.cancel(true);
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
                Log.e(TAG, "", e);
            }
        }
    }

    private void setTitle() {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(hashtag.substring(1));
        }
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
