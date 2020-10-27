package awais.instagrabber.fragments.main;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import awais.instagrabber.ProfileNavGraphDirections;
import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.adapters.HighlightsAdapter;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.HighlightsFetcher;
import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.asyncs.ProfilePostFetchService;
import awais.instagrabber.asyncs.UsernameFetcher;
import awais.instagrabber.asyncs.direct_messages.CreateThreadAction;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.PrimaryActionModeCallback.CallbacksHelper;
import awais.instagrabber.customviews.helpers.NestedCoordinatorLayout;
import awais.instagrabber.databinding.FragmentProfileBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.dialogs.ProfilePicDialogFragment;
import awais.instagrabber.fragments.PostViewV2Fragment;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoRestrictRootResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.HighlightsViewModel;
import awais.instagrabber.webservices.FriendshipService;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesService;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class ProfileFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "ProfileFragment";
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentProfileBinding binding;
    private boolean isLoggedIn;
    private String cookie;
    private String username;
    private ProfileModel profileModel;
    private PostsAdapter postsAdapter;
    private ActionMode actionMode;
    private Handler usernameSettingHandler;
    private FriendshipService friendshipService;
    private StoriesService storiesService;
    private boolean shouldRefresh = true;
    private boolean hasStories = false;
    private HighlightsAdapter highlightsAdapter;
    private HighlightsViewModel highlightsViewModel;
    private MenuItem blockMenuItem;
    private MenuItem restrictMenuItem;
    private boolean highlightsFetching;

    private final Runnable usernameSettingRunnable = () -> {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null && !TextUtils.isEmpty(username)) {
            final String finalUsername = username.startsWith("@") ? username.substring(1)
                                                                  : username;
            actionBar.setTitle(finalUsername);
            actionBar.setSubtitle(null);
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            remove();
            if (postsAdapter == null) return;
            postsAdapter.clearSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new CallbacksHelper() {
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
                        final Context context = getContext();
                        if (context == null) return false;
                        DownloadUtils.batchDownload(context,
                                                    username,
                                                    DownloadMethod.DOWNLOAD_MAIN,
                                                    postsAdapter.getSelectedModels());
                        checkAndResetAction();
                        return true;
                    }
                    return false;
                }
            });
    private final MentionClickListener mentionClickListener = (view, text, isHashtag, isLocation) -> {
        Log.d(TAG, "action...");
        if (isHashtag) {
            final NavDirections action = ProfileFragmentDirections
                    .actionGlobalHashTagFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        if (isLocation) {
            final NavDirections action = FeedFragmentDirections.actionGlobalLocationFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        final ProfileNavGraphDirections.ActionGlobalProfileFragment action = ProfileFragmentDirections
                .actionGlobalProfileFragment();
        action.setUsername("@" + text);
        NavHostFragment.findNavController(this).navigate(action);
    };
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
            NavHostFragment.findNavController(ProfileFragment.this).navigate(commentsAction);
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
            final NavDirections action = FeedFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(ProfileFragment.this).navigate(action);
        }

        @Override
        public void onLocationClick(final FeedModel feedModel) {
            final NavDirections action = FeedFragmentDirections.actionGlobalLocationFragment(feedModel.getLocationId());
            NavHostFragment.findNavController(ProfileFragment.this).navigate(action);
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
    private boolean postsSetupDone = false;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        friendshipService = FriendshipService.getInstance();
        storiesService = StoriesService.getInstance();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != null;
        if (root != null) {
            if (getArguments() != null) {
                final ProfileFragmentArgs fragmentArgs = ProfileFragmentArgs.fromBundle(getArguments());
                final String username = fragmentArgs.getUsername();
                if (TextUtils.isEmpty(username) && profileModel != null) {
                    final String profileModelUsername = profileModel.getUsername();
                    final boolean isSame = ("@" + profileModelUsername).equals(this.username);
                    if (isSame) {
                        setUsernameDelayed();
                        shouldRefresh = false;
                        return root;
                    }
                }
                if (username == null || !username.equals(this.username)) {
                    shouldRefresh = true;
                    return root;
                }
            }
            setUsernameDelayed();
            shouldRefresh = false;
            return root;
        }
        binding = FragmentProfileBinding.inflate(inflater, container, false);
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
        inflater.inflate(R.menu.profile_menu, menu);
        blockMenuItem = menu.findItem(R.id.block);
        if (blockMenuItem != null) {
            blockMenuItem.setVisible(false);
        }
        restrictMenuItem = menu.findItem(R.id.restrict);
        if (restrictMenuItem != null) {
            restrictMenuItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.layout) {
            showPostsLayoutPreferences();
            return true;
        }
        if (item.getItemId() == R.id.restrict) {
            if (!isLoggedIn) return false;
            final String action = profileModel.getRestricted() ? "Unrestrict" : "Restrict";
            friendshipService.toggleRestrict(
                    profileModel.getId(),
                    !profileModel.getRestricted(),
                    CookieUtils.getCsrfTokenFromCookie(cookie),
                    new ServiceCallback<FriendshipRepoRestrictRootResponse>() {
                        @Override
                        public void onSuccess(final FriendshipRepoRestrictRootResponse result) {
                            Log.d(TAG, action + " success: " + result);
                            fetchProfileDetails();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error while performing " + action, t);
                        }
                    });
            return true;
        }
        if (item.getItemId() == R.id.block) {
            final String userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
            if (!isLoggedIn) return false;
            if (profileModel.getBlocked()) {
                friendshipService.unblock(
                        userIdFromCookie,
                        profileModel.getId(),
                        CookieUtils.getCsrfTokenFromCookie(cookie),
                        new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                            @Override
                            public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                                Log.d(TAG, "Unblock success: " + result);
                                fetchProfileDetails();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error unblocking", t);
                            }
                        });
                return true;
            }
            friendshipService.block(
                    userIdFromCookie,
                    profileModel.getId(),
                    CookieUtils.getCsrfTokenFromCookie(cookie),
                    new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                        @Override
                        public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                            Log.d(TAG, "Block success: " + result);
                            fetchProfileDetails();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error blocking", t);
                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        fetchProfileDetails();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        postsAdapter = null;
        if (usernameSettingHandler != null) {
            usernameSettingHandler.removeCallbacks(usernameSettingRunnable);
        }
        // if (postsViewModel != null) {
        //     postsViewModel.getList().postValue(Collections.emptyList());
        // }
        if (highlightsViewModel != null) {
            highlightsViewModel.getList().postValue(Collections.emptyList());
        }
    }

    private void init() {
        if (getArguments() != null) {
            final ProfileFragmentArgs fragmentArgs = ProfileFragmentArgs.fromBundle(getArguments());
            username = fragmentArgs.getUsername();
            setUsernameDelayed();
        }
        if (TextUtils.isEmpty(username) && !isLoggedIn) {
            binding.infoContainer.setVisibility(View.GONE);
            binding.swipeRefreshLayout.setEnabled(false);
            binding.privatePage1.setImageResource(R.drawable.ic_outline_info_24);
            binding.privatePage2.setText(R.string.no_acc);
            final NestedCoordinatorLayout.LayoutParams layoutParams = (NestedCoordinatorLayout.LayoutParams) binding.privatePage.getLayoutParams();
            layoutParams.topMargin = 0;
            binding.privatePage.setLayoutParams(layoutParams);
            binding.privatePage.setVisibility(View.VISIBLE);
            return;
        }
        binding.swipeRefreshLayout.setEnabled(true);
        setupHighlights();
        setupCommonListeners();
        fetchUsername();
    }

    private void fetchUsername() {
        final String uid = CookieUtils.getUserIdFromCookie(cookie);
        if (TextUtils.isEmpty(username) && uid != null) {
            final FetchListener<String> fetchListener = username -> {
                if (TextUtils.isEmpty(username)) return;
                this.username = username;
                setUsernameDelayed();
                fetchProfileDetails();
            };
            boolean found = false;
            final DataBox.CookieModel cookieModel = Utils.dataBox.getCookie(uid);
            if (cookieModel != null) {
                final String username = cookieModel.getUsername();
                if (!TextUtils.isEmpty(username)) {
                    found = true;
                    fetchListener.onResult("@" + username);
                }
            }
            if (!found) {
                // if not in database, fetch info from instagram
                new UsernameFetcher(uid, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            return;
        }
        fetchProfileDetails();
    }

    private void fetchProfileDetails() {
        if (TextUtils.isEmpty(username)) return;
        new ProfileFetcher(username.trim().substring(1), profileModel -> {
            if (getContext() == null) return;
            this.profileModel = profileModel;
            // final String userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
            // final boolean isSelf = isLoggedIn
            //         && profileModel != null
            //         && userIdFromCookie != null
            //         && userIdFromCookie.equals(profileModel.getId());
            // if (favMenuItem != null) {
            //     favMenuItem.setVisible(isSelf);
            // }
            setProfileDetails();

        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setProfileDetails() {
        final Context context = getContext();
        if (context == null) return;
        if (profileModel == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(context, R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!postsSetupDone) {
            setupPosts();
        } else {
            binding.postsRecyclerView.refresh();
        }
        binding.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
        final String profileId = profileModel.getId();
        final String myId = CookieUtils.getUserIdFromCookie(cookie);
        if (isLoggedIn) {
            fetchStoryAndHighlights(profileId);
        }
        setupButtons(profileId, myId);
        if (!profileId.equals(myId)) {
            binding.favCb.setVisibility(View.VISIBLE);
            final boolean isFav = Utils.dataBox.getFavorite(username.substring(1), FavoriteType.USER) != null;
            binding.favCb.setChecked(isFav);
            binding.favCb.setButtonDrawable(isFav ? R.drawable.ic_star_check_24 : R.drawable.ic_outline_star_plus_24);
        } else {
            binding.favCb.setVisibility(View.GONE);
        }
        final ControllerListener<ImageInfo> listener = new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(final String id, final ImageInfo imageInfo, final Animatable animatable) {
                startPostponedEnterTransition();
            }
        };
        binding.mainProfileImage.setImageURI(profileModel.getHdProfilePic());

        final long followersCount = profileModel.getFollowersCount();
        final long followingCount = profileModel.getFollowingCount();

        final String postCount = String.valueOf(profileModel.getPostCount());

        SpannableStringBuilder span = new SpannableStringBuilder(getString(R.string.main_posts_count,
                                                                           postCount));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
        binding.mainPostCount.setText(span);

        final String followersCountStr = String.valueOf(followersCount);
        final int followersCountStrLen = followersCountStr.length();
        span = new SpannableStringBuilder(getString(R.string.main_posts_followers,
                                                    followersCountStr));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, followersCountStrLen, 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, followersCountStrLen, 0);
        binding.mainFollowers.setText(span);

        final String followingCountStr = String.valueOf(followingCount);
        final int followingCountStrLen = followingCountStr.length();
        span = new SpannableStringBuilder(getString(R.string.main_posts_following,
                                                    followingCountStr));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, followingCountStrLen, 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, followingCountStrLen, 0);
        binding.mainFollowing.setText(span);

        binding.mainFullName.setText(TextUtils.isEmpty(profileModel.getName()) ? profileModel.getUsername()
                                                                               : profileModel.getName());

        CharSequence biography = profileModel.getBiography();
        if (TextUtils.hasMentions(biography)) {
            biography = TextUtils.getMentionText(biography);
            binding.mainBiography.setText(biography, TextView.BufferType.SPANNABLE);
            binding.mainBiography.setMentionClickListener(mentionClickListener);
        } else {
            binding.mainBiography.setText(biography);
            binding.mainBiography.setMentionClickListener(null);
        }
        binding.mainBiography.setOnLongClickListener(v -> {
            if (context != null) Utils.copyText(context, profileModel.getBiography());
            return true;
        });

        final String url = profileModel.getUrl();
        if (TextUtils.isEmpty(url)) {
            binding.mainUrl.setVisibility(View.GONE);
        } else {
            binding.mainUrl.setVisibility(View.VISIBLE);
            binding.mainUrl.setText(TextUtils.getSpannableUrl(url));
            binding.mainUrl.setMovementMethod(LinkMovementMethod.getInstance());
        }

        binding.mainFullName.setSelected(true);
        binding.mainBiography.setEnabled(true);
        binding.mainBiography.setClickable(true);

        if (!profileModel.isReallyPrivate()) {
            binding.mainFollowing.setClickable(true);
            binding.mainFollowers.setClickable(true);

            if (isLoggedIn) {
                final View.OnClickListener followClickListener = v -> {
                    final NavDirections action = ProfileFragmentDirections.actionProfileFragmentToFollowViewerFragment(
                            profileId,
                            v == binding.mainFollowers,
                            profileModel.getUsername());
                    NavHostFragment.findNavController(this).navigate(action);
                };

                binding.mainFollowers.setOnClickListener(followersCount > 0 ? followClickListener : null);
                binding.mainFollowing.setOnClickListener(followingCount > 0 ? followClickListener : null);
            }

            binding.swipeRefreshLayout.setRefreshing(true);
            binding.postsRecyclerView.setVisibility(View.VISIBLE);
            fetchPosts();
        } else {
            binding.mainFollowers.setClickable(false);
            binding.mainFollowing.setClickable(false);
            binding.swipeRefreshLayout.setRefreshing(false);
            // error
            binding.privatePage1.setImageResource(R.drawable.lock);
            binding.privatePage2.setText(R.string.priv_acc);
            binding.privatePage.setVisibility(View.VISIBLE);
            binding.postsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void setupButtons(final String profileId, final String myId) {
        if (isLoggedIn) {
            if (profileId.equals(myId)) {
                binding.btnTagged.setVisibility(View.VISIBLE);
                binding.btnSaved.setVisibility(View.VISIBLE);
                binding.btnLiked.setVisibility(View.VISIBLE);
                binding.btnDM.setVisibility(View.GONE);
                binding.btnSaved.setText(R.string.saved);
                return;
            }
            binding.btnTagged.setVisibility(View.GONE);
            binding.btnSaved.setVisibility(View.GONE);
            binding.btnLiked.setVisibility(View.GONE);
            binding.btnDM.setVisibility(View.VISIBLE); // maybe there is a judgment mechanism?
            binding.btnFollow.setVisibility(View.VISIBLE);
            if (profileModel.getFollowing()) {
                binding.btnFollow.setText(R.string.unfollow);
                binding.btnFollow.setIconResource(R.drawable.ic_outline_person_add_disabled_24);
            } else if (profileModel.getRequested()) {
                binding.btnFollow.setText(R.string.cancel);
                binding.btnFollow.setIconResource(R.drawable.ic_outline_person_add_disabled_24);
            } else {
                binding.btnFollow.setText(R.string.follow);
                binding.btnFollow.setIconResource(R.drawable.ic_outline_person_add_24);
            }
            if (restrictMenuItem != null) {
                restrictMenuItem.setVisible(true);
                if (profileModel.getRestricted()) {
                    restrictMenuItem.setTitle(R.string.unrestrict);
                } else {
                    restrictMenuItem.setTitle(R.string.restrict);
                }
            }
            binding.btnTagged.setVisibility(profileModel.isReallyPrivate() ? View.GONE : View.VISIBLE);
            if (blockMenuItem != null) {
                blockMenuItem.setVisible(true);
                if (profileModel.getBlocked()) {
                    blockMenuItem.setTitle(R.string.unblock);
                } else {
                    blockMenuItem.setTitle(R.string.block);
                }
            }
            return;
        }
        if (!profileModel.isReallyPrivate() && restrictMenuItem != null) {
            restrictMenuItem.setVisible(true);
            if (profileModel.getRestricted()) {
                restrictMenuItem.setTitle(R.string.unrestrict);
            } else {
                restrictMenuItem.setTitle(R.string.restrict);
            }
        }
    }

    private void fetchStoryAndHighlights(final String profileId) {
        storiesService.getUserStory(profileId,
                                    profileModel.getUsername(),
                                    false,
                                    false,
                                    false,
                                    new ServiceCallback<List<StoryModel>>() {
                                        @Override
                                        public void onSuccess(final List<StoryModel> storyModels) {
                                            if (storyModels != null && !storyModels.isEmpty()) {
                                                binding.mainProfileImage.setStoriesBorder();
                                                hasStories = true;
                                            }
                                        }

                                        @Override
                                        public void onFailure(final Throwable t) {
                                            Log.e(TAG, "Error", t);
                                        }
                                    });
        new HighlightsFetcher(profileId,
                              result -> {
                                  highlightsFetching = false;
                                  if (result != null) {
                                      binding.highlightsList.setVisibility(View.VISIBLE);
                                      highlightsViewModel.getList().postValue(result);
                                  } else binding.highlightsList.setVisibility(View.GONE);
                              }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setupCommonListeners() {

        final String userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
        // final boolean isSelf = isLoggedIn && profileModel != null && userIdFromCookie != null && userIdFromCookie
        //         .equals(profileModel.getId());
        binding.btnFollow.setOnClickListener(v -> {
            if (profileModel.getFollowing() || profileModel.getRequested()) {
                friendshipService.unfollow(
                        userIdFromCookie,
                        profileModel.getId(),
                        CookieUtils.getCsrfTokenFromCookie(cookie),
                        new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                            @Override
                            public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                                Log.d(TAG, "Unfollow success: " + result);
                                onRefresh();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error unfollowing", t);
                            }
                        });
            } else {
                friendshipService.follow(
                        userIdFromCookie,
                        profileModel.getId(),
                        CookieUtils.getCsrfTokenFromCookie(cookie),
                        new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                            @Override
                            public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                                Log.d(TAG, "Follow success: " + result);
                                onRefresh();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error following", t);
                            }
                        });
            }
        });
        binding.btnSaved.setOnClickListener(v -> {
            final NavDirections action = ProfileFragmentDirections.actionProfileFragmentToSavedViewerFragment(profileModel.getUsername(),
                                                                                                              profileModel.getId(),
                                                                                                              PostItemType.SAVED);
            NavHostFragment.findNavController(this).navigate(action);
        });
        binding.btnLiked.setOnClickListener(v -> {
            final NavDirections action = ProfileFragmentDirections.actionProfileFragmentToSavedViewerFragment(profileModel.getUsername(),
                                                                                                              profileModel.getId(),
                                                                                                              PostItemType.LIKED);
            NavHostFragment.findNavController(this).navigate(action);
        });
        binding.btnTagged.setOnClickListener(v -> {
            final NavDirections action = ProfileFragmentDirections.actionProfileFragmentToSavedViewerFragment(profileModel.getUsername(),
                                                                                                              profileModel.getId(),
                                                                                                              PostItemType.TAGGED);
            NavHostFragment.findNavController(this).navigate(action);
        });
        binding.btnDM.setOnClickListener(v -> {
            new CreateThreadAction(cookie, profileModel.getId(), threadId -> {
                final NavDirections action = ProfileFragmentDirections.actionProfileFragmentToDMThreadFragment(threadId, profileModel.getUsername());
                NavHostFragment.findNavController(this).navigate(action);
            }).execute();
        });
        binding.mainProfileImage.setOnClickListener(v -> {
            if (!hasStories) {
                // show profile pic
                showProfilePicDialog();
                return;
            }
            // show dialog
            final String[] options = {getString(R.string.view_pfp), getString(R.string.show_stories)};
            final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
                if (which == AlertDialog.BUTTON_NEUTRAL) {
                    dialog.dismiss();
                    return;
                }
                if (which == 1) {
                    // show stories
                    final NavDirections action = ProfileFragmentDirections
                            .actionProfileFragmentToStoryViewerFragment(-1, null, false, false, profileModel.getId(), username);
                    NavHostFragment.findNavController(this).navigate(action);
                    return;
                }
                showProfilePicDialog();
            };
            final Context context = getContext();
            if (context == null) return;
            new AlertDialog.Builder(context)
                    .setItems(options, profileDialogListener)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
        binding.favCb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // do not do anything if state matches the db, as listener is set before profile details are set
            final String finalUsername = username.startsWith("@") ? username.substring(1) : username;
            final DataBox.FavoriteModel favorite = Utils.dataBox.getFavorite(finalUsername, FavoriteType.USER);
            if ((isChecked && favorite != null) || (!isChecked && favorite == null)) {
                return;
            }
            buttonView.setVisibility(View.GONE);
            binding.favProgress.setVisibility(View.VISIBLE);
            final String message;
            if (isChecked) {
                final DataBox.FavoriteModel model = new DataBox.FavoriteModel(
                        -1,
                        finalUsername,
                        FavoriteType.USER,
                        profileModel.getName(),
                        profileModel.getSdProfilePic(),
                        new Date()
                );
                Utils.dataBox.addOrUpdateFavorite(model);
                binding.favCb.setButtonDrawable(R.drawable.ic_star_check_24);
                message = getString(R.string.added_to_favs);
            } else {
                Utils.dataBox.deleteFavorite(finalUsername, FavoriteType.USER);
                message = getString(R.string.removed_from_favs);
                binding.favCb.setButtonDrawable(R.drawable.ic_outline_star_plus_24);
            }
            final Snackbar snackbar = Snackbar.make(root, message, BaseTransientBottomBar.LENGTH_LONG);
            snackbar.setAction(R.string.ok, v -> snackbar.dismiss())
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAnchorView(fragmentActivity.getBottomNavView())
                    .show();
            binding.favProgress.setVisibility(View.GONE);
            binding.favCb.setVisibility(View.VISIBLE);
        });
    }

    private void showProfilePicDialog() {
        final FragmentManager fragmentManager = getParentFragmentManager();
        final ProfilePicDialogFragment fragment = new ProfilePicDialogFragment(profileModel.getId(), username, profileModel.getHdProfilePic());
        final FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .add(fragment, "profilePicDialog")
          .commit();
    }

    private void setUsernameDelayed() {
        if (usernameSettingHandler == null) {
            usernameSettingHandler = new Handler(Looper.getMainLooper());
        }
        usernameSettingHandler.postDelayed(usernameSettingRunnable, 200);
    }

    private void setupPosts() {
        binding.postsRecyclerView.setViewModelStoreOwner(this)
                                 .setLifeCycleOwner(this)
                                 .setPostFetchService(new ProfilePostFetchService(profileModel))
                                 .setLayoutPreferences(PostsLayoutPreferences.fromJson(settingsHelper.getString(Constants.PREF_PROFILE_POSTS_LAYOUT)))
                                 .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
                                 .setFeedItemCallback(feedItemCallback)
                                 .init();
        binding.swipeRefreshLayout.setRefreshing(true);
        postsSetupDone = true;
        // postsAdapter = new PostsAdapter((postModel, position) -> {
        //     if (postsAdapter.isSelecting()) {
        //         if (actionMode == null) return;
        //         final String title = getString(R.string.number_selected,
        //                                        postsAdapter.getSelectedModels().size());
        //         actionMode.setTitle(title);
        //         return;
        //     }
        //     if (checkAndResetAction()) return;
        //     final List<PostModel> postModels = postsViewModel.getList().getValue();
        //     if (postModels == null || postModels.size() == 0) return;
        //     if (postModels.get(0) == null) return;
        //     final String postId = isLoggedIn ? postModels.get(0).getPostId() : postModels.get(0).getShortCode();
        //     final boolean isId = isLoggedIn && postId != null;
        //     final String[] idsOrShortCodes = new String[postModels.size()];
        //     for (int i = 0; i < postModels.size(); i++) {
        //         idsOrShortCodes[i] = isId ? postModels.get(i).getPostId()
        //                                   : postModels.get(i).getShortCode();
        //     }
        //     final NavDirections action = ProfileFragmentDirections.actionGlobalPostViewFragment(
        //             position,
        //             idsOrShortCodes,
        //             isId);
        //     NavHostFragment.findNavController(this).navigate(action);
        //
        // }, (model, position) -> {
        //     if (!postsAdapter.isSelecting()) {
        //         checkAndResetAction();
        //         return true;
        //     }
        //     if (onBackPressedCallback.isEnabled()) {
        //         return true;
        //     }
        //     final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
        //     onBackPressedCallback.setEnabled(true);
        //     actionMode = fragmentActivity.startActionMode(multiSelectAction);
        //     final String title = getString(R.string.number_selected, 1);
        //     actionMode.setTitle(title);
        //     onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        //     return true;
        // });
    }

    private void updateSwipeRefreshState() {
        binding.swipeRefreshLayout.setRefreshing(binding.postsRecyclerView.isFetching() || highlightsFetching);
    }

    private void setupHighlights() {
        highlightsViewModel = new ViewModelProvider(fragmentActivity).get(HighlightsViewModel.class);
        highlightsAdapter = new HighlightsAdapter((model, position) -> {
            final NavDirections action = ProfileFragmentDirections
                    .actionProfileFragmentToStoryViewerFragment(position, model.getTitle(), false, false, null, null);
            NavHostFragment.findNavController(this).navigate(action);
        });
        final Context context = getContext();
        if (context == null) return;
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
        binding.highlightsList.setLayoutManager(layoutManager);
        binding.highlightsList.setAdapter(highlightsAdapter);
        highlightsViewModel.getList().observe(getViewLifecycleOwner(), highlightModels -> highlightsAdapter.submitList(highlightModels));
    }

    private void fetchPosts() {
        // stopCurrentExecutor();
        binding.swipeRefreshLayout.setRefreshing(true);
        // currentlyExecuting = new PostsFetcher(profileModel.getId(), PostItemType.MAIN, endCursor, postsFetchListener)
        //         .setUsername(profileModel.getUsername())
        //         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    private void navigateToProfile(final String username) {
        final NavController navController = NavHostFragment.findNavController(this);
        final Bundle bundle = new Bundle();
        bundle.putString("username", username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
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

    private void showPostsLayoutPreferences() {
        final PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_PROFILE_POSTS_LAYOUT,
                preferences -> new Handler().postDelayed(() -> binding.postsRecyclerView.setLayoutPreferences(preferences), 200));
        fragment.show(getChildFragmentManager(), "posts_layout_preferences");
    }
}
