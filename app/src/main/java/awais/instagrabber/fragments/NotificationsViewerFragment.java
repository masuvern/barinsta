package awais.instagrabber.fragments;

import android.content.Context;
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
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.NotificationsAdapter;
import awais.instagrabber.adapters.NotificationsAdapter.OnNotificationClickListener;
import awais.instagrabber.asyncs.NotificationsFetcher;
import awais.instagrabber.databinding.FragmentNotificationsViewerBinding;
import awais.instagrabber.fragments.settings.MorePreferencesFragmentDirections;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.NotificationModel;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.FriendshipChangeResponse;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.NotificationViewModel;
import awais.instagrabber.webservices.FriendshipService;
import awais.instagrabber.webservices.MediaService;
import awais.instagrabber.webservices.NewsService;
import awais.instagrabber.webservices.ServiceCallback;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class NotificationsViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "NotificationsViewer";

    private FragmentNotificationsViewerBinding binding;
    private SwipeRefreshLayout root;
    private boolean shouldRefresh = true;
    private NotificationViewModel notificationViewModel;
    private FriendshipService friendshipService;
    private MediaService mediaService;
    private String csrfToken;
    private String type;
    private Context context;

    private final OnNotificationClickListener clickListener = new OnNotificationClickListener() {
        @Override
        public void onProfileClick(final String username) {
            openProfile(username);
        }

        @Override
        public void onPreviewClick(final NotificationModel model) {
            if (model.getType() == NotificationType.RESPONDED_STORY) {
                final NavDirections action = NotificationsViewerFragmentDirections
                        .actionNotificationsViewerFragmentToStoryViewerFragment(StoryViewerOptions.forStory(model.getPostId(),
                                                                                                            model.getUsername()));
                NavHostFragment.findNavController(NotificationsViewerFragment.this).navigate(action);
            } else {
                mediaService.fetch(model.getPostId(), new ServiceCallback<Media>() {
                    @Override
                    public void onSuccess(final Media feedModel) {
                        final PostViewV2Fragment fragment = PostViewV2Fragment
                                .builder(feedModel)
                                .build();
                        fragment.show(getChildFragmentManager(), "post_view");
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onNotificationClick(final NotificationModel model) {
            if (model == null) return;
            final String username = model.getUsername();
            if (model.getType() == NotificationType.FOLLOW || model.getType() == NotificationType.AYML) {
                openProfile(username);
            } else {
                final SpannableString title = new SpannableString(username + (TextUtils.isEmpty(model.getText()) ? "" : (":\n" + model.getText())));
                title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                String[] commentDialogList;
                if (model.getType() == NotificationType.RESPONDED_STORY) {
                    commentDialogList = new String[]{
                            getString(R.string.open_profile),
                            getString(R.string.view_story)
                    };
                } else if (model.getPostId() > 0) {
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
                } else commentDialogList = null; // shouldn't happen
                final Context context = getContext();
                if (context == null) return;
                final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openProfile(username);
                            break;
                        case 1:
                            if (model.getType() == NotificationType.REQUEST) {
                                friendshipService.approve(model.getUserId(), new ServiceCallback<FriendshipChangeResponse>() {
                                    @Override
                                    public void onSuccess(final FriendshipChangeResponse result) {
                                        onRefresh();
                                        Log.e(TAG, "approve: status was not ok!");
                                    }

                                    @Override
                                    public void onFailure(final Throwable t) {
                                        Log.e(TAG, "approve: onFailure: ", t);
                                    }
                                });
                                return;
                            } else if (model.getType() == NotificationType.RESPONDED_STORY) {
                                final NavDirections action = NotificationsViewerFragmentDirections
                                        .actionNotificationsViewerFragmentToStoryViewerFragment(StoryViewerOptions.forStory(model.getPostId(),
                                                                                                                            model.getUsername()));
                                NavHostFragment.findNavController(NotificationsViewerFragment.this).navigate(action);
                                return;
                            }
                            final AlertDialog alertDialog = new AlertDialog.Builder(context)
                                    .setCancelable(false)
                                    .setView(R.layout.dialog_opening_post)
                                    .create();
                            alertDialog.show();
                            mediaService.fetch(model.getPostId(), new ServiceCallback<Media>() {
                                @Override
                                public void onSuccess(final Media feedModel) {
                                    final PostViewV2Fragment fragment = PostViewV2Fragment
                                            .builder(feedModel)
                                            .build();
                                    fragment.setOnShowListener(dialog1 -> alertDialog.dismiss());
                                    fragment.show(getChildFragmentManager(), "post_view");
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 2:
                            friendshipService.ignore(model.getUserId(), new ServiceCallback<FriendshipChangeResponse>() {
                                @Override
                                public void onSuccess(final FriendshipChangeResponse result) {
                                    onRefresh();
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    Log.e(TAG, "ignore: onFailure: ", t);
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
        }
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
        context = getContext();
        if (context == null) return;
        NotificationManagerCompat.from(context.getApplicationContext()).cancel(Constants.ACTIVITY_NOTIFICATION_ID);
        final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        if (TextUtils.isEmpty(cookie)) {
            Toast.makeText(context, R.string.activity_notloggedin, Toast.LENGTH_SHORT).show();
        }
        mediaService = MediaService.getInstance(null, null, 0);
        final long userId = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID);
        csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        friendshipService = FriendshipService.getInstance(deviceUuid, csrfToken, userId);
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
        final NotificationsViewerFragmentArgs fragmentArgs = NotificationsViewerFragmentArgs.fromBundle(getArguments());
        type = fragmentArgs.getType();
        final Context context = getContext();
        CookieUtils.setupCookies(settingsHelper.getString(Constants.COOKIE));
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        final NotificationsAdapter adapter = new NotificationsAdapter(clickListener, mentionClickListener);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(context));
        binding.rvComments.setAdapter(adapter);
        notificationViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        binding.swipeRefreshLayout.setRefreshing(true);
        switch (type) {
            case "notif":
                new NotificationsFetcher(true, new FetchListener<List<NotificationModel>>() {
                    @Override
                    public void onResult(final List<NotificationModel> notificationModels) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        notificationViewModel.getList().postValue(notificationModels);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case "ayml":
                final NewsService newsService = NewsService.getInstance();
                newsService.fetchSuggestions(csrfToken, new ServiceCallback<List<NotificationModel>>() {
                    @Override
                    public void onSuccess(final List<NotificationModel> notificationModels) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        notificationViewModel.getList().postValue(notificationModels);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void openProfile(final String username) {
        final NavDirections action = MorePreferencesFragmentDirections
                .actionGlobalProfileFragment("@" + username);
        NavHostFragment.findNavController(this).navigate(action);
    }
}