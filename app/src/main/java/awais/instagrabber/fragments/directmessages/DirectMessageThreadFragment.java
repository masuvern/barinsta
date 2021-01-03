package awais.instagrabber.fragments.directmessages;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;
import java.util.Optional;

import awais.instagrabber.R;
import awais.instagrabber.activities.CameraActivity;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.adapters.DirectItemsAdapter.DirectItemOrHeader;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemViewHolder;
import awais.instagrabber.animations.CubicBezierInterpolator;
import awais.instagrabber.customviews.RecordView;
import awais.instagrabber.customviews.Tooltip;
import awais.instagrabber.customviews.helpers.HeaderItemDecoration;
import awais.instagrabber.customviews.helpers.HeightProvider;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtEdge;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.databinding.FragmentDirectMessagesThreadBinding;
import awais.instagrabber.dialogs.MediaPickerBottomDialogFragment;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.PermissionUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.AppStateViewModel;
import awais.instagrabber.viewmodels.DirectInboxViewModel;
import awais.instagrabber.viewmodels.DirectThreadViewModel;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;

public class DirectMessageThreadFragment extends Fragment {
    private static final String TAG = DirectMessageThreadFragment.class.getSimpleName();
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private static final int AUDIO_RECORD_PERM_REQUEST_CODE = 1000;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final String UPDATING_TITLE = "Updating...";
    private static final String MESSAGE_LABEL = "Message";
    private static final String HOLD_TO_RECORD_AUDIO_LABEL = "Press and hold to record audio";
    private static final String TRANSLATION_Y = "translationY";

    private DirectItemsAdapter itemsAdapter;
    private MainActivity fragmentActivity;
    private DirectThreadViewModel viewModel;
    public static boolean hasSentSomething;
    private ConstraintLayout root;
    private boolean shouldRefresh = true;
    private List<DirectItemOrHeader> itemOrHeaders;
    private MenuItem markAsSeenMenuItem;
    private FragmentDirectMessagesThreadBinding binding;
    private Tooltip tooltip;
    private float initialSendX;
    private ActionBar actionBar;
    private AppStateViewModel appStateViewModel;
    private Runnable prevTitleRunnable;
    private int originalSoftInputMode;
    private AnimatorSet animatorSet;
    private boolean isEmojiPickerShown;
    private boolean isKbShown;
    private HeightProvider heightProvider;
    private boolean isRecording;
    private boolean wasKbShowing;
    private int keyboardHeight = Utils.convertDpToPx(250);

    // private final View.OnClickListener clickListener = v -> {
    //     if (v == binding.commentSend) {
    //         final String text = binding.commentText.getText().toString();
    //         if (TextUtils.isEmpty(text)) {
    //             final Context context = getContext();
    //             if (context == null) return;
    //             Toast.makeText(context, R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
    //             return;
    //         }
    //         sendText(text, null, false);
    //         return;
    //     }
    //     if (v == binding.image) {
    //         final Intent intent = new Intent();
    //         intent.setType("image/*");
    //         intent.setAction(Intent.ACTION_GET_CONTENT);
    //         startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE);
    //     }
    // };
    // private final FetchListener<InboxThreadModel> fetchListener = new FetchListener<InboxThreadModel>() {
    //     @Override
    //     public void doBefore() {
    //         setTitle(true);g
    //     }
    //
    //     @Override
    //     public void onResult(final InboxThreadModel result) {
    //         if (result == null && ("MINCURSOR".equals(cursor) || "MAXCURSOR".equals(cursor) || TextUtils.isEmpty(cursor))) {
    //             final Context context = getContext();
    //             if (context == null) return;
    //             Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    //         }
    //
    //         if (result != null) {
    //             cursor = result.getOldestCursor();
    //             hasOlder = result.hasOlder();
    //             if ("MINCURSOR".equals(cursor) || "MAXCURSOR".equals(cursor)) {
    //                 cursor = null;
    //             }
    //             users.clear();
    //             users.addAll(result.getUsers());
    //             leftUsers.clear();
    //             leftUsers.addAll(result.getLeftUsers());
    //
    //             List<DirectItemModel> list = listViewModel.getList().getValue();
    //             final List<DirectItemModel> newList = result.getItems();
    //             list = list != null ? new LinkedList<>(list) : new LinkedList<>();
    //             if (hasSentSomething || hasDeletedSomething) {
    //                 list = newList;
    //                 final Handler handler = new Handler();
    //                 if (hasSentSomething) handler.postDelayed(() -> {
    //                     binding.messageList.smoothScrollToPosition(0);
    //                 }, 200);
    //                 hasSentSomething = false;
    //                 hasDeletedSomething = false;
    //             } else {
    //                 list.addAll(newList);
    //             }
    //             listViewModel.getList().postValue(list);
    //             lastMessage = result.getNewestCursor();
    //
    //             // if (Utils.settingsHelper.getBoolean(Constants.DM_MARK_AS_SEEN)) new ThreadAction().execute("seen", lastMessage);
    //         }
    //         setTitle(false);
    //     }
    // };
    // private DirectItemMediaModel downloadMediaItem;
    // private int prevSizeChangeHeight;
    // private ArrayAdapter<String> dialogAdapter;
    private final AppExecutors appExecutors = AppExecutors.getInstance();
    private final Animatable2Compat.AnimationCallback micToSendAnimationCallback = new Animatable2Compat.AnimationCallback() {
        @Override
        public void onAnimationEnd(final Drawable drawable) {
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable, this);
            setSendToMicIcon();
        }
    };
    private final Animatable2Compat.AnimationCallback sendToMicAnimationCallback = new Animatable2Compat.AnimationCallback() {
        @Override
        public void onAnimationEnd(final Drawable drawable) {
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable, this);
            setMicToSendIcon();
        }
    };


    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        appStateViewModel = new ViewModelProvider(fragmentActivity).get(AppStateViewModel.class);
        viewModel = new ViewModelProvider(this).get(DirectThreadViewModel.class);
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
        binding = FragmentDirectMessagesThreadBinding.inflate(inflater, container, false);
        binding.send.setRecordView(binding.recordView);
        root = binding.getRoot();
        final Context context = getContext();
        if (context == null) {
            return root;
        }
        tooltip = new Tooltip(context, root, getResources().getColor(R.color.grey_400), getResources().getColor(R.color.black));
        originalSoftInputMode = fragmentActivity.getWindow().getAttributes().softInputMode;
        // todo check has camera and remove view
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        binding.send.post(() -> initialSendX = binding.send.getX());
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.dm_thread_menu, menu);
        markAsSeenMenuItem = menu.findItem(R.id.mark_as_seen);
        if (markAsSeenMenuItem != null) {
            markAsSeenMenuItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.info) {
            final NavDirections action = DirectMessageThreadFragmentDirections.actionDMThreadFragmentToDMSettingsFragment(viewModel.getThreadId());
            NavHostFragment.findNavController(this).navigate(action);
            return true;
        }
        if (itemId == R.id.mark_as_seen) {
            // new ThreadAction().execute("seen", lastMessage);
            item.setVisible(false);
            return true;
        }
        if (itemId == R.id.refresh && viewModel != null) {
            viewModel.refreshChats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Log.w(TAG, "data is null!");
                return;
            }
            final Uri uri = data.getData();
            navigateToImageEditFragment(uri);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final Context context = getContext();
        if (context == null) return;
        if (requestCode == STORAGE_PERM_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // downloadItem(context);
        }
        if (requestCode == AUDIO_RECORD_PERM_REQUEST_CODE) {
            if (PermissionUtils.hasAudioRecordPerms(context)) {
                Toast.makeText(context, "You can send voice messages now!", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(context, "Require RECORD_AUDIO and WRITE_EXTERNAL_STORAGE permissions", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        if (isRecording) {
            binding.recordView.cancelRecording(binding.send);
        }
        if (isKbShown) {
            wasKbShowing = true;
            binding.emojiPicker.setAlpha(0);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentActivity.getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_NOTHING | SOFT_INPUT_STATE_HIDDEN);
        if (wasKbShowing) {
            appExecutors.mainThread().execute(() -> {
                binding.input.requestFocus();
                binding.input.post(this::showKeyboard);
            });
            wasKbShowing = false;
        }
        setObservers();
        if (initialSendX != 0) {
            binding.send.setX(initialSendX);
        }
        binding.send.stopScale();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private void cleanup() {
        if (prevTitleRunnable != null) {
            appExecutors.mainThread().cancel(prevTitleRunnable);
        }
        if (heightProvider != null) {
            // need to close the height provider popup before navigating back to prevent leak
            heightProvider.dismiss();
        }
        if (originalSoftInputMode != 0) {
            fragmentActivity.getWindow().setSoftInputMode(originalSoftInputMode);
        }
        for (int childCount = binding.chats.getChildCount(), i = 0; i < childCount; ++i) {
            final RecyclerView.ViewHolder holder = binding.chats.getChildViewHolder(binding.chats.getChildAt(i));
            if (holder == null) continue;
            if (holder instanceof DirectItemViewHolder) {
                ((DirectItemViewHolder) holder).cleanup();
            }
        }
    }

    private void init() {
        final Context context = getContext();
        if (context == null) return;
        if (getArguments() == null) return;
        actionBar = fragmentActivity.getSupportActionBar();
        setObservers();
        final DirectMessageThreadFragmentArgs fragmentArgs = DirectMessageThreadFragmentArgs.fromBundle(getArguments());
        viewModel.getThreadTitle().postValue(fragmentArgs.getTitle());
        final String threadId = fragmentArgs.getThreadId();
        viewModel.setThreadId(threadId);
        setupList();
        root.post(this::setupInput);
    }

    private void getInitialData() {
        final NavController navController = NavHostFragment.findNavController(this);
        final ViewModelStoreOwner viewModelStoreOwner = navController.getViewModelStoreOwner(R.id.direct_messages_nav_graph);
        final DirectInboxViewModel threadListViewModel = new ViewModelProvider(viewModelStoreOwner).get(DirectInboxViewModel.class);
        final List<DirectThread> threads = threadListViewModel.getThreads().getValue();
        final Optional<DirectThread> first = threads != null
                                             ? threads.stream()
                                                      .filter(thread -> thread.getThreadId().equals(viewModel.getThreadId()))
                                                      .findFirst()
                                             : Optional.empty();
        if (first.isPresent()) {
            // setTitle(UPDATING_TITLE);
            final DirectThread thread = first.get();
            viewModel.setThread(thread);
            if (itemsAdapter != null) {
                itemsAdapter.setThread(thread);
            }
            return;
        }
        viewModel.fetchChats();
    }

    private void setupList() {
        final Context context = getContext();
        if (context == null) return;
        binding.chats.setItemViewCacheSize(20);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setReverseLayout(true);
        // layoutManager.setStackFromEnd(false);
        // binding.messageList.addItemDecoration(new VerticalSpaceItemDecoration(3));
        final RecyclerView.ItemAnimator animator = binding.chats.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            final SimpleItemAnimator itemAnimator = (SimpleItemAnimator) animator;
            itemAnimator.setSupportsChangeAnimations(false);
        }
        binding.chats.setLayoutManager(layoutManager);
        binding.chats.addOnScrollListener(new RecyclerLazyLoaderAtEdge(layoutManager, true, page -> viewModel.fetchChats()));
        final HeaderItemDecoration headerItemDecoration = new HeaderItemDecoration(binding.chats, itemPosition -> {
            if (itemOrHeaders == null || itemOrHeaders.isEmpty()) return false;
            try {
                final DirectItemOrHeader itemOrHeader = itemOrHeaders.get(itemPosition);
                return itemOrHeader.isHeader();
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        });
        binding.chats.addItemDecoration(headerItemDecoration);
        // final MentionClickListener mentionClickListener = (view, text, isHashtag, isLocation) -> searchUsername(text);
        // final DialogInterface.OnClickListener onDialogListener = (dialogInterface, which) -> {
        //     if (which == 0) {
        //         final DirectItemType itemType = directItemModel.getItemType();
        //         switch (itemType) {
        //             case MEDIA_SHARE:
        //             case CLIP:
        //             case FELIX_SHARE:
        //                 final String shortCode = ((DirectItemMediaModel) directItemModel.getMediaModel()).getCode();
        //                 new PostFetcher(shortCode, feedModel -> {
        //                     final PostViewV2Fragment fragment = PostViewV2Fragment
        //                             .builder(feedModel)
        //                             .build();
        //                     fragment.show(getChildFragmentManager(), "post_view");
        //                 }).execute();
        //                 break;
        //             case LINK:
        //                 Intent linkIntent = new Intent(Intent.ACTION_VIEW);
        //                 linkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //                 linkIntent.setData(Uri.parse(((DirectItemLinkModel) directItemModel.getMediaModel()).getLinkContext().getLinkUrl()));
        //                 startActivity(linkIntent);
        //                 break;
        //             case TEXT:
        //             case REEL_SHARE:
        //                 Utils.copyText(context, directItemModel.getText());
        //                 Toast.makeText(context, R.string.clipboard_copied, Toast.LENGTH_SHORT).show();
        //                 break;
        //             case RAVEN_MEDIA:
        //             case MEDIA:
        //                 downloadItem(context);
        //                 break;
        //             case STORY_SHARE:
        //                 if (directItemModel.getMediaModel() != null) {
        //                     // StoryModel sm = new StoryModel(
        //                     //         directItemModel.getReelShare().getReelId(),
        //                     //         directItemModel.getReelShare().getMedia().getVideoUrl(),
        //                     //         directItemModel.getReelShare().getMedia().getMediaType(),
        //                     //         directItemModel.getTimestamp(),
        //                     //         directItemModel.getReelShare().getReelOwnerName(),
        //                     //         String.valueOf(directItemModel.getReelShare().getReelOwnerId()),
        //                     //         false
        //                     // );
        //                     // sm.setVideoUrl(directItemModel.getReelShare().getMedia().getVideoUrl());
        //                     // StoryModel[] sms = {sm};
        //                     // startActivity(new Intent(getContext(), StoryViewer.class)
        //                     //         .putExtra(Constants.EXTRAS_USERNAME, directItemModel.getReelShare().getReelOwnerName())
        //                     //         .putExtra(Constants.EXTRAS_STORIES, sms)
        //                     // );
        //                 } else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
        //                     searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
        //                 }
        //                 break;
        //             case PLACEHOLDER:
        //                 if (directItemModel.getText().toString().contains("@"))
        //                     searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
        //                 break;
        //             default:
        //                 Log.d(TAG, "unsupported type " + itemType);
        //         }
        //     } else if (which == 1) {
        //         sendText(null, directItemModel.getItemId(), directItemModel.isLiked());
        //     } else if (which == 2) {
        //         if (directItemModel == null) {
        //             Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        //         } else if (String.valueOf(directItemModel.getUserId()).equals(myId)) {
        //             new ThreadAction().execute("delete", directItemModel.getItemId());
        //         } else {
        //             searchUsername(getUser(directItemModel.getUserId()).getUsername());
        //         }
        //     }
        // };
        // final View.OnClickListener onClickListener = v -> {
        //     Object tag = v.getTag();
        //     if (tag instanceof ProfileModel) {
        //         searchUsername(((ProfileModel) tag).getUsername());
        //     } else if (tag instanceof DirectItemModel) {
        //         directItemModel = (DirectItemModel) tag;
        //         final DirectItemType itemType = directItemModel.getItemType();
        //         int firstOption = R.string.dms_inbox_raven_message_unknown;
        //         String[] dialogList;
        //
        //         switch (itemType) {
        //             case MEDIA_SHARE:
        //             case CLIP:
        //             case FELIX_SHARE:
        //                 firstOption = R.string.view_post;
        //                 break;
        //             case LINK:
        //                 firstOption = R.string.dms_inbox_open_link;
        //                 break;
        //             case TEXT:
        //             case REEL_SHARE:
        //                 firstOption = R.string.dms_inbox_copy_text;
        //                 break;
        //             case RAVEN_MEDIA:
        //             case MEDIA:
        //                 firstOption = R.string.dms_inbox_download;
        //                 break;
        //             case STORY_SHARE:
        //                 if (directItemModel.getMediaModel() != null) {
        //                     firstOption = R.string.show_stories;
        //                 } else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
        //                     firstOption = R.string.open_profile;
        //                 }
        //                 break;
        //             case PLACEHOLDER:
        //                 if (directItemModel.getText().toString().contains("@"))
        //                     firstOption = R.string.open_profile;
        //                 break;
        //         }
        //
        //         dialogList = new String[]{
        //                 getString(firstOption),
        //                 getString(directItemModel.isLiked() ? R.string.dms_inbox_unlike : R.string.dms_inbox_like),
        //                 getString(String.valueOf(directItemModel.getUserId()).equals(myId) ? R.string.dms_inbox_unsend : R.string.dms_inbox_author)
        //         };
        //
        //         dialogAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, dialogList);
        //
        //         new AlertDialog.Builder(context)
        //                 .setAdapter(dialogAdapter, onDialogListener)
        //                 .show();
        //     }
        // };
    }

    private void setObservers() {
        viewModel.getThreadTitle().observe(getViewLifecycleOwner(), this::setTitle);
        viewModel.getFetching().observe(getViewLifecycleOwner(), fetching -> {
            if (fetching) {
                setTitle(UPDATING_TITLE);
                return;
            }
            setTitle(viewModel.getThreadTitle().getValue());
        });
        viewModel.getThread().observe(getViewLifecycleOwner(), thread -> {
            if (thread != null && itemsAdapter != null) itemsAdapter.setThread(thread);
        });
        appStateViewModel.getCurrentUser().observe(getViewLifecycleOwner(), currentUser -> {
            viewModel.setCurrentUser(currentUser);
            setupItemsAdapter(currentUser);
            viewModel.getItems().observe(
                    getViewLifecycleOwner(),
                    list -> itemsAdapter.submitList(list, () -> {
                        itemOrHeaders = itemsAdapter.getList();
                        binding.chats.post(() -> {
                            final RecyclerView.LayoutManager layoutManager = binding.chats.getLayoutManager();
                            if (layoutManager instanceof LinearLayoutManager) {
                                final int position = ((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition();
                                if (position < 0) return;
                                if (position == itemsAdapter.getItemCount() - 1) {
                                    viewModel.fetchChats();
                                }
                            }
                        });
                    })
            );
        });
        final NavController navController = NavHostFragment.findNavController(this);
        final NavBackStackEntry backStackEntry = navController.getCurrentBackStackEntry();
        if (backStackEntry != null) {
            final MutableLiveData<Object> resultLiveData = backStackEntry.getSavedStateHandle().getLiveData("result");
            resultLiveData.observe(getViewLifecycleOwner(), result -> {
                if (!(result instanceof Uri)) return;
                final Uri uri = (Uri) result;
                viewModel.sendUri(uri);
            });
        }
    }

    private void setupItemsAdapter(final ProfileModel currentUser) {
        if (itemsAdapter != null) return;
        itemsAdapter = new DirectItemsAdapter(currentUser);
        itemsAdapter.setHasStableIds(true);
        binding.chats.setAdapter(itemsAdapter);
        itemsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(final int positionStart, final int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                final LinearLayoutManager layoutManager = (LinearLayoutManager) binding.chats.getLayoutManager();
                if (layoutManager == null) return;
                int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                if ((firstVisiblePosition == -1 || firstVisiblePosition == 0) && (positionStart == 0)) {
                    binding.chats.scrollToPosition(0);
                }
            }
        });
        root.post(this::getInitialData);
    }

    private void setupInput() {
        final Context context = getContext();
        if (context == null) return;
        tooltip.setText(HOLD_TO_RECORD_AUDIO_LABEL);
        setMicToSendIcon();
        binding.recordView.setMinMillis(1000);
        binding.recordView.setOnRecordListener(new RecordView.OnRecordListener() {
            @Override
            public void onStart() {
                isRecording = true;
                binding.input.setHint(null);
                binding.gallery.setVisibility(View.GONE);
                binding.camera.setVisibility(View.GONE);
                if (PermissionUtils.hasAudioRecordPerms(context)) {
                    viewModel.startRecording();
                    return;
                }
                PermissionUtils.requestAudioRecordPerms(DirectMessageThreadFragment.this, AUDIO_RECORD_PERM_REQUEST_CODE);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
                // binding.input.setHint("Message");
                viewModel.stopRecording(true);
                isRecording = false;
            }

            @Override
            public void onFinish(final long recordTime) {
                Log.d(TAG, "onFinish");
                binding.input.setHint("Message");
                binding.gallery.setVisibility(View.VISIBLE);
                binding.camera.setVisibility(View.VISIBLE);
                viewModel.stopRecording(false);
                isRecording = false;
            }

            @Override
            public void onLessThanMin() {
                Log.d(TAG, "onLessThanMin");
                binding.input.setHint("Message");
                if (PermissionUtils.hasAudioRecordPerms(context)) {
                    tooltip.show(binding.send);
                }
                binding.gallery.setVisibility(View.VISIBLE);
                binding.camera.setVisibility(View.VISIBLE);
                viewModel.stopRecording(true);
                isRecording = false;
            }
        });
        binding.recordView.setOnBasketAnimationEndListener(() -> {
            binding.input.setHint(MESSAGE_LABEL);
            binding.gallery.setVisibility(View.VISIBLE);
            binding.camera.setVisibility(View.VISIBLE);
        });
        binding.input.addTextChangedListener(new TextWatcherAdapter() {
            int prevLength = 0;

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                final int length = s.length();
                if (prevLength != 0 && length == 0) {
                    binding.send.setListenForRecord(true);
                    startIconAnimation();
                }
                if (prevLength == 0 && length != 0) {
                    binding.send.setListenForRecord(false);
                    startIconAnimation();
                }
                binding.gallery.setVisibility(length == 0 ? View.VISIBLE : View.GONE);
                binding.camera.setVisibility(length == 0 ? View.VISIBLE : View.GONE);
                prevLength = length;
            }

            private void startIconAnimation() {
                final Drawable icon = binding.send.getIcon();
                if (icon instanceof Animatable) {
                    final Animatable animatable = (Animatable) icon;
                    if (animatable.isRunning()) {
                        animatable.stop();
                    }
                    animatable.start();
                }
            }
        });
        binding.send.setOnRecordClickListener(v -> {
            final Editable text = binding.input.getText();
            if (TextUtils.isEmpty(text)) return;
            viewModel.sendText(text.toString()).observe(getViewLifecycleOwner(), this::handleSentMessage);
            binding.input.setText("");
        });
        binding.send.setOnRecordLongClickListener(v -> {
            Log.d(TAG, "setOnRecordLongClickListener");
            return true;
        });
        binding.input.setShowSoftInputOnFocus(false);
        binding.input.requestFocus();
        binding.input.setOnKeyEventListener((keyCode, keyEvent) -> {
            if (keyCode != KeyEvent.KEYCODE_BACK) return false;
            // We'll close the keyboard/emoji picker only when user releases the back button
            // return true so that system doesn't handle the event
            if (keyEvent.getAction() != KeyEvent.ACTION_UP) return true;
            if (!isKbShown && !isEmojiPickerShown) {
                // if both keyboard and emoji picker are hidden, navigate back
                if (heightProvider != null) {
                    // need to close the height provider popup before navigating back to prevent leak
                    heightProvider.dismiss();
                }
                NavHostFragment.findNavController(this).navigateUp();
                return true;
            }
            binding.emojiToggle.setIconResource(R.drawable.ic_face_24);
            hideKeyboard(true);
            return true;
        });
        binding.input.setOnClickListener(v -> {
            if (isKbShown) return;
            showKeyboard();
        });
        setupEmojiPicker();
        binding.gallery.setOnClickListener(v -> {
            final MediaPickerBottomDialogFragment mediaPicker = MediaPickerBottomDialogFragment.newInstance();
            mediaPicker.setOnSelectListener(entry -> {
                mediaPicker.dismiss();
                if (!isAdded()) return;
                if (!entry.isVideo) {
                    navigateToImageEditFragment(entry.path);
                }
            });
            mediaPicker.show(getChildFragmentManager(), "MediaPicker");
            hideKeyboard(true);
        });
        binding.camera.setOnClickListener(v -> {
            final Intent intent = new Intent(context, CameraActivity.class);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        });
    }

    private void navigateToImageEditFragment(final String path) {
        navigateToImageEditFragment(Uri.fromFile(new File(path)));
    }

    private void navigateToImageEditFragment(final Uri uri) {
        final NavDirections navDirections = DirectMessageThreadFragmentDirections.actionThreadToImageEdit(uri);
        final NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(navDirections);
    }

    private void handleSentMessage(@NonNull final Resource<DirectItem> resource) {
        final Resource.Status status = resource.status;
        switch (status) {
            case SUCCESS:
            case LOADING:
                break;
            case ERROR:
                if (resource.message != null) {
                    Snackbar.make(binding.getRoot(), resource.message, Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void setupEmojiPicker() {
        root.post(() -> binding.emojiPicker.init(
                root,
                (view, emoji) -> binding.input.append(emoji.getUnicode()),
                () -> binding.input.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ));
        setupKbHeightProvider();
        if (keyboardHeight == 0) {
            keyboardHeight = Utils.convertDpToPx(250);
        }
        setEmojiPickerBounds();
        binding.emojiToggle.setOnClickListener(v -> toggleEmojiPicker());
    }

    public void showKeyboard() {
        final Context context = getContext();
        if (context == null) return;
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (!isEmojiPickerShown) {
            binding.emojiPicker.setAlpha(0);
        }
        imm.showSoftInput(binding.input, InputMethodManager.SHOW_IMPLICIT);
        if (!isEmojiPickerShown) {
            animatePan(keyboardHeight);
        }
        isKbShown = true;
    }

    public void hideKeyboard(final boolean shouldPan) {
        final Context context = getContext();
        if (context == null) return;
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (shouldPan) {
            binding.emojiPicker.setAlpha(0);
        }
        imm.hideSoftInputFromWindow(binding.input.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        if (shouldPan) {
            animatePan(0);
            isEmojiPickerShown = false;
            binding.emojiToggle.setIconResource(R.drawable.ic_face_24);
        }
        isKbShown = false;
    }

    /**
     * Toggle between emoji picker and keyboard
     * If both are hidden, the emoji picker is shown first
     */
    private void toggleEmojiPicker() {
        if (isKbShown) {
            binding.emojiToggle.setIconResource(R.drawable.ic_keyboard_24);
            hideKeyboard(false);
            return;
        }
        if (isEmojiPickerShown) {
            binding.emojiToggle.setIconResource(R.drawable.ic_face_24);
            showKeyboard();
            return;
        }
        binding.emojiToggle.setIconResource(R.drawable.ic_keyboard_24);
        animatePan(keyboardHeight);
        isEmojiPickerShown = true;
    }

    /**
     * Set height of the emoji picker
     */
    private void setEmojiPickerBounds() {
        final ViewGroup.LayoutParams layoutParams = binding.emojiPicker.getLayoutParams();
        layoutParams.height = keyboardHeight;
        if (!isEmojiPickerShown) {
            // If emoji picker is hidden reset the translationY so that it doesn't peek from bottom
            binding.emojiPicker.setTranslationY(keyboardHeight);
        }
        binding.emojiPicker.requestLayout();
    }

    private void setSendToMicIcon() {
        final Context context = getContext();
        if (context == null) return;
        final Drawable sendToMicDrawable = ContextCompat.getDrawable(context, R.drawable.avd_send_to_mic_anim);
        if (sendToMicDrawable instanceof Animatable) {
            AnimatedVectorDrawableCompat.registerAnimationCallback(sendToMicDrawable, sendToMicAnimationCallback);
        }
        binding.send.setIcon(sendToMicDrawable);
    }

    private void setMicToSendIcon() {
        final Context context = getContext();
        if (context == null) return;
        final Drawable micToSendDrawable = ContextCompat.getDrawable(context, R.drawable.avd_mic_to_send_anim);
        if (micToSendDrawable instanceof Animatable) {
            AnimatedVectorDrawableCompat.registerAnimationCallback(micToSendDrawable, micToSendAnimationCallback);
        }
        binding.send.setIcon(micToSendDrawable);
    }

    private void setTitle(final String title) {
        if (actionBar == null) return;
        if (prevTitleRunnable != null) {
            appExecutors.mainThread().cancel(prevTitleRunnable);
        }
        prevTitleRunnable = () -> actionBar.setTitle(title);
        // set title delayed to avoid title blink if fetch is fast
        appExecutors.mainThread().execute(prevTitleRunnable, 1000);
    }

    // private void downloadItem(final Context context) {
    //     final DirectUser user = getUser(directItemModel.getUserId());
    //     final DirectItemMediaModel selectedItem = directItemModel.getItemType() == DirectItemType.MEDIA
    //                                               ? (DirectItemMediaModel) directItemModel.getMediaModel()
    //                                               : ((DirectItemRavenMediaModel) directItemModel.getMediaModel()).getMedia();
    //     final String url = selectedItem.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO
    //                        ? selectedItem.getVideoUrl()
    //                        : selectedItem.getThumbUrl();
    //     if (url == null) {
    //         Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    //     } else {
    //         if (ContextCompat.checkSelfPermission(context, DownloadUtils.PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
    //             DownloadUtils.dmDownload(context, user != null ? user.getUsername() : "", selectedItem.getId(), url);
    //         } else {
    //             requestPermissions(DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE);
    //         }
    //         Toast.makeText(context, R.string.downloader_downloading_media, Toast.LENGTH_SHORT).show();
    //     }
    // }

    // private void sendText(final String text, final String itemId, final boolean delete) {
    //     DirectThreadBroadcaster.TextBroadcastOptions textOptions = null;
    //     DirectThreadBroadcaster.ReactionBroadcastOptions reactionOptions = null;
    //     if (text != null) {
    //         try {
    //             textOptions = new DirectThreadBroadcaster.TextBroadcastOptions(text);
    //         } catch (UnsupportedEncodingException e) {
    //             Log.e(TAG, "Error", e);
    //             return;
    //         }
    //     } else {
    //         reactionOptions = new DirectThreadBroadcaster.ReactionBroadcastOptions(itemId, delete);
    //     }
    //     broadcast(text != null ? textOptions : reactionOptions, result -> {
    //         final Context context = getContext();
    //         if (context == null) return;
    //         if (result == null || result.getResponseCode() != HttpURLConnection.HTTP_OK) {
    //             Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    //             return;
    //         }
    //         if (text != null) {
    //             // binding.commentText.setText("");
    //         } else {
    //             // final View viewWithTag = binding.messageList.findViewWithTag(directItemModel);
    //             // if (viewWithTag != null) {
    //             //     final ViewParent dim = viewWithTag.getParent();
    //             //     if (dim instanceof View) {
    //             //         final View dimView = (View) dim;
    //             // final View likedContainer = dimView.findViewById(R.id.liked_container);
    //             // if (likedContainer != null) {
    //             //     likedContainer.setVisibility(delete ? View.GONE : View.VISIBLE);
    //             // }
    //             // }
    //             // }
    //             // directItemModel.setLiked();
    //         }
    //         context.sendBroadcast(new Intent(DMRefreshBroadcastReceiver.ACTION_REFRESH_DM));
    //         hasSentSomething = true;
    //         // new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
    //         //         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //     });
    // }

    // private void sendImage(final Uri imageUri) {
    //     final Context context = getContext();
    //     if (context == null) return;
    //     try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
    //         final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
    //         Toast.makeText(context, R.string.uploading, Toast.LENGTH_SHORT).show();
    //         // Upload Image
    //         final ImageUploader imageUploader = new ImageUploader();
    //         imageUploader.setOnTaskCompleteListener(response -> {
    //             if (response == null || response.getResponseCode() != HttpURLConnection.HTTP_OK) {
    //                 Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    //                 if (response != null && response.getResponse() != null) {
    //                     Log.e(TAG, response.getResponse().toString());
    //                 }
    //                 return;
    //             }
    //             final JSONObject responseJson = response.getResponse();
    //             try {
    //                 final String uploadId = responseJson.getString("upload_id");
    //                 // Broadcast
    //                 final DirectThreadBroadcaster.ImageBroadcastOptions options = new DirectThreadBroadcaster.ImageBroadcastOptions(true, uploadId);
    //                 hasSentSomething = true;
    //                 broadcast(options,
    //                           broadcastResponse -> {
    //                               // new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
    //                               //         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //                           });
    //             } catch (JSONException e) {
    //                 Log.e(TAG, "Error parsing json response", e);
    //             }
    //         });
    //         final ImageUploadOptions options = ImageUploadOptions.builder(bitmap).build();
    //         imageUploader.execute(options);
    //     } catch (IOException e) {
    //         Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    //         Log.e(TAG, "Error opening file", e);
    //     }
    // }

    // private void broadcast(final DirectThreadBroadcaster.BroadcastOptions broadcastOptions,
    //                        final DirectThreadBroadcaster.OnBroadcastCompleteListener listener) {
    //     final DirectThreadBroadcaster broadcaster = new DirectThreadBroadcaster(threadId);
    //     broadcaster.setOnTaskCompleteListener(listener);
    //     broadcaster.execute(broadcastOptions);
    // }

    // @NonNull
    // private DirectUser getUser(final long userId) {
    //     for (final DirectUser user : users) {
    //         if (userId != user.getPk()) continue;
    //         return user;
    //     }
    //     return null;
    // }

    // private void searchUsername(final String text) {
    //     final Bundle bundle = new Bundle();
    //     bundle.putString("username", "@" + text);
    //     NavHostFragment.findNavController(this).navigate(R.id.action_global_profileFragment, bundle);
    // }

    // class ThreadAction extends AsyncTask<String, Void, Void> {
    //     String action, argument;
    //
    //     protected Void doInBackground(String... rawAction) {
    //         action = rawAction[0];
    //         argument = rawAction[1];
    //         final String url = "https://i.instagram.com/api/v1/direct_v2/threads/" + threadId + "/items/" + argument + "/" + action + "/";
    //         try {
    //             String urlParameters = "_csrftoken=" + COOKIE.split("csrftoken=")[1].split(";")[0]
    //                     + "&_uuid=" + Utils.settingsHelper.getString(Constants.DEVICE_UUID);
    //             final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
    //             urlConnection.setRequestMethod("POST");
    //             urlConnection.setUseCaches(false);
    //             urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
    //             urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    //             urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
    //             urlConnection.setDoOutput(true);
    //             DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
    //             wr.writeBytes(urlParameters);
    //             wr.flush();
    //             wr.close();
    //             urlConnection.connect();
    //             if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
    //                 if (action.equals("delete")) {
    //                     hasDeletedSomething = true;
    //                 } else if (action.equals("seen")) {
    //                     // context.sendBroadcast(new Intent(DMRefreshBroadcastReceiver.ACTION_REFRESH_DM));
    //                 }
    //             }
    //             urlConnection.disconnect();
    //         } catch (Throwable ex) {
    //             Log.e("austin_debug", action + ": " + ex);
    //         }
    //         return null;
    //     }
    //
    //     @Override
    //     protected void onPostExecute(Void result) {
    //         if (hasDeletedSomething) {
    //             // directItemModel = null;
    //             // new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
    //             //         .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    //         }
    //     }
    // }

    private void setupKbHeightProvider() {
        if (heightProvider != null) return;
        heightProvider = new HeightProvider(fragmentActivity).init().setHeightListener(height -> {
            if (height > 100 && keyboardHeight != height) {
                // save the current keyboard height to settings to use later
                keyboardHeight = height;
                setEmojiPickerBounds();
                animatePan(keyboardHeight);
            }
        });
    }

    // Sets the translationY of views to height with animation
    private void animatePan(final int height) {
        if (animatorSet != null && animatorSet.isStarted()) {
            animatorSet.cancel();
        }
        final ImmutableList.Builder<Animator> builder = ImmutableList.builder();
        builder.add(
                ObjectAnimator.ofFloat(binding.chats, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.input, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.inputBg, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.recordView, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.emojiToggle, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.gallery, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.camera, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.send, TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(binding.emojiPicker, TRANSLATION_Y, keyboardHeight - height)
        );
        // if (headerItemDecoration != null && headerItemDecoration.getCurrentHeader() != null) {
        //     builder.add(ObjectAnimator.ofFloat(headerItemDecoration.getCurrentHeader(), TRANSLATION_Y, height));
        // }
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(builder.build());
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(CubicBezierInterpolator.EASE_IN);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                binding.emojiPicker.setAlpha(1);
                animatorSet = null;
            }
        });
        animatorSet.start();
    }
}
