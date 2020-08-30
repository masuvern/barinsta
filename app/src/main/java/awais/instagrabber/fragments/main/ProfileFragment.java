package awais.instagrabber.fragments.main;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.activities.FollowViewer;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.activities.PostViewer;
import awais.instagrabber.activities.SavedViewer;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.HighlightsFetcher;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.asyncs.UsernameFetcher;
import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.PrimaryActionModeCallback.CallbacksHelper;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentProfileBinding;
import awais.instagrabber.fragments.main.viewmodels.PostsViewModel;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoRestrictRootResponse;
import awais.instagrabber.services.FriendshipService;
import awais.instagrabber.services.ServiceCallback;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentProfileBinding binding;
    private boolean isLoggedIn;
    private String cookie;
    private String username;
    private ProfileModel profileModel;
    private PostsViewModel postsViewModel;
    private PostsAdapter postsAdapter;
    private ActionMode actionMode;
    private Handler usernameSettingHandler;
    private FriendshipService friendshipService;
    private boolean shouldRefresh = true;
    private StoryModel[] storyModels;
    private boolean hasNextPage;
    private String endCursor;
    private AsyncTask<Void, Void, PostModel[]> currentlyExecuting;
    ;
    private final Runnable usernameSettingRunnable = () -> {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(username.substring(1));
        }
    };
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
                        Utils.batchDownload(requireContext(),
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
            binding.swipeRefreshLayout.setRefreshing(false);
            if (result != null) {
                binding.mainPosts.post(() -> binding.mainPosts.setVisibility(View.VISIBLE));
                // final int oldSize = mainActivity.allItems.size();
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
                return;
            }
            binding.privatePage1.setImageResource(R.drawable.ic_cancel);
            binding.privatePage2.setText(R.string.empty_acc);
            binding.privatePage.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        friendshipService = FriendshipService.getInstance();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            if (getArguments() != null) {
                final ProfileFragmentArgs fragmentArgs = ProfileFragmentArgs.fromBundle(getArguments());
                if (!fragmentArgs.getUsername().equals(username)) {
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
        init();
        shouldRefresh = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (usernameSettingHandler != null) {
            usernameSettingHandler.removeCallbacks(usernameSettingRunnable);
        }
        if (postsViewModel != null) {
            postsViewModel.getList().postValue(Collections.emptyList());
        }
    }

    private void init() {
        cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !Utils.isEmpty(cookie) && Utils.getUserIdFromCookie(cookie) != null;
        if (getArguments() != null) {
            final ProfileFragmentArgs fragmentArgs = ProfileFragmentArgs.fromBundle(getArguments());
            username = fragmentArgs.getUsername();
            setUsernameDelayed();
        }
        if (!isLoggedIn) {
            binding.privatePage1.setImageResource(R.drawable.ic_outline_info_24);
            binding.privatePage2.setText(R.string.no_acc);
            binding.privatePage.setVisibility(View.VISIBLE);
            return;
        }
        setupPosts();
        setupCommonListeners();
        fetchUsername();
    }

    private void fetchUsername() {
        final String uid = Utils.getUserIdFromCookie(cookie);
        if (username == null && uid != null) {
            final FetchListener<String> fetchListener = username -> {
                if (Utils.isEmpty(username)) return;
                this.username = username;
                setUsernameDelayed();
                fetchProfileDetails();
                // adds cookies to database for quick access
                final DataBox.CookieModel cookieModel = Utils.dataBox.getCookie(uid);
                if (Utils.dataBox.getCookieCount() == 0 || cookieModel == null || Utils.isEmpty(cookieModel.getUsername()))
                    Utils.dataBox.addUserCookie(new DataBox.CookieModel(uid, username, cookie));
            };
            boolean found = false;
            final DataBox.CookieModel cookieModel = Utils.dataBox.getCookie(uid);
            if (cookieModel != null) {
                final String username = cookieModel.getUsername();
                if (username != null) {
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
        new ProfileFetcher(username.substring(1), profileModel -> {
            this.profileModel = profileModel;
            setProfileDetails();

        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setProfileDetails() {
        if (profileModel == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(requireContext(), R.string.error_loading_profile, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.isVerified.setVisibility(profileModel.isVerified() ? View.VISIBLE : View.GONE);
        final String profileId = profileModel.getId();
        if (settingsHelper.getBoolean(Constants.STORIESIG)) {
            new iStoryStatusFetcher(
                    profileId,
                    profileModel.getUsername(),
                    false,
                    false,
                    (!isLoggedIn && settingsHelper.getBoolean(Constants.STORIESIG)),
                    false,
                    result -> {
                        storyModels = result;
                        if (result != null && result.length > 0) {
                            binding.mainProfileImage.setStoriesBorder();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            new HighlightsFetcher(profileId, (!isLoggedIn && settingsHelper.getBoolean(Constants.STORIESIG)), result -> {
                if (result != null && result.length > 0) {
                    binding.highlightsList.setVisibility(View.VISIBLE);
                    // highlightsAdapter.setData(result);
                } else
                    binding.highlightsList.setVisibility(View.GONE);
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (isLoggedIn) {
            final String myId = Utils.getUserIdFromCookie(cookie);
            if (profileId.equals(myId)) {
                binding.btnTagged.setVisibility(View.VISIBLE);
                binding.btnSaved.setVisibility(View.VISIBLE);
                binding.btnLiked.setVisibility(View.VISIBLE);
                binding.btnSaved.setText(R.string.saved);
                ViewCompat.setBackgroundTintList(
                        binding.btnSaved,
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_orange_background)));
            } else {
                binding.btnTagged.setVisibility(View.GONE);
                binding.btnSaved.setVisibility(View.GONE);
                binding.btnLiked.setVisibility(View.GONE);
                binding.btnFollow.setVisibility(View.VISIBLE);
                if (profileModel.getFollowing()) {
                    binding.btnFollow.setText(R.string.unfollow);
                    ViewCompat.setBackgroundTintList(
                            binding.btnFollow,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_purple_background)));
                } else if (profileModel.getRequested()) {
                    binding.btnFollow.setText(R.string.cancel);
                    ViewCompat.setBackgroundTintList(
                            binding.btnFollow,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_purple_background)));
                } else {
                    binding.btnFollow.setText(R.string.follow);
                    ViewCompat.setBackgroundTintList(
                            binding.btnFollow,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_pink_background)));
                }
                binding.btnRestrict.setVisibility(View.VISIBLE);
                if (profileModel.getRestricted()) {
                    binding.btnRestrict.setText(R.string.unrestrict);
                    ViewCompat.setBackgroundTintList(
                            binding.btnRestrict,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_green_background)));
                } else {
                    binding.btnRestrict.setText(R.string.restrict);
                    ViewCompat.setBackgroundTintList(
                            binding.btnRestrict,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_orange_background)));
                }
                binding.btnBlock.setVisibility(View.VISIBLE);
                binding.btnTagged.setVisibility(View.VISIBLE);
                if (profileModel.getBlocked()) {
                    binding.btnBlock.setText(R.string.unblock);
                    ViewCompat.setBackgroundTintList(
                            binding.btnBlock,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_green_background)));
                } else {
                    binding.btnBlock.setText(R.string.block);
                    ViewCompat.setBackgroundTintList(
                            binding.btnBlock,
                            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_red_background)));
                }
            }
        } else {
            if (Utils.dataBox.getFavorite(username) != null) {
                binding.btnFollow.setText(R.string.unfavorite_short);
                ViewCompat.setBackgroundTintList(
                        binding.btnFollow,
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_purple_background)));
            } else {
                binding.btnFollow.setText(R.string.favorite_short);
                ViewCompat.setBackgroundTintList(
                        binding.btnFollow,
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_pink_background)));
            }
            binding.btnFollow.setVisibility(View.VISIBLE);
            if (!profileModel.isReallyPrivate()) {
                binding.btnRestrict.setVisibility(View.VISIBLE);
                binding.btnRestrict.setText(R.string.tagged);
                ViewCompat.setBackgroundTintList(
                        binding.btnRestrict,
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_blue_background)));
            }
        }

        binding.mainProfileImage.setImageURI(profileModel.getSdProfilePic());

        final long followersCount = profileModel.getFollowersCount();
        final long followingCount = profileModel.getFollowingCount();

        final String postCount = String.valueOf(profileModel.getPostCount());

        SpannableStringBuilder span = new SpannableStringBuilder(getString(R.string.main_posts_count, postCount));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
        binding.mainPostCount.setText(span);

        final String followersCountStr = String.valueOf(followersCount);
        final int followersCountStrLen = followersCountStr.length();
        span = new SpannableStringBuilder(getString(R.string.main_posts_followers, followersCountStr));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, followersCountStrLen, 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, followersCountStrLen, 0);
        binding.mainFollowers.setText(span);

        final String followingCountStr = String.valueOf(followingCount);
        final int followingCountStrLen = followingCountStr.length();
        span = new SpannableStringBuilder(getString(R.string.main_posts_following, followingCountStr));
        span.setSpan(new RelativeSizeSpan(1.2f), 0, followingCountStrLen, 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, followingCountStrLen, 0);
        binding.mainFollowing.setText(span);

        binding.mainFullName.setText(Utils.isEmpty(profileModel.getName()) ? profileModel.getUsername() : profileModel.getName());

        CharSequence biography = profileModel.getBiography();
        binding.mainBiography.setCaptionIsExpandable(true);
        binding.mainBiography.setCaptionIsExpanded(true);
        if (Utils.hasMentions(biography)) {
            biography = Utils.getMentionText(biography);
            binding.mainBiography.setText(biography, TextView.BufferType.SPANNABLE);
            // binding.mainBiography.setMentionClickListener(mentionClickListener);
        } else {
            binding.mainBiography.setText(biography);
            binding.mainBiography.setMentionClickListener(null);
        }

        final String url = profileModel.getUrl();
        if (Utils.isEmpty(url)) {
            binding.mainUrl.setVisibility(View.GONE);
        } else {
            binding.mainUrl.setVisibility(View.VISIBLE);
            binding.mainUrl.setText(Utils.getSpannableUrl(url));
        }

        binding.mainFullName.setSelected(true);
        binding.mainBiography.setEnabled(true);

        if (!profileModel.isReallyPrivate()) {
            binding.mainFollowing.setClickable(true);
            binding.mainFollowers.setClickable(true);

            if (isLoggedIn) {
                final View.OnClickListener followClickListener = v -> startActivity(new Intent(requireContext(), FollowViewer.class)
                        .putExtra(Constants.EXTRAS_FOLLOWERS, v == binding.mainFollowers)
                        .putExtra(Constants.EXTRAS_NAME, profileModel.getUsername())
                        .putExtra(Constants.EXTRAS_ID, profileId));

                binding.mainFollowers.setOnClickListener(followersCount > 0 ? followClickListener : null);
                binding.mainFollowing.setOnClickListener(followingCount > 0 ? followClickListener : null);
            }

            if (profileModel.getPostCount() == 0) {
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.privatePage1.setImageResource(R.drawable.ic_cancel);
                binding.privatePage2.setText(R.string.empty_acc);
                binding.privatePage.setVisibility(View.VISIBLE);
            } else {
                binding.swipeRefreshLayout.setRefreshing(true);
                binding.mainPosts.setVisibility(View.VISIBLE);
                fetchPosts();
            }
        } else {
            binding.mainFollowers.setClickable(false);
            binding.mainFollowing.setClickable(false);
            binding.swipeRefreshLayout.setRefreshing(false);
            // error
            binding.privatePage1.setImageResource(R.drawable.lock);
            binding.privatePage2.setText(R.string.priv_acc);
            binding.privatePage.setVisibility(View.VISIBLE);
            binding.mainPosts.setVisibility(View.GONE);
        }
    }

    private void setupCommonListeners() {

        final String userIdFromCookie = Utils.getUserIdFromCookie(cookie);
        final boolean isSelf = isLoggedIn
                && profileModel != null
                && userIdFromCookie != null
                && userIdFromCookie.equals(profileModel.getId());
        final String favorite = Utils.dataBox.getFavorite(username);

        binding.btnFollow.setOnClickListener(v -> {
            if (!isLoggedIn) {
                if (favorite != null && v == binding.btnFollow) {
                    Utils.dataBox.delFavorite(
                            new DataBox.FavoriteModel(username,
                                    Long.parseLong(favorite.split("/")[1]),
                                    username.replaceAll("^@", "")
                            )
                    );
                } else if (v == binding.btnFollow) {
                    Utils.dataBox.addFavorite(
                            new DataBox.FavoriteModel(username, System.currentTimeMillis(),
                                    username.replaceAll("^@", "")));
                }
                fetchProfileDetails();
                return;
            }
            if (profileModel.getFollowing() || profileModel.getRequested()) {
                friendshipService.unfollow(
                        userIdFromCookie,
                        profileModel.getId(),
                        Utils.getCsrfTokenFromCookie(cookie),
                        new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                            @Override
                            public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                                Log.d(TAG, "Unfollow success: " + result);
                                fetchProfileDetails();
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
                        Utils.getCsrfTokenFromCookie(cookie),
                        new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                            @Override
                            public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                                Log.d(TAG, "Follow success: " + result);
                                fetchProfileDetails();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error following", t);
                            }
                        });
            }
        });

        binding.btnRestrict.setOnClickListener(v -> {
            if (!isLoggedIn) return;
            final String action = profileModel.getRestricted() ? "Unrestrict" : "Restrict";
            friendshipService.toggleRestrict(
                    profileModel.getId(),
                    !profileModel.getRestricted(),
                    Utils.getCsrfTokenFromCookie(cookie),
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
        });
        binding.btnBlock.setOnClickListener(v -> {
            if (!isLoggedIn) return;
            if (profileModel.getBlocked()) {
                friendshipService.unblock(
                        userIdFromCookie,
                        profileModel.getId(),
                        Utils.getCsrfTokenFromCookie(cookie),
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
                return;
            }
            friendshipService.block(
                    userIdFromCookie,
                    profileModel.getId(),
                    Utils.getCsrfTokenFromCookie(cookie),
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
        });
        binding.btnSaved.setOnClickListener(v -> startActivity(new Intent(requireContext(), SavedViewer.class)
                .putExtra(Constants.EXTRAS_INDEX, "$" + profileModel.getId())
                .putExtra(Constants.EXTRAS_USER, "@" + profileModel.getUsername())
        ));
        binding.btnLiked.setOnClickListener(v -> startActivity(new Intent(requireContext(), SavedViewer.class)
                .putExtra(Constants.EXTRAS_INDEX, "^" + profileModel.getId())
                .putExtra(Constants.EXTRAS_USER, "@" + profileModel.getUsername())
        ));

        binding.btnTagged.setOnClickListener(v -> startActivity(new Intent(requireContext(), SavedViewer.class)
                .putExtra(Constants.EXTRAS_INDEX, "%" + profileModel.getId())
                .putExtra(Constants.EXTRAS_USER, "@" + profileModel.getUsername())
        ));
        // binding.btnFollowTag.setOnClickListener(profileActionListener);
    }

    private void setUsernameDelayed() {
        if (usernameSettingHandler == null) {
            usernameSettingHandler = new Handler(Looper.getMainLooper());
        }
        usernameSettingHandler.postDelayed(usernameSettingRunnable, 200);
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
                    .putExtra(Constants.EXTRAS_USER, username)
                    .putExtra(Constants.EXTRAS_TYPE, ItemGetType.MAIN_ITEMS));

        }, (model, position) -> {
            if (!postsAdapter.isSelecting()) {
                checkAndResetAction();
                return true;
            }
            final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                return true;
            }
            actionMode = fragmentActivity.startActionMode(multiSelectAction);
            final String title = getString(R.string.number_selected, 1);
            actionMode.setTitle(title);
            onBackPressedDispatcher.addCallback(onBackPressedCallback);
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

    private void fetchPosts() {
        stopCurrentExecutor();
        currentlyExecuting = new PostsFetcher(profileModel.getId(), endCursor, postsFetchListener)
                .setUsername(profileModel.getUsername())
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    private boolean checkAndResetAction() {
        final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
        if (!onBackPressedDispatcher.hasEnabledCallbacks() || actionMode == null) {
            return false;
        }
        actionMode.finish();
        actionMode = null;
        return true;
    }
}
