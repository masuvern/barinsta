package awais.instagrabber.fragments.directmessages;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import awais.instagrabber.ProfileNavGraphDirections;
import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectMessageItemsAdapter;
import awais.instagrabber.asyncs.ImageUploader;
import awais.instagrabber.asyncs.direct_messages.DirectMessageInboxThreadFetcher;
import awais.instagrabber.asyncs.direct_messages.DirectThreadBroadcaster;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentDirectMessagesThreadBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ImageUploadOptions;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.enums.UserInboxDirection;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class DirectMessageThreadFragment extends Fragment {
    private static final String TAG = "DirectMessagesThreadFmt";
    private static final int PICK_IMAGE = 100;

    private AppCompatActivity fragmentActivity;
    private String threadId;
    private String threadTitle;
    private String cursor;
    private String lastMessage;
    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    private final String myId = CookieUtils.getUserIdFromCookie(cookie);
    private FragmentDirectMessagesThreadBinding binding;
    private DirectItemModelListViewModel listViewModel;
    private DirectItemModel directItemModel;
    private RecyclerView messageList;
    private boolean hasDeletedSomething;
    private boolean hasOlder = true;
    public static boolean hasSentSomething;

    private final ProfileModel myProfileHolder = ProfileModel.getDefaultProfileModel();
    private final List<ProfileModel> users = new ArrayList<>();
    private final List<ProfileModel> leftUsers = new ArrayList<>();
    private ArrayAdapter<String> dialogAdapter;

    private final View.OnClickListener clickListener = v -> {
        if (v == binding.commentSend) {
            final String text = binding.commentText.getText().toString();
            if (TextUtils.isEmpty(text)) {
                final Context context = getContext();
                if (context == null) return;
                Toast.makeText(context, R.string.comment_send_empty_comment, Toast.LENGTH_SHORT).show();
                return;
            }
            sendText(text, null, false);
            return;
        }
        if (v == binding.image) {
            final Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE);
        }
    };

    private final FetchListener<InboxThreadModel> fetchListener = new FetchListener<InboxThreadModel>() {
        @Override
        public void doBefore() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final InboxThreadModel result) {
            if (result == null && ("MINCURSOR".equals(cursor) || "MAXCURSOR".equals(cursor) || TextUtils.isEmpty(cursor))) {
                final Context context = getContext();
                if (context == null) return;
                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            }

            if (result != null) {
                cursor = result.getOldestCursor();
                hasOlder = result.hasOlder();
                if ("MINCURSOR".equals(cursor) || "MAXCURSOR".equals(cursor)) {
                    cursor = null;
                }
                users.clear();
                users.addAll(Arrays.asList(result.getUsers()));
                leftUsers.clear();
                leftUsers.addAll(Arrays.asList(result.getLeftUsers()));

                List<DirectItemModel> list = listViewModel.getList().getValue();
                final List<DirectItemModel> newList = Arrays.asList(result.getItems());
                list = list != null ? new LinkedList<>(list) : new LinkedList<>();
                if (hasSentSomething || hasDeletedSomething) {
                    list = newList;
                    final Handler handler = new Handler();
                    if (hasSentSomething) handler.postDelayed(() -> {
                        if (messageList != null) {
                            messageList.smoothScrollToPosition(0);
                        }
                    }, 200);
                    hasSentSomething = false;
                    hasDeletedSomething = false;
                } else {
                    list.addAll(newList);
                }
                listViewModel.getList().postValue(list);

                lastMessage = result.getNewestCursor();

                if (Utils.settingsHelper.getBoolean(Constants.DM_MARK_AS_SEEN)) new ThreadAction().execute("seen", lastMessage);
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    };
    private LinearLayout root;
    private boolean shouldRefresh = true;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) requireActivity();
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
        listViewModel = new ViewModelProvider(fragmentActivity).get(DirectItemModelListViewModel.class);
        if (getArguments() == null) return;
        if (!DirectMessageThreadFragmentArgs.fromBundle(getArguments()).getThreadId().equals(threadId)) {
            listViewModel.empty();
            threadId = DirectMessageThreadFragmentArgs.fromBundle(getArguments()).getThreadId();
        }
        threadTitle = DirectMessageThreadFragmentArgs.fromBundle(getArguments()).getTitle();
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(threadTitle);
        }
        binding.swipeRefreshLayout.setEnabled(false);
        messageList = binding.messageList;
        messageList.setHasFixedSize(true);
        binding.commentSend.setOnClickListener(clickListener);
        binding.image.setOnClickListener(clickListener);
        final Context context = getContext();
        if (context == null) return;
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setReverseLayout(true);
        messageList.setLayoutManager(layoutManager);
        messageList.addOnScrollListener(new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (TextUtils.isEmpty(cursor) || !hasOlder) {
                return;
            }
            new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, cursor, fetchListener)
                    .execute(); // serial because we don't want messages to be randomly ordered
        }));
        final DialogInterface.OnClickListener onDialogListener = (dialogInterface, which) -> {
            if (which == 0) {
                final DirectItemType itemType = directItemModel.getItemType();
                switch (itemType) {
                    case MEDIA_SHARE:
                    case CLIP:
                    case FELIX_SHARE:
                        final long postId = directItemModel.getMediaModel().getPk();
                        // open post
                        break;
                    case LINK:
                        Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                        linkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        linkIntent.setData(Uri.parse(directItemModel.getLinkModel().getLinkContext().getLinkUrl()));
                        startActivity(linkIntent);
                        break;
                    case TEXT:
                    case REEL_SHARE:
                        Utils.copyText(context, directItemModel.getText());
                        Toast.makeText(context, R.string.clipboard_copied, Toast.LENGTH_SHORT).show();
                        break;
                    case RAVEN_MEDIA:
                    case MEDIA:
                        final ProfileModel user = getUser(directItemModel.getUserId());
                        final DirectItemModel.DirectItemMediaModel selectedItem =
                                itemType == DirectItemType.MEDIA ? directItemModel.getMediaModel() : directItemModel.getRavenMediaModel().getMedia();
                        final String url = selectedItem.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO
                                           ? selectedItem.getVideoUrl()
                                           : selectedItem.getThumbUrl();
                        if (url == null) {
                            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                        } else {
                            DownloadUtils.dmDownload(context, user.getUsername(), DownloadMethod.DOWNLOAD_DIRECT, selectedItem);
                            Toast.makeText(context, R.string.downloader_downloading_media, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case STORY_SHARE:
                        if (directItemModel.getReelShare() != null) {
                            // StoryModel sm = new StoryModel(
                            //         directItemModel.getReelShare().getReelId(),
                            //         directItemModel.getReelShare().getMedia().getVideoUrl(),
                            //         directItemModel.getReelShare().getMedia().getMediaType(),
                            //         directItemModel.getTimestamp(),
                            //         directItemModel.getReelShare().getReelOwnerName(),
                            //         String.valueOf(directItemModel.getReelShare().getReelOwnerId()),
                            //         false
                            // );
                            // sm.setVideoUrl(directItemModel.getReelShare().getMedia().getVideoUrl());
                            // StoryModel[] sms = {sm};
                            // startActivity(new Intent(getContext(), StoryViewer.class)
                            //         .putExtra(Constants.EXTRAS_USERNAME, directItemModel.getReelShare().getReelOwnerName())
                            //         .putExtra(Constants.EXTRAS_STORIES, sms)
                            // );
                        } else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
                            searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
                        }
                        break;
                    case PLACEHOLDER:
                        if (directItemModel.getText().toString().contains("@"))
                            searchUsername(directItemModel.getText().toString().split("@")[1].split(" ")[0]);
                        break;
                    default:
                        Log.d(TAG, "unsupported type " + itemType);
                }
            } else if (which == 1) {
                sendText(null, directItemModel.getItemId(), directItemModel.isLiked());
            } else if (which == 2) {
                if (directItemModel == null)
                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                else if (String.valueOf(directItemModel.getUserId()).equals(myId))
                    new ThreadAction().execute("delete", directItemModel.getItemId());
                else searchUsername(getUser(directItemModel.getUserId()).getUsername());
            }
        };
        final View.OnClickListener onClickListener = v -> {
            Object tag = v.getTag();
            if (tag instanceof ProfileModel) {
                searchUsername(((ProfileModel) tag).getUsername());
            } else if (tag instanceof DirectItemModel) {
                directItemModel = (DirectItemModel) tag;
                final DirectItemType itemType = directItemModel.getItemType();
                int firstOption = R.string.dms_inbox_raven_message_unknown;
                String[] dialogList;

                switch (itemType) {
                    case MEDIA_SHARE:
                    case CLIP:
                    case FELIX_SHARE:
                        firstOption = R.string.view_post;
                        break;
                    case LINK:
                        firstOption = R.string.dms_inbox_open_link;
                        break;
                    case TEXT:
                    case REEL_SHARE:
                        firstOption = R.string.dms_inbox_copy_text;
                        break;
                    case RAVEN_MEDIA:
                    case MEDIA:
                        firstOption = R.string.dms_inbox_download;
                        break;
                    case STORY_SHARE:
                        if (directItemModel.getReelShare() != null) {
                            firstOption = R.string.show_stories;
                        } else if (directItemModel.getText() != null && directItemModel.getText().toString().contains("@")) {
                            firstOption = R.string.open_profile;
                        }
                        break;
                    case PLACEHOLDER:
                        if (directItemModel.getText().toString().contains("@"))
                            firstOption = R.string.open_profile;
                        break;
                }

                dialogList = new String[]{
                        getString(firstOption),
                        getString(directItemModel.isLiked() ? R.string.dms_inbox_unlike : R.string.dms_inbox_like),
                        getString(String.valueOf(directItemModel.getUserId()).equals(myId) ? R.string.dms_inbox_unsend : R.string.dms_inbox_author)
                };

                dialogAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, dialogList);

                new AlertDialog.Builder(context)
                        .setAdapter(dialogAdapter, onDialogListener)
                        .show();
            }
        };
        final MentionClickListener mentionClickListener = (view, text, isHashtag, isLocation) -> searchUsername(text);
        final DirectMessageItemsAdapter adapter = new DirectMessageItemsAdapter(users, leftUsers, onClickListener, mentionClickListener);
        messageList.setAdapter(adapter);
        listViewModel.getList().observe(fragmentActivity, adapter::submitList);
        if (listViewModel.isEmpty()) {
            new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasSentSomething) {
            new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(threadTitle);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.dm_thread_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.info:
                final NavDirections action = DirectMessageThreadFragmentDirections.actionDMThreadFragmentToDMSettingsFragment(threadId, threadTitle);
                NavHostFragment.findNavController(this).navigate(action);
                return true;
            case R.id.mark_as_seen:
                new ThreadAction().execute("seen", lastMessage);
                item.setVisible(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Log.w(TAG, "data is null!");
                return;
            }
            final Uri uri = data.getData();
            sendImage(uri);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listViewModel != null) listViewModel.getList().postValue(Collections.emptyList());
    }

    private void sendText(final String text, final String itemId, final boolean delete) {
        DirectThreadBroadcaster.TextBroadcastOptions textOptions = null;
        DirectThreadBroadcaster.ReactionBroadcastOptions reactionOptions = null;
        if (text != null) {
            try {
                textOptions = new DirectThreadBroadcaster.TextBroadcastOptions(text);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error", e);
                return;
            }
        } else {
            reactionOptions = new DirectThreadBroadcaster.ReactionBroadcastOptions(itemId, delete);
        }
        broadcast(text != null ? textOptions : reactionOptions, result -> {
            if (result == null || result.getResponseCode() != HttpURLConnection.HTTP_OK) {
                final Context context = getContext();
                if (context == null) return;
                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (text != null) {
                binding.commentText.setText("");
            } else {
                final LinearLayout dim = (LinearLayout) binding.messageList.findViewWithTag(directItemModel).getParent();
                if (dim.findViewById(R.id.liked_container) != null) {
                    dim.findViewById(R.id.liked_container).setVisibility(delete ? View.GONE : View.VISIBLE);
                }
                directItemModel.setLiked();
            }
            DirectMessageInboxFragment.refreshPlease = true;
            hasSentSomething = true;
            new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
    }

    private void sendImage(final Uri imageUri) {
        final Context context = getContext();
        if (context == null) return;
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Toast.makeText(context, R.string.uploading, Toast.LENGTH_SHORT).show();
            // Upload Image
            final ImageUploader imageUploader = new ImageUploader();
            imageUploader.setOnTaskCompleteListener(response -> {
                if (response == null || response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    if (response != null && response.getResponse() != null) {
                        Log.e(TAG, response.getResponse().toString());
                    }
                    return;
                }
                final JSONObject responseJson = response.getResponse();
                try {
                    final String uploadId = responseJson.getString("upload_id");
                    // Broadcast
                    final DirectThreadBroadcaster.ImageBroadcastOptions options = new DirectThreadBroadcaster.ImageBroadcastOptions(true, uploadId);
                    hasSentSomething = true;
                    broadcast(options,
                              broadcastResponse -> new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
                                      .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing json response", e);
                }
            });
            final ImageUploadOptions options = ImageUploadOptions.builder(bitmap).build();
            imageUploader.execute(options);
        } catch (IOException e) {
            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening file", e);
        }
    }

    private void broadcast(final DirectThreadBroadcaster.BroadcastOptions broadcastOptions,
                           final DirectThreadBroadcaster.OnBroadcastCompleteListener listener) {
        final DirectThreadBroadcaster broadcaster = new DirectThreadBroadcaster(threadId);
        broadcaster.setOnTaskCompleteListener(listener);
        broadcaster.execute(broadcastOptions);
    }

    @NonNull
    private ProfileModel getUser(final long userId) {
        ProfileModel result = myProfileHolder;
        for (final ProfileModel user : users) {
            if (Long.toString(userId).equals(user.getId())) result = user;
        }
        for (final ProfileModel leftUser : leftUsers) {
            if (Long.toString(userId).equals(leftUser.getId())) result = leftUser;
        }
        return result;
    }

    private void searchUsername(final String text) {
        final NavDirections action = DirectMessageThreadFragmentDirections.actionGlobalProfileFragment("@" + text);
        NavHostFragment.findNavController(this).navigate(action);
    }

    public static class DirectItemModelListViewModel extends ViewModel {
        private MutableLiveData<List<DirectItemModel>> list;
        private boolean isEmpty;

        public MutableLiveData<List<DirectItemModel>> getList() {
            if (list == null) {
                list = new MutableLiveData<>();
                isEmpty = true;
            } else isEmpty = false;
            return list;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void empty() {
            list = null;
            isEmpty = true;
        }
    }

    class ThreadAction extends AsyncTask<String, Void, Void> {
        String action, argument;

        protected Void doInBackground(String... rawAction) {
            action = rawAction[0];
            argument = rawAction[1];
            final String url = "https://i.instagram.com/api/v1/direct_v2/threads/" + threadId + "/items/" + argument + "/" + action + "/";
            try {
                String urlParameters = "_csrftoken=" + cookie.split("csrftoken=")[1].split(";")[0]
                        + "&_uuid=" + Utils.settingsHelper.getString(Constants.DEVICE_UUID);
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    if (action.equals("delete")) hasDeletedSomething = true;
                    else if (action.equals("seen")) DirectMessageInboxFragment.refreshPlease = true;
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", action + ": " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (hasDeletedSomething) {
                directItemModel = null;
                new DirectMessageInboxThreadFetcher(threadId, UserInboxDirection.OLDER, null, fetchListener)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
}
