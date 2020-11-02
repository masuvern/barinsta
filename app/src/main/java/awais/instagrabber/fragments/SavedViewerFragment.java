package awais.instagrabber.fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.SavedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentSavedBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.main.ProfileFragmentDirections;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.Utils;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class SavedViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;

    private FragmentSavedBinding binding;
    private String username;
    private ActionMode actionMode;
    private SwipeRefreshLayout root;
    private AppCompatActivity fragmentActivity;
    private boolean shouldRefresh = true;
    private PostItemType type;
    private String profileId;

    private final ArrayList<PostModel> selectedItems = new ArrayList<>();
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            remove();
            // if (postsAdapter == null) return;
            // postsAdapter.clearSelection();
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
                        // if (postsAdapter == null || username == null) {
                        //     return false;
                        // }
                        // final Context context = getContext();
                        // if (context == null) return false;
                        // DownloadUtils.batchDownload(context,
                        //                             username,
                        //                             DownloadMethod.DOWNLOAD_SAVED,
                        //                             postsAdapter.getSelectedModels());
                        // checkAndResetAction();
                        return true;
                    }
                    return false;
                }
            });
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
            final NavDirections commentsAction = ProfileFragmentDirections.actionGlobalCommentsViewerFragment(
                    feedModel.getShortCode(),
                    feedModel.getPostId(),
                    feedModel.getProfileModel().getId()
            );
            NavHostFragment.findNavController(SavedViewerFragment.this).navigate(commentsAction);
        }

        @Override
        public void onDownloadClick(final FeedModel feedModel) {
            final Context context = getContext();
            if (context == null) return;
            if (checkSelfPermission(context, WRITE_PERMISSION) == PermissionChecker.PERMISSION_GRANTED) {
                showDownloadDialog(feedModel);
                return;
            }
            requestPermissions(DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE);
        }

        @Override
        public void onHashtagClick(final String hashtag) {
            final NavDirections action = ProfileFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
        }

        @Override
        public void onLocationClick(final FeedModel feedModel) {
            final NavDirections action = ProfileFragmentDirections.actionGlobalLocationFragment(feedModel.getLocationId());
            NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
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

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentSavedBinding.inflate(getLayoutInflater(), container, false);
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

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.saved_viewer_menu, menu);
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
    public void onResume() {
        super.onResume();
        setTitle();
    }

    @Override
    public void onRefresh() {
        binding.posts.refresh();
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) return;
        final SavedViewerFragmentArgs fragmentArgs = SavedViewerFragmentArgs.fromBundle(arguments);
        username = fragmentArgs.getUsername();
        profileId = fragmentArgs.getProfileId();
        type = fragmentArgs.getType();
        setupPosts();
        // postsAdapter = new PostsAdapter((postModel, position) -> {
        //     if (postsAdapter.isSelecting()) {
        //         if (actionMode == null) return;
        //         final String title = getString(R.string.number_selected, postsAdapter.getSelectedModels().size());
        //         actionMode.setTitle(title);
        //         return;
        //     }
        //     if (checkAndResetAction()) return;
        //     final List<PostModel> postModels = postsViewModel.getList().getValue();
        //     if (postModels == null || postModels.size() == 0) return;
        //     if (postModels.get(0) == null) return;
        //     final String postId = postModels.get(0).getPostId();
        //     final boolean isId = postId != null;
        //     final String[] idsOrShortCodes = new String[postModels.size()];
        //     for (int i = 0; i < postModels.size(); i++) {
        //         final PostModel tempPostModel = postModels.get(i);
        //         final String tempId = tempPostModel.getPostId();
        //         final String finalPostId = type == PostItemType.LIKED ? tempId.substring(0, tempId.indexOf("_")) : tempId;
        //         idsOrShortCodes[i] = isId ? finalPostId
        //                                   : tempPostModel.getShortCode();
        //     }
        //     final NavDirections action = ProfileFragmentDirections.actionGlobalPostViewFragment(
        //             position,
        //             idsOrShortCodes,
        //             isId);
        //     NavHostFragment.findNavController(this).navigate(action);
        // }, (model, position) -> {
        //     if (!postsAdapter.isSelecting()) {
        //         checkAndResetAction();
        //         return true;
        //     }
        //     final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
        //     if (onBackPressedCallback.isEnabled()) return true;
        //     actionMode = fragmentActivity.startActionMode(multiSelectAction);
        //     final String title = getString(R.string.number_selected, 1);
        //     actionMode.setTitle(title);
        //     onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        //     return true;
        // });
    }

    private void setupPosts() {
        binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new SavedPostFetchService(profileId, type))
                     .setLayoutPreferences(PostsLayoutPreferences.fromJson(settingsHelper.getString(getPostsLayoutPreferenceKey())))
                     .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
                     .setFeedItemCallback(feedItemCallback)
                     .init();
        binding.swipeRefreshLayout.setRefreshing(true);
    }

    @NonNull
    private String getPostsLayoutPreferenceKey() {
        switch (type) {
            case LIKED:
                return Constants.PREF_LIKED_POSTS_LAYOUT;
            case TAGGED:
                return Constants.PREF_TAGGED_POSTS_LAYOUT;
            case SAVED:
            default:
                return Constants.PREF_SAVED_POSTS_LAYOUT;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // final Context context = getContext();
            // if (context == null) return;
            // DownloadUtils.batchDownload(context, null, DownloadMethod.DOWNLOAD_SAVED, selectedItems);
        }
    }

    private void setTitle() {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        final int titleRes;
        switch (type) {
            case LIKED:
                titleRes = R.string.liked;
                break;
            case TAGGED:
                titleRes = R.string.tagged;
                break;
            default:
            case SAVED:
                titleRes = R.string.saved;
                break;
        }
        actionBar.setTitle(titleRes);
        actionBar.setSubtitle(username);
    }

    private void updateSwipeRefreshState() {
        binding.swipeRefreshLayout.setRefreshing(binding.posts.isFetching());
    }

    private void showDownloadDialog(final FeedModel feedModel) {
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

    private void navigateToProfile(final String username) {
        final NavController navController = NavHostFragment.findNavController(this);
        final Bundle bundle = new Bundle();
        bundle.putString("username", username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    private void showPostsLayoutPreferences() {
        final PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                getPostsLayoutPreferenceKey(),
                preferences -> new Handler().postDelayed(() -> binding.posts.setLayoutPreferences(preferences), 200));
        fragment.show(getChildFragmentManager(), "posts_layout_preferences");
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