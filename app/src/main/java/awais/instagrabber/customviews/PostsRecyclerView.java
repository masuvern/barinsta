package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.adapters.FeedAdapterV2.OnPostClickListener;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtBottom;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FeedViewModel;

public class PostsRecyclerView extends RecyclerView {
    private static final String TAG = "PostsRecyclerView";

    private StaggeredGridLayoutManager gridLayoutManager;
    private PostsLayoutPreferences layoutPreferences;
    private PostFetcher.PostFetchService postFetchService;
    private Transition transition;
    private OnClickListener postViewClickListener;
    private MentionClickListener mentionClickListener;
    private PostFetcher postFetcher;
    private ViewModelStoreOwner viewModelStoreOwner;
    private FeedAdapterV2 feedAdapter;
    private LifecycleOwner lifeCycleOwner;
    private FeedViewModel feedViewModel;
    private boolean initCalled = false;
    private GridSpacingItemDecoration gridSpacingItemDecoration;
    private RecyclerLazyLoaderAtBottom lazyLoader;
    private OnPostClickListener onPostClickListener;

    private final FetchListener<List<FeedModel>> fetchListener = new FetchListener<List<FeedModel>>() {
        @Override
        public void onResult(final List<FeedModel> result) {
            final int currentPage = lazyLoader.getCurrentPage();
            if (currentPage == 0) {
                feedViewModel.getList().postValue(result);
                dispatchFetchStatus();
                return;
            }
            final List<FeedModel> models = feedViewModel.getList().getValue();
            final List<FeedModel> modelsCopy = models == null ? new ArrayList<>() : new ArrayList<>(models);
            modelsCopy.addAll(result);
            feedViewModel.getList().postValue(modelsCopy);
            dispatchFetchStatus();
        }

        @Override
        public void onFailure(final Throwable t) {
            Log.e(TAG, "onFailure: ", t);
        }
    };
    private final List<FetchStatusChangeListener> fetchStatusChangeListeners = new ArrayList<>();

    public PostsRecyclerView(@NonNull final Context context) {
        super(context);
    }

    public PostsRecyclerView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public PostsRecyclerView(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PostsRecyclerView setViewModelStoreOwner(final ViewModelStoreOwner owner) {
        if (initCalled) {
            throw new IllegalArgumentException("init already called!");
        }
        this.viewModelStoreOwner = owner;
        return this;
    }

    public PostsRecyclerView setLifeCycleOwner(final LifecycleOwner lifeCycleOwner) {
        if (initCalled) {
            throw new IllegalArgumentException("init already called!");
        }
        this.lifeCycleOwner = lifeCycleOwner;
        return this;
    }

    public PostsRecyclerView setPostFetchService(final PostFetcher.PostFetchService postFetchService) {
        if (initCalled) {
            throw new IllegalArgumentException("init already called!");
        }
        this.postFetchService = postFetchService;
        return this;
    }

    public PostsRecyclerView setOnPostClickListener(@NonNull final OnPostClickListener onPostClickListener) {
        this.onPostClickListener = onPostClickListener;
        return this;
    }

    public PostsRecyclerView setMentionClickListener(final MentionClickListener mentionClickListener) {
        this.mentionClickListener = mentionClickListener;
        return this;
    }

    public PostsRecyclerView setLayoutPreferences(final PostsLayoutPreferences layoutPreferences) {
        this.layoutPreferences = layoutPreferences;
        if (initCalled) {
            if (layoutPreferences == null) return this;
            feedAdapter.setLayoutPreferences(layoutPreferences);
            updateLayout();
        }
        return this;
    }

    public void init() {
        initCalled = true;
        if (viewModelStoreOwner == null) {
            throw new IllegalArgumentException("ViewModelStoreOwner cannot be null");
        } else if (lifeCycleOwner == null) {
            throw new IllegalArgumentException("LifecycleOwner cannot be null");
        } else if (postFetchService == null) {
            throw new IllegalArgumentException("PostFetchService cannot be null");
        }
        if (layoutPreferences == null) {
            layoutPreferences = PostsLayoutPreferences.builder()
                                                      .setType(PostsLayoutPreferences.PostsLayoutType.GRID)
                                                      .setColCount(3)
                                                      .setAvatarVisible(true)
                                                      .setNameVisible(false)
                                                      .setProfilePicSize(PostsLayoutPreferences.ProfilePicSize.TINY)
                                                      .setHasGap(true)
                                                      .setHasRoundedCorners(true)
                                                      .build();
            Utils.settingsHelper.putString(Constants.PREF_POSTS_LAYOUT, layoutPreferences.getJson());
        }
        gridSpacingItemDecoration = new GridSpacingItemDecoration(Utils.convertDpToPx(2));
        initTransition();
        initAdapter();
        initLayoutManager();
        initSelf();

    }

    private void initTransition() {
        transition = new ChangeBounds();
        // transition.addListener(new TransitionListenerAdapter(){
        //     @Override
        //     public void onTransitionEnd(@NonNull final Transition transition) {
        //         super.onTransitionEnd(transition);
        //     }
        // });
        transition.setDuration(300);
    }

    private void initLayoutManager() {
        gridLayoutManager = new StaggeredGridLayoutManager(layoutPreferences.getColCount(), StaggeredGridLayoutManager.VERTICAL);
        setLayoutManager(gridLayoutManager);
    }

    private void initAdapter() {
        feedAdapter = new FeedAdapterV2(
                layoutPreferences,
                postViewClickListener,
                mentionClickListener,
                (feedModel, view, postImage) -> {
                    if (onPostClickListener != null) {
                        onPostClickListener.onPostClick(feedModel, view, postImage);
                    }
                });
        feedAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        setAdapter(feedAdapter);
    }

    private void initSelf() {
        feedViewModel = new ViewModelProvider(viewModelStoreOwner).get(FeedViewModel.class);
        feedViewModel.getList().observe(lifeCycleOwner, feedAdapter::submitList);
        postFetcher = new PostFetcher(postFetchService, fetchListener);
        addItemDecoration(gridSpacingItemDecoration);
        setHasFixedSize(true);
        lazyLoader = new RecyclerLazyLoaderAtBottom(gridLayoutManager, (page) -> {
            if (postFetcher.hasMore()) {
                postFetcher.fetchNextPage();
                dispatchFetchStatus();
            }
        });
        addOnScrollListener(lazyLoader);
        postFetcher.fetch();
        dispatchFetchStatus();
    }

    private void updateLayout() {
        post(() -> {
            TransitionManager.beginDelayedTransition(this, transition);
            feedAdapter.notifyDataSetChanged();
            if (!layoutPreferences.getHasGap()) {
                removeItemDecoration(gridSpacingItemDecoration);
            }
            gridLayoutManager.setSpanCount(layoutPreferences.getColCount());
        });
    }

    public void refresh() {
        lazyLoader.resetState();
        postFetcher.fetch();
        dispatchFetchStatus();
    }

    public boolean isFetching() {
        return postFetcher != null && postFetcher.isFetching();
    }

    public PostsRecyclerView addFetchStatusChangeListener(final FetchStatusChangeListener fetchStatusChangeListener) {
        if (fetchStatusChangeListener == null) return this;
        fetchStatusChangeListeners.add(fetchStatusChangeListener);
        return this;
    }

    public void removeFetchStatusListener(final FetchStatusChangeListener fetchStatusChangeListener) {
        if (fetchStatusChangeListener == null) return;
        fetchStatusChangeListeners.remove(fetchStatusChangeListener);
    }

    private void dispatchFetchStatus() {
        for (final FetchStatusChangeListener listener : fetchStatusChangeListeners) {
            listener.onFetchStatusChange(isFetching());
        }
    }

    public PostsLayoutPreferences getLayoutPreferences() {
        return layoutPreferences;
    }

    public interface FetchStatusChangeListener {
        void onFetchStatusChange(boolean fetching);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        lifeCycleOwner = null;
    }
}
