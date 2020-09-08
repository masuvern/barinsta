package awais.instagrabber.fragments.main;

import android.content.DialogInterface;
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
import android.view.Menu;
import android.view.MenuInflater;
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
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.ProfileNavGraphDirections;
import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.HighlightsAdapter;
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
import awais.instagrabber.dialogs.ProfilePicDialogFragment;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoRestrictRootResponse;
import awais.instagrabber.services.FriendshipService;
import awais.instagrabber.services.ServiceCallback;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.HighlightsViewModel;
import awais.instagrabber.viewmodels.PostsViewModel;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class ProfileFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
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
    private MenuItem favMenuItem;
    private boolean isPullToRefresh;
    private HighlightsAdapter highlightsAdapter;

    private final Runnable usernameSettingRunnable = () -> {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null && !Utils.isEmpty(username)) {
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
            if (result == null || result.length <= 0) {
                binding.privatePage1.setImageResource(R.drawable.ic_cancel);
                binding.privatePage2.setText(R.string.empty_acc);
                binding.privatePage.setVisibility(View.VISIBLE);
                return;
            }
            binding.mainPosts.post(() -> binding.mainPosts.setVisibility(View.VISIBLE));
            final List<PostModel> postModels = postsViewModel.getList().getValue();
            List<PostModel> finalList = postModels == null || postModels.isEmpty() ? new ArrayList<>()
                                                                                   : new ArrayList<>(postModels);
            final List<PostModel> resultList = Arrays.asList(result);
            if (isPullToRefresh) {
                finalList = resultList;
                isPullToRefresh = false;
            } else {
                finalList.addAll(resultList);
            }
            postsViewModel.getList().postValue(finalList);
            final PostModel lastPostModel = result[result.length - 1];
            if (lastPostModel == null) return;
            endCursor = lastPostModel.getEndCursor();
            hasNextPage = lastPostModel.hasNextPage();
            lastPostModel.setPageCursor(false, null);
        }
    };
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
    private HighlightsViewModel highlightsViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        friendshipService = FriendshipService.getInstance();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            if (getArguments() != null) {
                final ProfileFragmentArgs fragmentArgs = ProfileFragmentArgs.fromBundle(getArguments());
                final String username = fragmentArgs.getUsername();
                if (Utils.isEmpty(username) && profileModel != null) {
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
        favMenuItem = menu.findItem(R.id.favourites);
    }

    @Override
    public void onRefresh() {
        isPullToRefresh = true;
        endCursor = null;
        fetchProfileDetails();
        fetchPosts();
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
        if (highlightsViewModel != null) {
            highlightsViewModel.getList().postValue(Collections.emptyList());
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
        if (Utils.isEmpty(username) && !isLoggedIn) {
            binding.privatePage1.setImageResource(R.drawable.ic_outline_info_24);
            binding.privatePage2.setText(R.string.no_acc);
            binding.privatePage.setVisibility(View.VISIBLE);
            return;
        }
        setupPosts();
        setupHighlights();
        setupCommonListeners();
        fetchUsername();
    }

    private void fetchUsername() {
        final String uid = Utils.getUserIdFromCookie(cookie);
        if (Utils.isEmpty(username) && uid != null) {
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
                if (!Utils.isEmpty(username)) {
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
            final String userIdFromCookie = Utils.getUserIdFromCookie(cookie);
            final boolean isSelf = isLoggedIn
                    && profileModel != null
                    && userIdFromCookie != null
                    && userIdFromCookie.equals(profileModel.getId());
            favMenuItem.setVisible(isSelf);
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
        if (settingsHelper.getBoolean(Constants.STORIESIG) || isLoggedIn) {
            new iStoryStatusFetcher(profileId,
                                    profileModel.getUsername(),
                                    false,
                                    false,
                                    !isLoggedIn && settingsHelper.getBoolean(Constants.STORIESIG),
                                    false,
                                    result -> {
                                        storyModels = result;
                                        if (result != null && result.length > 0) {
                                            binding.mainProfileImage.setStoriesBorder();
                                        }
                                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new HighlightsFetcher(profileId,
                                  !isLoggedIn && settingsHelper.getBoolean(Constants.STORIESIG),
                                  result -> {
                                      if (result != null) {
                                          binding.highlightsList.setVisibility(View.VISIBLE);
                                          highlightsViewModel.getList().postValue(result);
                                      } else binding.highlightsList.setVisibility(View.GONE);
                                  }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (isLoggedIn) {
            final String myId = Utils.getUserIdFromCookie(cookie);
            if (profileId.equals(myId)) {
                binding.btnTagged.setVisibility(View.VISIBLE);
                binding.btnSaved.setVisibility(View.VISIBLE);
                binding.btnLiked.setVisibility(View.VISIBLE);
                binding.btnSaved.setText(R.string.saved);
                ViewCompat.setBackgroundTintList(binding.btnSaved,
                                                 ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_orange_background)));
            } else {
                binding.btnTagged.setVisibility(View.GONE);
                binding.btnSaved.setVisibility(View.GONE);
                binding.btnLiked.setVisibility(View.GONE);
                binding.btnFollow.setVisibility(View.VISIBLE);
                if (profileModel.getFollowing()) {
                    binding.btnFollow.setText(R.string.unfollow);
                    ViewCompat.setBackgroundTintList(binding.btnFollow,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_purple_background)));
                } else if (profileModel.getRequested()) {
                    binding.btnFollow.setText(R.string.cancel);
                    ViewCompat.setBackgroundTintList(binding.btnFollow,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_purple_background)));
                } else {
                    binding.btnFollow.setText(R.string.follow);
                    ViewCompat.setBackgroundTintList(binding.btnFollow,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_pink_background)));
                }
                binding.btnRestrict.setVisibility(View.VISIBLE);
                if (profileModel.getRestricted()) {
                    binding.btnRestrict.setText(R.string.unrestrict);
                    ViewCompat.setBackgroundTintList(binding.btnRestrict,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_green_background)));
                } else {
                    binding.btnRestrict.setText(R.string.restrict);
                    ViewCompat.setBackgroundTintList(binding.btnRestrict,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_orange_background)));
                }
                binding.btnBlock.setVisibility(View.VISIBLE);
                binding.btnTagged.setVisibility(View.VISIBLE);
                if (profileModel.getBlocked()) {
                    binding.btnBlock.setText(R.string.unblock);
                    ViewCompat.setBackgroundTintList(binding.btnBlock,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_green_background)));
                } else {
                    binding.btnBlock.setText(R.string.block);
                    ViewCompat.setBackgroundTintList(binding.btnBlock,
                                                     ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_red_background)));
                }
            }
        } else {
            if (Utils.dataBox.getFavorite(username) != null) {
                binding.btnFollow.setText(R.string.unfavorite_short);
                ViewCompat.setBackgroundTintList(binding.btnFollow,
                                                 ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_purple_background)));
            } else {
                binding.btnFollow.setText(R.string.favorite_short);
                ViewCompat.setBackgroundTintList(binding.btnFollow,
                                                 ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_pink_background)));
            }
            binding.btnFollow.setVisibility(View.VISIBLE);
            if (!profileModel.isReallyPrivate()) {
                binding.btnRestrict.setVisibility(View.VISIBLE);
                binding.btnRestrict.setText(R.string.tagged);
                ViewCompat.setBackgroundTintList(binding.btnRestrict,
                                                 ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_blue_background)));
            }
        }

        binding.mainProfileImage.setImageURI(profileModel.getSdProfilePic());

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

        binding.mainFullName.setText(Utils.isEmpty(profileModel.getName()) ? profileModel.getUsername()
                                                                           : profileModel.getName());

        CharSequence biography = profileModel.getBiography();
        binding.mainBiography.setCaptionIsExpandable(true);
        binding.mainBiography.setCaptionIsExpanded(true);
        if (Utils.hasMentions(biography)) {
            biography = Utils.getMentionText(biography);
            binding.mainBiography.setText(biography, TextView.BufferType.SPANNABLE);
            binding.mainBiography.setMentionClickListener(mentionClickListener);
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
        // final boolean isSelf = isLoggedIn && profileModel != null && userIdFromCookie != null && userIdFromCookie
        //         .equals(profileModel.getId());
        final String favorite = Utils.dataBox.getFavorite(username);
        binding.btnFollow.setOnClickListener(v -> {
            if (!isLoggedIn) {
                if (favorite != null && v == binding.btnFollow) {
                    Utils.dataBox.delFavorite(new DataBox.FavoriteModel(
                            username,
                            Long.parseLong(favorite.split("/")[1]),
                            username.replaceAll("^@", "")));
                } else if (v == binding.btnFollow) {
                    Utils.dataBox.addFavorite(new DataBox.FavoriteModel(
                            username,
                            System.currentTimeMillis(),
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
        binding.btnSaved.setOnClickListener(v -> {
            // startActivity(new Intent(requireContext(), SavedViewerFragment.class)
            //                       .putExtra(Constants.EXTRAS_INDEX, "$" + profileModel.getId())
            //                       .putExtra(Constants.EXTRAS_USER, "@" + profileModel.getUsername()));
            final NavDirections action = ProfileFragmentDirections.actionProfileFragmentToSavedViewerFragment(profileModel.getUsername(),
                                                                                                              profileModel.getId(),
                                                                                                              PostItemType.SAVED);
            NavHostFragment.findNavController(this).navigate(action);
        });
        binding.btnLiked.setOnClickListener(v -> {
            // startActivity(new Intent(requireContext(), SavedViewerFragment.class)
            //                       .putExtra(Constants.EXTRAS_INDEX, "^" + profileModel.getId())
            //                       .putExtra(Constants.EXTRAS_USER, username));
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
        binding.mainProfileImage.setOnClickListener(v -> {
            if (storyModels == null || storyModels.length <= 0) {
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
                            .actionProfileFragmentToStoryViewerFragment(-1, null, false, profileModel.getId(), username);
                    NavHostFragment.findNavController(this).navigate(action);
                    return;
                }
                showProfilePicDialog();
            };
            new AlertDialog.Builder(requireContext())
                    .setItems(options, profileDialogListener)
                    .setNeutralButton(R.string.cancel, null)
                    .show();
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
        postsViewModel = new ViewModelProvider(this).get(PostsViewModel.class);
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(requireContext(), Utils.convertDpToPx(110));
        binding.mainPosts.setLayoutManager(layoutManager);
        binding.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        postsAdapter = new PostsAdapter((postModel, position) -> {
            if (postsAdapter.isSelecting()) {
                if (actionMode == null) return;
                final String title = getString(R.string.number_selected,
                                               postsAdapter.getSelectedModels().size());
                actionMode.setTitle(title);
                return;
            }
            if (checkAndResetAction()) return;
            final List<PostModel> postModels = postsViewModel.getList().getValue();
            if (postModels == null || postModels.size() == 0) return;
            if (postModels.get(0) == null) return;
            final String postId = isLoggedIn ? postModels.get(0).getPostId() : postModels.get(0).getShortCode();
            final boolean isId = isLoggedIn && postId != null;
            final String[] idsOrShortCodes = new String[postModels.size()];
            for (int i = 0; i < postModels.size(); i++) {
                idsOrShortCodes[i] = isId ? postModels.get(i).getPostId()
                                          : postModels.get(i).getShortCode();
            }
            final NavDirections action = ProfileFragmentDirections.actionGlobalPostViewFragment(
                    position,
                    idsOrShortCodes,
                    isId);
            NavHostFragment.findNavController(this).navigate(action);

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

    private void setupHighlights() {
        highlightsViewModel = new ViewModelProvider(fragmentActivity).get(HighlightsViewModel.class);
        highlightsAdapter = new HighlightsAdapter((model, position) -> {
            final NavDirections action = ProfileFragmentDirections
                    .actionProfileFragmentToStoryViewerFragment(position, model.getTitle(), false, null, null);
            NavHostFragment.findNavController(this).navigate(action);
        });
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false);
        binding.highlightsList.setLayoutManager(layoutManager);
        binding.highlightsList.setAdapter(highlightsAdapter);
        highlightsViewModel.getList().observe(getViewLifecycleOwner(), highlightModels -> highlightsAdapter.submitList(highlightModels));
    }

    private void fetchPosts() {
        stopCurrentExecutor();
        binding.swipeRefreshLayout.setRefreshing(true);
        currentlyExecuting = new PostsFetcher(profileModel.getId(), PostItemType.MAIN, endCursor, postsFetchListener)
                .setUsername(profileModel.getUsername())
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void stopCurrentExecutor() {
        if (currentlyExecuting != null) {
            try {
                currentlyExecuting.cancel(true);
            } catch (final Exception e) {
                if (logCollector != null) logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
                Log.e(TAG, "", e);
            }
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
