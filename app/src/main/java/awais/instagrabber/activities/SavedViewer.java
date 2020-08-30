package awais.instagrabber.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.i.iLikedFetcher;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.ActivitySavedBinding;
import awais.instagrabber.fragments.main.viewmodels.PostsViewModel;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.ItemGetter;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
import static awais.instagrabber.utils.Utils.logCollector;

public final class SavedViewer extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private static AsyncTask<?, ?, ?> currentlyExecuting;
    public static ItemGetter itemGetter;
    private PostsAdapter postsAdapter;
    private boolean hasNextPage, autoloadPosts;
    //private CommentModel commentModel;
    private ActivitySavedBinding savedBinding;
    private String action, username, endCursor;
    private RecyclerLazyLoader lazyLoader;
    private Resources resources;
    private ArrayList<PostModel> selectedItems = new ArrayList<>();
    private ActionMode actionMode;
    private PostsViewModel postsViewModel;

    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (postsAdapter == null) {
                remove();
                return;
            }
            postsAdapter.clearSelection();
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
                        if (postsAdapter == null || username == null) {
                            return false;
                        }
                        Utils.batchDownload(SavedViewer.this,
                                username,
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
            if (result != null) {
                final List<PostModel> current = postsViewModel.getList().getValue();
                final List<PostModel> resultList = Arrays.asList(result);
                if (current == null) {
                    postsViewModel.getList().postValue(resultList);
                } else {
                    final List<PostModel> currentCopy = new ArrayList<>(current);
                    currentCopy.addAll(resultList);
                    postsViewModel.getList().postValue(currentCopy);
                }
                savedBinding.mainPosts.post(() -> {
                    savedBinding.mainPosts.setNestedScrollingEnabled(true);
                    savedBinding.mainPosts.setVisibility(View.VISIBLE);
                });

                final PostModel model = result.length > 0 ? result[result.length - 1] : null;
                if (model != null) {
                    endCursor = model.getEndCursor();

                    hasNextPage = model.hasNextPage();
                    if (autoloadPosts && hasNextPage && action.charAt(0) == '^')
                        currentlyExecuting = new iLikedFetcher(endCursor, postsFetchListener)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else if (autoloadPosts && hasNextPage)
                        currentlyExecuting = new PostsFetcher(action, endCursor, this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else {
                        savedBinding.swipeRefreshLayout.setRefreshing(false);
                    }
                    model.setPageCursor(false, null);
                }
            }
            savedBinding.swipeRefreshLayout.setRefreshing(false);
            // if (oldSize == 0) {
            //     Toast.makeText(getApplicationContext(), R.string.empty_list, Toast.LENGTH_SHORT).show();
            //     finish();
            // }
        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedBinding = ActivitySavedBinding.inflate(getLayoutInflater());
        setContentView(savedBinding.getRoot());
        savedBinding.swipeRefreshLayout.setOnRefreshListener(this);
        autoloadPosts = Utils.settingsHelper.getBoolean(AUTOLOAD_POSTS);
        savedBinding.mainPosts.setNestedScrollingEnabled(false);
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(this, Utils.convertDpToPx(110));
        savedBinding.mainPosts.setLayoutManager(layoutManager);
        savedBinding.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_INDEX)
                || Utils.isEmpty((action = intent.getStringExtra(Constants.EXTRAS_INDEX)))
                || !intent.hasExtra(Constants.EXTRAS_USER)
                || Utils.isEmpty((username = intent.getStringExtra(Constants.EXTRAS_USER)))) {
            Utils.errorFinish(this);
            return;
        }

        postsViewModel = new ViewModelProvider(this).get(PostsViewModel.class);
        postsAdapter = new PostsAdapter((postModel, position) -> {
            if (postsAdapter.isSelecting()) {
                if (actionMode == null) return;
                final String title = getString(R.string.number_selected, postsAdapter.getSelectedModels().size());
                actionMode.setTitle(title);
                return;
            }
            if (checkAndResetAction()) return;
            startActivity(new Intent(this, PostViewer.class)
                    .putExtra(Constants.EXTRAS_INDEX, position)
                    .putExtra(Constants.EXTRAS_POST, postModel)
                    .putExtra(Constants.EXTRAS_USER, username)
                    .putExtra(Constants.EXTRAS_TYPE, ItemGetType.SAVED_ITEMS));

        }, (model, position) -> {
            if (!postsAdapter.isSelecting()) {
                checkAndResetAction();
                return true;
            }
            final OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
            if (onBackPressedDispatcher.hasEnabledCallbacks()) return true;
            actionMode = startActionMode(multiSelectAction);
            final String title = getString(R.string.number_selected, 1);
            actionMode.setTitle(title);
            onBackPressedDispatcher.addCallback(onBackPressedCallback);
            return true;
        });
        savedBinding.mainPosts.setAdapter(postsAdapter);
        postsViewModel.getList().observe(this, postsAdapter::submitList);
        savedBinding.swipeRefreshLayout.setRefreshing(true);
        setSupportActionBar(savedBinding.toolbar.toolbar);
        savedBinding.toolbar.toolbar.setTitle((action.charAt(0) == '$' ? R.string.saved :
                (action.charAt(0) == '%' ? R.string.tagged : R.string.liked)));
        savedBinding.toolbar.toolbar.setSubtitle(username);

        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!autoloadPosts && hasNextPage) {
                savedBinding.swipeRefreshLayout.setRefreshing(true);
                stopCurrentExecutor();

                currentlyExecuting = action.charAt(0) == '^'
                        ? new iLikedFetcher(endCursor, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                        : new PostsFetcher(action, endCursor, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                endCursor = null;
            }
        });
        savedBinding.mainPosts.addOnScrollListener(lazyLoader);

        itemGetter = itemGetType -> {
            if (itemGetType == ItemGetType.SAVED_ITEMS)
                return postsViewModel.getList().getValue();
            return null;
        };

        if (action.charAt(0) == '^')
            new iLikedFetcher(postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            new PostsFetcher(action, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.saved, menu);

        final MenuItem downloadAction = menu.findItem(R.id.downloadAction);
        downloadAction.setVisible(false);

        menu.findItem(R.id.favouriteAction).setVisible(false);

        downloadAction.setOnMenuItemClickListener(item -> {
            if (selectedItems.size() > 0) {
                Utils.batchDownload(this, null, DownloadMethod.DOWNLOAD_SAVED, selectedItems);
            }
            return true;
        });
        return true;
    }

    @Override
    public void onRefresh() {
        if (lazyLoader != null) lazyLoader.resetState();
        stopCurrentExecutor();
        postsViewModel.getList().postValue(Collections.emptyList());
        selectedItems.clear();
        if (postsAdapter != null) {
            // postsAdapter.isSelecting = false;
            postsAdapter.notifyDataSetChanged();
        }
        savedBinding.swipeRefreshLayout.setRefreshing(true);
        if (action.charAt(0) == '^')
            new iLikedFetcher(postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            new PostsFetcher(action, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED && selectedItems.size() > 0)
            Utils.batchDownload(this, null, DownloadMethod.DOWNLOAD_SAVED, selectedItems);
    }

    public static void stopCurrentExecutor() {
        if (currentlyExecuting != null) {
            try {
                currentlyExecuting.cancel(true);
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
    }

    private boolean checkAndResetAction() {
        final OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        if (!onBackPressedDispatcher.hasEnabledCallbacks() || actionMode == null) {
            return false;
        }
        actionMode.finish();
        actionMode = null;
        return true;
    }
}