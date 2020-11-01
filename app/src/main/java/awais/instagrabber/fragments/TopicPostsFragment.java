package awais.instagrabber.fragments;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.PermissionChecker;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionSet;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.DiscoverPostFetchService;
import awais.instagrabber.customviews.helpers.NestedCoordinatorLayout;
import awais.instagrabber.databinding.FragmentTopicPostsBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.main.DiscoverFragmentDirections;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.TopicCluster;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.DiscoverService;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class TopicPostsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private MainActivity fragmentActivity;
    private FragmentTopicPostsBinding binding;
    private NestedCoordinatorLayout root;
    private boolean shouldRefresh = true;
    private TopicCluster topicCluster;

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
            final NavDirections commentsAction = DiscoverFragmentDirections.actionGlobalCommentsViewerFragment(
                    feedModel.getShortCode(),
                    feedModel.getPostId(),
                    feedModel.getProfileModel().getId()
            );
            NavHostFragment.findNavController(TopicPostsFragment.this).navigate(commentsAction);
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
            final NavDirections action = DiscoverFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(TopicPostsFragment.this).navigate(action);
        }

        @Override
        public void onLocationClick(final FeedModel feedModel) {
            final NavDirections action = DiscoverFragmentDirections.actionGlobalLocationFragment(feedModel.getLocationId());
            NavHostFragment.findNavController(TopicPostsFragment.this).navigate(action);
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
        fragmentActivity = (MainActivity) requireActivity();
        final TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(new ChangeBounds())
                     .addTransition(TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move))
                     .setDuration(200);
        setSharedElementEnterTransition(transitionSet);
        postponeEnterTransition();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentTopicPostsBinding.inflate(inflater, container, false);
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
        inflater.inflate(R.menu.topic_posts_menu, menu);
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
        fragmentActivity.setToolbar(binding.toolbar);
    }

    @Override
    public void onRefresh() {
        binding.posts.refresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetToolbar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetToolbar();
    }

    private void resetToolbar() {
        fragmentActivity.resetToolbar();
    }

    private void init() {
        if (getArguments() == null) return;
        final TopicPostsFragmentArgs fragmentArgs = TopicPostsFragmentArgs.fromBundle(getArguments());
        topicCluster = fragmentArgs.getTopicCluster();
        setupToolbar(fragmentArgs.getTitleColor(), fragmentArgs.getBackgroundColor());
        setupPosts();
    }

    private void setupToolbar(final int titleColor, final int backgroundColor) {
        if (topicCluster == null) {
            return;
        }
        binding.cover.setTransitionName("cover-" + topicCluster.getId());
        fragmentActivity.setToolbar(binding.toolbar);
        binding.collapsingToolbarLayout.setTitle(topicCluster.getTitle());
        final int collapsedTitleTextColor = ColorUtils.setAlphaComponent(titleColor, 0xFF);
        final int expandedTitleTextColor = ColorUtils.setAlphaComponent(titleColor, 0x99);
        binding.collapsingToolbarLayout.setExpandedTitleColor(expandedTitleTextColor);
        binding.collapsingToolbarLayout.setCollapsedTitleTextColor(collapsedTitleTextColor);
        binding.collapsingToolbarLayout.setContentScrimColor(backgroundColor);
        final Drawable navigationIcon = binding.toolbar.getNavigationIcon();
        final Drawable overflowIcon = binding.toolbar.getOverflowIcon();
        if (navigationIcon != null && overflowIcon != null) {
            final Drawable navDrawable = navigationIcon.mutate();
            final Drawable overflowDrawable = overflowIcon.mutate();
            navDrawable.setAlpha(0xFF);
            overflowDrawable.setAlpha(0xFF);
            final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
            binding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                final int totalScrollRange = appBarLayout.getTotalScrollRange();
                final float current = totalScrollRange + verticalOffset;
                final float fraction = current / totalScrollRange;
                final int tempColor = (int) argbEvaluator.evaluate(fraction, collapsedTitleTextColor, expandedTitleTextColor);
                navDrawable.setColorFilter(tempColor, PorterDuff.Mode.SRC_ATOP);
                overflowDrawable.setColorFilter(tempColor, PorterDuff.Mode.SRC_ATOP);

            });
        }
        final GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, backgroundColor});
        binding.background.setBackground(gd);
        setupCover();
    }

    private void setupCover() {
        final String coverUrl = topicCluster.getCoverMedia().getDisplayUrl();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setOldController(binding.cover.getController())
                .setUri(coverUrl)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {

                    @Override
                    public void onFailure(final String id, final Throwable throwable) {
                        super.onFailure(id, throwable);
                        startPostponedEnterTransition();
                    }

                    @Override
                    public void onFinalImageSet(final String id,
                                                @Nullable final ImageInfo imageInfo,
                                                @Nullable final Animatable animatable) {
                        startPostponedEnterTransition();
                    }
                })
                .build();
        binding.cover.setController(controller);
    }

    private void setupPosts() {
        final DiscoverService.TopicalExploreRequest topicalExploreRequest = new DiscoverService.TopicalExploreRequest();
        topicalExploreRequest.setClusterId(topicCluster.getId());
        binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new DiscoverPostFetchService(topicalExploreRequest))
                     .setLayoutPreferences(PostsLayoutPreferences.fromJson(settingsHelper.getString(Constants.PREF_TOPIC_POSTS_LAYOUT)))
                     .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
                     .setFeedItemCallback(feedItemCallback)
                     .init();
        binding.swipeRefreshLayout.setRefreshing(true);
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
                Constants.PREF_TOPIC_POSTS_LAYOUT,
                preferences -> new Handler().postDelayed(() -> binding.posts.setLayoutPreferences(preferences), 200));
        fragment.show(getChildFragmentManager(), "posts_layout_preferences");
    }
}
