package awais.instagrabber.fragments;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import awais.instagrabber.R;
import awais.instagrabber.adapters.NotificationsAdapter;
import awais.instagrabber.adapters.NotificationsAdapter.OnNotificationClickListener;
import awais.instagrabber.asyncs.NotificationsFetcher;
import awais.instagrabber.databinding.FragmentNotificationsViewerBinding;
import awais.instagrabber.fragments.settings.MorePreferencesFragmentDirections;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.services.FriendshipService;
import awais.instagrabber.services.NewsService;
import awais.instagrabber.services.ServiceCallback;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.NotificationViewModel;

import static awais.instagrabber.utils.Utils.notificationManager;

public final class NotificationsViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "NotificationsViewer";

    private FragmentNotificationsViewerBinding binding;
    private SwipeRefreshLayout root;
    private boolean shouldRefresh = true;
    private NotificationViewModel notificationViewModel;
    private FriendshipService friendshipService;
    private String userId;
    private String csrfToken;
    private NewsService newsService;

    private final OnNotificationClickListener clickListener = model -> {
        if (model == null) return;
        final String username = model.getUsername();
        final SpannableString title = new SpannableString(username + (TextUtils.isEmpty(model.getText()) ? "" : (":\n" + model.getText())));
        title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        String[] commentDialogList;
        if (model.getShortCode() != null) {
            commentDialogList = new String[]{
                    getString(R.string.open_profile),
                    getString(R.string.view_post)
            };
        } else if (model.getType() == NotificationType.REQUEST) {
            commentDialogList = new String[]{
                    getString(R.string.open_profile),
                    getString(R.string.request_approve),
                    getString(R.string.request_reject)
            };
        } else {
            commentDialogList = new String[]{getString(R.string.open_profile)};
        }
        if (getContext() == null) return;
        final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
            switch (which) {
                case 0:
                    openProfile(model.getUsername());
                    break;
                case 1:
                    if (model.getType() == NotificationType.REQUEST) {
                        friendshipService.approve(userId, model.getUserId(), csrfToken, new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                            @Override
                            public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                                // Log.d(TAG, "onSuccess: " + result);
                                if (result.getStatus().equals("ok")) {
                                    onRefresh();
                                    return;
                                }
                                Log.e(TAG, "approve: status was not ok!");
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                Log.e(TAG, "approve: onFailure: ", t);
                            }
                        });
                        return;
                    }
                    final NavDirections action = MorePreferencesFragmentDirections
                            .actionGlobalPostViewFragment(0, new String[]{model.getShortCode()}, false);
                    NavHostFragment.findNavController(this).navigate(action);
                    break;
                case 2:
                    friendshipService.ignore(userId, model.getUserId(), csrfToken, new ServiceCallback<FriendshipRepoChangeRootResponse>() {
                        @Override
                        public void onSuccess(final FriendshipRepoChangeRootResponse result) {
                            // Log.d(TAG, "onSuccess: " + result);
                            if (result.getStatus().equals("ok")) {
                                onRefresh();
                                return;
                            }
                            Log.e(TAG, "ignore: status was not ok!");
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Log.e(TAG, "ignore: onFailure: ", t);
                        }
                    });
                    break;
            }
        };
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setItems(commentDialogList, profileDialogListener)
                .setNegativeButton(R.string.cancel, null)
                .show();
    };
    private final MentionClickListener mentionClickListener = (view, text, isHashtag, isLocation) -> {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(text)
                .setMessage(isHashtag ? R.string.comment_view_mention_hash_search
                                      : R.string.comment_view_mention_user_search)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> openProfile(text))
                .show();
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationManager.cancel(Constants.ACTIVITY_NOTIFICATION_ID);
        final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        if (TextUtils.isEmpty(cookie)) {
            Toast.makeText(getContext(), R.string.activity_notloggedin, Toast.LENGTH_SHORT).show();
        }
        friendshipService = FriendshipService.getInstance();
        newsService = NewsService.getInstance();
        userId = CookieUtils.getUserIdFromCookie(cookie);
        csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentNotificationsViewerBinding.inflate(getLayoutInflater());
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    private void init() {
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        final NotificationsAdapter adapter = new NotificationsAdapter(clickListener, mentionClickListener);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(adapter);
        notificationViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        binding.swipeRefreshLayout.setRefreshing(true);
        new NotificationsFetcher(notificationModels -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            notificationViewModel.getList().postValue(notificationModels);
            final String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            newsService.markChecked(timestamp, csrfToken, new ServiceCallback<Boolean>() {
                @Override
                public void onSuccess(@NonNull final Boolean result) {
                    // Log.d(TAG, "onResponse: body: " + result);
                    if (!result) Log.e(TAG, "onSuccess: Error marking activity checked, response is false");
                }

                @Override
                public void onFailure(final Throwable t) {
                    Log.e(TAG, "onFailure: Error marking activity checked", t);
                }
            });
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void openProfile(final String username) {
        final NavDirections action = MorePreferencesFragmentDirections
                .actionGlobalProfileFragment("@" + username);
        NavHostFragment.findNavController(this).navigate(action);
    }
}