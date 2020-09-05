package awais.instagrabber.fragments;

import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import awais.instagrabber.R;
import awais.instagrabber.activities.ProfilePicViewer;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.asyncs.CommentsFetcher;
import awais.instagrabber.databinding.FragmentCommentsBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.services.MediaService;
import awais.instagrabber.services.ServiceCallback;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static android.content.Context.INPUT_METHOD_SERVICE;

public final class CommentsViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "CommentsViewerFragment";

    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);

    private CommentsAdapter commentsAdapter;
    private CommentModel commentModel;
    private FragmentCommentsBinding binding;
    private String shortCode;
    private String userId;
    private Resources resources;
    private InputMethodManager imm;
    private AppCompatActivity fragmentActivity;
    private LinearLayout root;
    private boolean shouldRefresh = true;
    private MediaService mediaService;
    private String postId;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        mediaService = MediaService.getInstance();
        setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentCommentsBinding.inflate(getLayoutInflater());
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
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.follow, menu);
        menu.findItem(R.id.action_compare).setVisible(false);
        final MenuItem menuSearch = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                if (commentsAdapter != null) commentsAdapter.getFilter().filter(query);
                return true;
            }
        });
    }

    @Override
    public void onRefresh() {
        binding.swipeRefreshLayout.setRefreshing(true);
        new CommentsFetcher(shortCode, commentModels -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            commentsAdapter = new CommentsAdapter(commentModels, true, clickListener, mentionClickListener);
            binding.rvComments.setAdapter(commentsAdapter);
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void init() {
        if (getArguments() == null) return;
        final CommentsViewerFragmentArgs fragmentArgs = CommentsViewerFragmentArgs.fromBundle(getArguments());
        shortCode = fragmentArgs.getShortCode();
        postId = fragmentArgs.getPostId();
        userId = fragmentArgs.getPostUserId();
        setTitle();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        binding.swipeRefreshLayout.setRefreshing(true);
        resources = getResources();
        if (!Utils.isEmpty(cookie)) {
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
                commentModel = null;
                binding.commentText.setText("");
            });
            binding.commentField.setEndIconOnClickListener(newCommentListener);
        }
        new CommentsFetcher(this.shortCode, commentModels -> {
            commentsAdapter = new CommentsAdapter(commentModels, true, clickListener, mentionClickListener);
            binding.rvComments.setAdapter(commentsAdapter);
            binding.swipeRefreshLayout.setRefreshing(false);
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setTitle() {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setTitle(R.string.title_comments);
        // actionBar.setSubtitle(shortCode);
    }

    final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
        if (commentModel == null) {
            Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            return;
        }
        final ProfileModel profileModel = commentModel.getProfileModel();
        switch (which) {
            case 0: // open profile
                openProfile(profileModel.getUsername());
                break;
            case 1: // view profile pic
                startActivity(new Intent(requireContext(), ProfilePicViewer.class).putExtra(Constants.EXTRAS_PROFILE, profileModel));
                break;
            case 2: // copy username
                Utils.copyText(requireContext(), profileModel.getUsername());
                break;
            case 3: // copy comment
                Utils.copyText(requireContext(), commentModel.getText().toString());
                break;
            case 4: // reply to comment
                final View focus = binding.rvComments.findViewWithTag(commentModel);
                focus.setBackgroundColor(0x80888888);
                String mention = "@" + profileModel.getUsername() + " ";
                binding.commentText.setText(mention);
                binding.commentText.requestFocus();
                binding.commentText.setSelection(mention.length());
                binding.commentText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        imm = (InputMethodManager) requireContext().getSystemService(INPUT_METHOD_SERVICE);
                        if (imm == null) return;
                        imm.showSoftInput(binding.commentText, 0);
                    }
                }, 200);
                break;
            case 5: // like/unlike comment
                if (!commentModel.getLiked()) {
                    mediaService.commentLike(commentModel.getId(), Utils.getCsrfTokenFromCookie(cookie), new ServiceCallback<Boolean>() {
                        @Override
                        public void onSuccess(final Boolean result) {
                            commentModel = null;
                            if (!result) {
                                Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            onRefresh();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "Error liking comment", t);
                            Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                mediaService.commentUnlike(commentModel.getId(), Utils.getCsrfTokenFromCookie(cookie), new ServiceCallback<Boolean>() {
                    @Override
                    public void onSuccess(final Boolean result) {
                        commentModel = null;
                        if (!result) {
                            Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        onRefresh();
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        Log.e(TAG, "Error unliking comment", t);
                        Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case 6: // delete comment
                final String userId = Utils.getUserIdFromCookie(cookie);
                if (userId == null) return;
                mediaService.deleteComment(
                        postId, userId, commentModel.getId(), Utils.getCsrfTokenFromCookie(cookie),
                        new ServiceCallback<Boolean>() {
                            @Override
                            public void onSuccess(final Boolean result) {
                                commentModel = null;
                                if (!result) {
                                    Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                onRefresh();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "Error deleting comment", t);
                                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
        }
    };

    private final View.OnClickListener clickListener = v -> {
        final Object tag = v.getTag();
        if (tag instanceof CommentModel) {
            commentModel = (CommentModel) tag;

            final String username = commentModel.getProfileModel().getUsername();
            final SpannableString title = new SpannableString(username + ":\n" + commentModel.getText());
            title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            String[] commentDialogList;

            final String userIdFromCookie = Utils.getUserIdFromCookie(cookie);
            if (!Utils.isEmpty(cookie)
                    && userIdFromCookie != null
                    && (userIdFromCookie.equals(commentModel.getProfileModel().getId()) || userIdFromCookie.equals(userId))) {
                commentDialogList = new String[]{
                        resources.getString(R.string.open_profile),
                        resources.getString(R.string.view_pfp),
                        resources.getString(R.string.comment_viewer_copy_user),
                        resources.getString(R.string.comment_viewer_copy_comment),
                        resources.getString(R.string.comment_viewer_reply_comment),
                        commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment)
                                                : resources.getString(R.string.comment_viewer_like_comment),
                        resources.getString(R.string.comment_viewer_delete_comment)
                };
            } else if (!Utils.isEmpty(cookie)) {
                commentDialogList = new String[]{
                        resources.getString(R.string.open_profile),
                        resources.getString(R.string.view_pfp),
                        resources.getString(R.string.comment_viewer_copy_user),
                        resources.getString(R.string.comment_viewer_copy_comment),
                        resources.getString(R.string.comment_viewer_reply_comment),
                        commentModel.getLiked() ? resources.getString(R.string.comment_viewer_unlike_comment)
                                                : resources.getString(R.string.comment_viewer_like_comment),
                };
            } else {
                commentDialogList = new String[]{
                        resources.getString(R.string.open_profile),
                        resources.getString(R.string.view_pfp),
                        resources.getString(R.string.comment_viewer_copy_user),
                        resources.getString(R.string.comment_viewer_copy_comment)
                };
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setItems(commentDialogList, profileDialogListener)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    };

    private final MentionClickListener mentionClickListener = (view, text, isHashtag, isLocation) -> {
        if (isHashtag) {
            final NavDirections action = CommentsViewerFragmentDirections.actionGlobalHashTagFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        openProfile(text);
    };

    private final View.OnClickListener newCommentListener = v -> {
        final Editable text = binding.commentText.getText();
        if (text == null || Utils.isEmpty(text.toString())) {
            Toast.makeText(requireContext(), R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
            return;
        }
        final String userId = Utils.getUserIdFromCookie(cookie);
        if (userId == null) return;
        String replyToId = null;
        if (commentModel != null) {
            replyToId = commentModel.getId();
        }
        mediaService.comment(postId, text.toString(), userId, replyToId, Utils.getCsrfTokenFromCookie(cookie), new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                commentModel = null;
                binding.commentText.setText("");
                if (!result) {
                    Toast.makeText(requireContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                onRefresh();
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error during comment", t);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    };

    private void openProfile(final String username) {
        final NavDirections action = CommentsViewerFragmentDirections.actionGlobalProfileFragment("@" + username);
        NavHostFragment.findNavController(this).navigate(action);
    }
}