package awais.instagrabber.fragments.main;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.asyncs.FeedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentFeedBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.PostViewV2Fragment;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesService;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FeedFragment";
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private static final int STORAGE_PERM_REQUEST_CODE_FOR_SELECTION = 8030;

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentFeedBinding binding;
    private StoriesService storiesService;
    private boolean shouldRefresh = true;
    private FeedStoriesViewModel feedStoriesViewModel;
    private boolean storiesFetching;
    private ActionMode actionMode;
    private Set<FeedModel> selectedFeedModels;
    private FeedModel downloadFeedModel;

    private final FeedAdapterV2.FeedItemCallback feedItemCallback = new FeedAdapterV2.FeedItemCallback() {
        @Override
        public void onPostClick(final FeedModel feedModel, final View profilePicView, final View mainPostImage) {
            openPostDialog(feedModel, profilePicView, mainPostImage, -1);
        }

        @Override
        public void onSliderClick(final FeedModel feedModel, final int position) {
            openPostDialog(feedModel, null, null, position);
        }

        @Override
        public void onCommentsClick(final FeedModel feedModel) {
            final NavDirections commentsAction = FeedFragmentDirections.actionGlobalCommentsViewerFragment(
                    feedModel.getShortCode(),
                    feedModel.getPostId(),
                    feedModel.getProfileModel().getId()
            );
            NavHostFragment.findNavController(FeedFragment.this).navigate(commentsAction);
        }

        @Override
        public void onDownloadClick(final FeedModel feedModel) {
            final Context context = getContext();
            if (context == null) return;
            if (checkSelfPermission(context, WRITE_PERMISSION) == PermissionChecker.PERMISSION_GRANTED) {
                showDownloadDialog(feedModel);
                return;
            }
            downloadFeedModel = feedModel;
            requestPermissions(DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE);
        }

        @Override
        public void onHashtagClick(final String hashtag) {
            final NavDirections action = FeedFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(FeedFragment.this).navigate(action);
        }

        @Override
        public void onLocationClick(final FeedModel feedModel) {
            final NavDirections action = FeedFragmentDirections.actionGlobalLocationFragment(feedModel.getLocationId());
            NavHostFragment.findNavController(FeedFragment.this).navigate(action);
        }

        @Override
        public void onMentionClick(final String mention) {
            navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(final FeedModel feedModel, final View profilePicView) {
            navigateToProfile("@" + feedModel.getProfileModel().getUsername());
        }

        @Override
        public void onProfilePicClick(final FeedModel feedModel, final View profilePicView) {
            navigateToProfile("@" + feedModel.getProfileModel().getUsername());
        }

        @Override
        public void onURLClick(final String url) {
            Utils.openURL(getContext(), url);
        }

        @Override
        public void onEmailClick(final String emailId) {
            Utils.openEmailAddress(getContext(), emailId);
        }

        private void openPostDialog(final FeedModel feedModel,
                                    final View profilePicView,
                                    final View mainPostImage,
                                    final int position) {
            final PostViewV2Fragment.Builder builder = PostViewV2Fragment
                    .builder(feedModel);
            if (position >= 0) {
                builder.setPosition(position);
            }
            final PostViewV2Fragment fragment = builder
                    .setSharedProfilePicElement(profilePicView)
                    .setSharedMainPostElement(mainPostImage)
                    .build();
            fragment.show(getChildFragmentManager(), "post_view");
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            binding.feedRecyclerView.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(final ActionMode mode) {
                    binding.feedRecyclerView.endSelection();
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (FeedFragment.this.selectedFeedModels == null) return false;
                        final Context context = getContext();
                        if (context == null) return false;
                        if (checkSelfPermission(context, WRITE_PERMISSION) == PermissionChecker.PERMISSION_GRANTED) {
                            DownloadUtils.download(context, ImmutableList.copyOf(FeedFragment.this.selectedFeedModels));
                            binding.feedRecyclerView.endSelection();
                            return true;
                        }
                        requestPermissions(DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE_FOR_SELECTION);
                        return true;
                    }
                    return false;
                }
            });
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!onBackPressedCallback.isEnabled()) {
                final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
                onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
            }
            if (actionMode == null) {
                actionMode = fragmentActivity.startActionMode(multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(final Set<FeedModel> selectedFeedModels) {
            final String title = getString(R.string.number_selected, selectedFeedModels.size());
            if (actionMode != null) {
                actionMode.setTitle(title);
            }
            FeedFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (onBackPressedCallback.isEnabled()) {
                onBackPressedCallback.setEnabled(false);
                onBackPressedCallback.remove();
            }
            if (actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
        }
    };

    private void navigateToProfile(final String username) {
        final NavController navController = NavHostFragment.findNavController(this);
        final Bundle bundle = new Bundle();
        bundle.putString("username", username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        storiesService = StoriesService.getInstance();
        setHasOptionsMenu(true);
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
        binding.feedSwipeRefreshLayout.setOnRefreshListener(this);
        setupFeedStories();
        setupFeed();
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.feed_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.layout) {
            showPostsLayoutPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        // if (videoAwareRecyclerScroller != null) {
        //     videoAwareRecyclerScroller.stopPlaying();
        // }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.feedSwipeRefreshLayout.setRefreshing(false);
        // if (videoAwareRecyclerScroller != null && shouldAutoPlay) {
        //     videoAwareRecyclerScroller.startPlaying();
        // }
    }

    @Override
    public void onRefresh() {
        binding.feedRecyclerView.refresh();
        fetchStories();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == STORAGE_PERM_REQUEST_CODE && granted) {
            if (downloadFeedModel == null) return;
            showDownloadDialog(downloadFeedModel);
            return;
        }
        if (requestCode == STORAGE_PERM_REQUEST_CODE_FOR_SELECTION && granted) {
            final Context context = getContext();
            if (context == null) return;
            DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
            binding.feedRecyclerView.endSelection();
        }
    }

    private void setupFeed() {
        binding.feedRecyclerView.setViewModelStoreOwner(this)
                                .setLifeCycleOwner(this)
                                .setPostFetchService(new FeedPostFetchService())
                                .setLayoutPreferences(PostsLayoutPreferences.fromJson(settingsHelper.getString(Constants.PREF_POSTS_LAYOUT)))
                                .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
                                .setFeedItemCallback(feedItemCallback)
                                .setSelectionModeCallback(selectionModeCallback)
                                .init();
        binding.feedSwipeRefreshLayout.setRefreshing(true);
        // if (shouldAutoPlay) {
        //     videoAwareRecyclerScroller = new VideoAwareRecyclerScroller();
        //     binding.feedRecyclerView.addOnScrollListener(videoAwareRecyclerScroller);
        // }
    }

    private void updateSwipeRefreshState() {
        binding.feedSwipeRefreshLayout.setRefreshing(binding.feedRecyclerView.isFetching() || storiesFetching);
    }

    private void setupFeedStories() {
        feedStoriesViewModel = new ViewModelProvider(fragmentActivity).get(FeedStoriesViewModel.class);
        final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter((model, position) -> {
            final NavDirections action = FeedFragmentDirections.actionFeedFragmentToStoryViewerFragment(position, null, false, false, null, null);
            NavHostFragment.findNavController(this).navigate(action);
        });
        final Context context = getContext();
        if (context == null) return;
        binding.feedStoriesRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        binding.feedStoriesRecyclerView.setAdapter(feedStoriesAdapter);
        feedStoriesViewModel.getList().observe(fragmentActivity, feedStoriesAdapter::submitList);
        fetchStories();
    }

    private void fetchStories() {
        storiesFetching = true;
        updateSwipeRefreshState();
        storiesService.getFeedStories(new ServiceCallback<List<FeedStoryModel>>() {
            @Override
            public void onSuccess(final List<FeedStoryModel> result) {
                feedStoriesViewModel.getList().postValue(result);
                storiesFetching = false;
                updateSwipeRefreshState();
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "failed", t);
                storiesFetching = false;
                updateSwipeRefreshState();
            }
        });
    }

    private void showDownloadDialog(@NonNull final FeedModel feedModel) {
        final Context context = getContext();
        if (context == null) return;
        DownloadUtils.download(context, feedModel);
        // switch (feedModel.getItemType()) {
        //     case MEDIA_TYPE_IMAGE:
        //     case MEDIA_TYPE_VIDEO:
        //         break;
        //     case MEDIA_TYPE_SLIDER:
        //         break;
        // }
        // final List<ViewerPostModel> postModelsToDownload = new ArrayList<>();
        // // if (!session) {
        // final DialogInterface.OnClickListener clickListener = (dialog, which) -> {
        //     if (which == DialogInterface.BUTTON_NEGATIVE) {
        //         postModelsToDownload.addAll(postModels);
        //     } else if (which == DialogInterface.BUTTON_POSITIVE) {
        //         postModelsToDownload.add(postModels.get(childPosition));
        //     } else {
        //         session = true;
        //         postModelsToDownload.add(postModels.get(childPosition));
        //     }
        //     if (postModelsToDownload.size() > 0) {
        //         DownloadUtils.batchDownload(context,
        //                                     username,
        //                                     DownloadMethod.DOWNLOAD_POST_VIEWER,
        //                                     postModelsToDownload);
        //     }
        // };
        // new AlertDialog.Builder(context)
        //         .setTitle(R.string.post_viewer_download_dialog_title)
        //         .setMessage(R.string.post_viewer_download_message)
        //         .setNeutralButton(R.string.post_viewer_download_session, clickListener)
        //         .setPositiveButton(R.string.post_viewer_download_current, clickListener)
        //         .setNegativeButton(R.string.post_viewer_download_album, clickListener).show();
        // } else {
        //     DownloadUtils.batchDownload(context,
        //                                 username,
        //                                 DownloadMethod.DOWNLOAD_POST_VIEWER,
        //                                 Collections.singletonList(postModels.get(childPosition)));
    }

    private void showPostsLayoutPreferences() {
        final PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_POSTS_LAYOUT,
                preferences -> new Handler().postDelayed(() -> binding.feedRecyclerView.setLayoutPreferences(preferences), 200));
        fragment.show(getChildFragmentManager(), "posts_layout_preferences");
    }
}
