package awais.instagrabber.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.asyncs.CommentsFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentCommentsBinding;
import awais.instagrabber.dialogs.ProfilePicDialogFragment;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.CommentsViewModel;
import awais.instagrabber.webservices.MediaService;
import awais.instagrabber.webservices.ServiceCallback;

import static android.content.Context.INPUT_METHOD_SERVICE;

public final class CommentsViewerFragment extends BottomSheetDialogFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "CommentsViewerFragment";

    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);

    private CommentsAdapter commentsAdapter;
    private FragmentCommentsBinding binding;
    private LinearLayoutManager layoutManager;
    private RecyclerLazyLoader lazyLoader;
    private String shortCode, userId, endCursor = null;
    private Resources resources;
    private InputMethodManager imm;
    private AppCompatActivity fragmentActivity;
    private LinearLayoutCompat root;
    private boolean shouldRefresh = true;
    private MediaService mediaService;
    private String postId;
    private AsyncTask<Void, Void, List<CommentModel>> currentlyRunning;
    private CommentsViewModel commentsViewModel;

    private final FetchListener<List<CommentModel>> fetchListener = new FetchListener<List<CommentModel>>() {
        @Override
        public void doBefore() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final List<CommentModel> commentModels) {
            if (commentModels != null && commentModels.size() > 0) {
                endCursor = commentModels.get(0).getEndCursor();
                List<CommentModel> list = commentsViewModel.getList().getValue();
                list = list != null ? new LinkedList<>(list) : new LinkedList<>();
                // final int oldSize = list != null ? list.size() : 0;
                list.addAll(commentModels);
                commentsViewModel.getList().postValue(list);
            }
            binding.swipeRefreshLayout.setRefreshing(false);
            stopCurrentExecutor();
        }
    };

    private final CommentsAdapter.CommentCallback commentCallback = new CommentsAdapter.CommentCallback() {
        @Override
        public void onClick(final CommentModel comment) {
            onCommentClick(comment);
        }

        @Override
        public void onHashtagClick(final String hashtag) {
            final NavDirections action = CommentsViewerFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(CommentsViewerFragment.this).navigate(action);
        }

        @Override
        public void onMentionClick(final String mention) {
            openProfile(mention);
        }

        @Override
        public void onURLClick(final String url) {
            Utils.openURL(getContext(), url);
        }

        @Override
        public void onEmailClick(final String emailAddress) {
            Utils.openEmailAddress(getContext(), emailAddress);
        }
    };
    private final View.OnClickListener newCommentListener = v -> {
        final Editable text = binding.commentText.getText();
        final Context context = getContext();
        if (context == null) return;
        if (text == null || TextUtils.isEmpty(text.toString())) {
            Toast.makeText(context, R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
            return;
        }
        final String userId = CookieUtils.getUserIdFromCookie(cookie);
        if (userId == null) return;
        String replyToId = null;
        final CommentModel commentModel = commentsAdapter.getSelected();
        if (commentModel != null) {
            replyToId = commentModel.getId();
        }
        mediaService.comment(postId, text.toString(), userId, replyToId, CookieUtils.getCsrfTokenFromCookie(cookie), new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                commentsAdapter.clearSelection();
                binding.commentText.setText("");
                if (!result) {
                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                onRefresh();
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error during comment", t);
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        mediaService = MediaService.getInstance();
        // setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentCommentsBinding.inflate(getLayoutInflater());
        binding.swipeRefreshLayout.setEnabled(false);
        binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    // @Override
    // public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
    //     inflater.inflate(R.menu.follow, menu);
    //     menu.findItem(R.id.action_compare).setVisible(false);
    //     final MenuItem menuSearch = menu.findItem(R.id.action_search);
    //     final SearchView searchView = (SearchView) menuSearch.getActionView();
    //     searchView.setQueryHint(getResources().getString(R.string.action_search));
    //     searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
    //         @Override
    //         public boolean onQueryTextSubmit(final String query) {
    //             return false;
    //         }
    //
    //         @Override
    //         public boolean onQueryTextChange(final String query) {
    //             // if (commentsAdapter != null) commentsAdapter.getFilter().filter(query);
    //             return true;
    //         }
    //     });
    // }

    @Override
    public void onRefresh() {
        endCursor = null;
        lazyLoader.resetState();
        commentsViewModel.getList().postValue(Collections.emptyList());
        stopCurrentExecutor();
        currentlyRunning = new CommentsFetcher(shortCode, "", fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void init() {
        if (getArguments() == null) return;
        final CommentsViewerFragmentArgs fragmentArgs = CommentsViewerFragmentArgs.fromBundle(getArguments());
        shortCode = fragmentArgs.getShortCode();
        postId = fragmentArgs.getPostId();
        userId = fragmentArgs.getPostUserId();
        // setTitle();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        binding.swipeRefreshLayout.setRefreshing(true);
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        layoutManager = new LinearLayoutManager(getContext());
        binding.rvComments.setLayoutManager(layoutManager);
        commentsAdapter = new CommentsAdapter(commentCallback);
        binding.rvComments.setAdapter(commentsAdapter);
        commentsViewModel.getList().observe(getViewLifecycleOwner(), commentsAdapter::submitList);
        resources = getResources();
        if (!TextUtils.isEmpty(cookie)) {
            binding.commentField.setStartIconVisible(false);
            binding.commentField.setEndIconVisible(false);
            binding.commentField.setVisibility(View.VISIBLE);
            binding.commentText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    binding.commentField.setStartIconVisible(s.length() > 0);
                    binding.commentField.setEndIconVisible(s.length() > 0);
                }

                @Override
                public void afterTextChanged(final Editable s) {}
            });
            binding.commentField.setStartIconOnClickListener(v -> {
                commentsAdapter.clearSelection();
                binding.commentText.setText("");
            });
            binding.commentField.setEndIconOnClickListener(newCommentListener);
        }
        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!TextUtils.isEmpty(endCursor))
                currentlyRunning = new CommentsFetcher(shortCode, endCursor, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            endCursor = null;
        });
        binding.rvComments.addOnScrollListener(lazyLoader);
        stopCurrentExecutor();
        onRefresh();
    }

    // private void setTitle() {
    //     final ActionBar actionBar = fragmentActivity.getSupportActionBar();
    //     if (actionBar == null) return;
    //     actionBar.setTitle(R.string.title_comments);
        // actionBar.setSubtitle(shortCode);
    // }

    private void onCommentClick(final CommentModel commentModel) {
        final String username = commentModel.getProfileModel().getUsername();
        final SpannableString title = new SpannableString(username + ":\n" + commentModel.getText());
        title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        String[] commentDialogList;

        final String userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
        if (!TextUtils.isEmpty(cookie)
                && userIdFromCookie != null
                && (userIdFromCookie.equals(commentModel.getProfileModel().getId()) || userIdFromCookie.equals(userId))) {
            commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_see_likers),
                    resources.getString(R.string.comment_viewer_reply_comment),
                    commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment)
                                            : resources.getString(R.string.comment_viewer_like_comment),
                    resources.getString(R.string.comment_viewer_translate_comment),
                    resources.getString(R.string.comment_viewer_delete_comment)
            };
        } else if (!TextUtils.isEmpty(cookie)) {
            commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_see_likers),
                    resources.getString(R.string.comment_viewer_reply_comment),
                    commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment)
                                            : resources.getString(R.string.comment_viewer_like_comment),
                    resources.getString(R.string.comment_viewer_translate_comment)
            };
        } else {
            commentDialogList = new String[]{
                    resources.getString(R.string.open_profile),
                    resources.getString(R.string.comment_viewer_copy_comment),
                    resources.getString(R.string.comment_viewer_see_likers)
            };
        }
        final Context context = getContext();
        if (context == null) return;
        final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
            final ProfileModel profileModel = commentModel.getProfileModel();
            final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
            switch (which) {
                case 0: // open profile
                    openProfile("@" + profileModel.getUsername());
                    break;
                case 1: // copy comment
                    Utils.copyText(context, "@" + profileModel.getUsername() + ": " + commentModel.getText());
                    break;
                case 2: // see comment likers, this is surprisingly available to anons
                    final NavController navController = getNavController();
                    if (navController != null) {
                        final Bundle bundle = new Bundle();
                        bundle.putString("postId", commentModel.getId());
                        bundle.putBoolean("isComment", true);
                        navController.navigate(R.id.action_global_likesViewerFragment, bundle);
                    }
                    else Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    break;
                case 3: // reply to comment
                    commentsAdapter.setSelected(commentModel);
                    String mention = "@" + profileModel.getUsername() + " ";
                    binding.commentText.setText(mention);
                    binding.commentText.requestFocus();
                    binding.commentText.setSelection(mention.length());
                    binding.commentText.postDelayed(() -> {
                        imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                        if (imm == null) return;
                        imm.showSoftInput(binding.commentText, 0);
                    }, 200);
                    break;
                case 4: // like/unlike comment
                    if (csrfToken == null) {
                        return;
                    }
                    if (!commentModel.getLiked()) {
                        mediaService.commentLike(commentModel.getId(), csrfToken, new ServiceCallback<Boolean>() {
                            @Override
                            public void onSuccess(final Boolean result) {
                                if (!result) {
                                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                commentsAdapter.setLiked(commentModel, true);
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error liking comment", t);
                                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    mediaService.commentUnlike(commentModel.getId(), csrfToken, new ServiceCallback<Boolean>() {
                        @Override
                        public void onSuccess(final Boolean result) {
                            if (!result) {
                                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            commentsAdapter.setLiked(commentModel, false);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error unliking comment", t);
                            Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case 5: // translate comment
                    mediaService.translate(commentModel.getId(), "2", new ServiceCallback<String>() {
                        @Override
                        public void onSuccess(final String result) {
                            if (TextUtils.isEmpty(result)) {
                                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new AlertDialog.Builder(context)
                                    .setTitle(username)
                                    .setMessage(result)
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error translating comment", t);
                            Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case 6: // delete comment
                    final String userId = CookieUtils.getUserIdFromCookie(cookie);
                    if (userId == null) return;
                    mediaService.deleteComment(
                            postId, userId, commentModel.getId(), csrfToken,
                            new ServiceCallback<Boolean>() {
                                @Override
                                public void onSuccess(final Boolean result) {
                                    if (!result) {
                                        Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    onRefresh();
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    Log.e(TAG, "Error deleting comment", t);
                                    Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
            }
        };
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(commentDialogList, profileDialogListener)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openProfile(final String username) {
        final NavDirections action = CommentsViewerFragmentDirections.actionGlobalProfileFragment(username);
        NavHostFragment.findNavController(this).navigate(action);
    }

    private void stopCurrentExecutor() {
        if (currentlyRunning != null) {
            try {
                currentlyRunning.cancel(true);
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e(TAG, "", e);
            }
        }
    }

    @Nullable
    private NavController getNavController() {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            Log.e(TAG, "navigateToProfile", e);
        }
        return navController;
    }
}