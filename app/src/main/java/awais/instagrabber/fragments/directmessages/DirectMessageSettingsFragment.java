package awais.instagrabber.fragments.directmessages;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import awais.instagrabber.R;
import awais.instagrabber.broadcasts.DMRefreshBroadcastReceiver;
import awais.instagrabber.databinding.FragmentDirectMessagesSettingsBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public class DirectMessageSettingsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "DirectMsgsSettingsFrag";

    private RecyclerView userList;
    private RecyclerView leftUserList;
    private EditText titleText;
    private View leftTitle;
    private AppCompatImageView titleSend;
    private String threadId;
    private String threadTitle;
    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    // private AsyncTask<Void, Void, InboxThreadModel> currentlyRunning;
    private View.OnClickListener clickListener;
    private View.OnClickListener basicClickListener;

    // private final FetchListener<InboxThreadModel> fetchListener = new FetchListener<InboxThreadModel>() {
    //     @Override
    //     public void doBefore() {}
    //
    //     @Override
    //     public void onResult(final InboxThreadModel threadModel) {
    //         if (threadModel == null) return;
    //         final List<Long> adminList = threadModel.getAdmins();
    //         final String userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
    //         if (userIdFromCookie == null) return;
    //         final boolean amAdmin = adminList.contains(Long.parseLong(userIdFromCookie));
    //         final DirectMessageMembersAdapter memberAdapter = new DirectMessageMembersAdapter(threadModel.getUsers(),
    //                                                                                           adminList,
    //                                                                                           amAdmin ? clickListener : basicClickListener);
    //         userList.setAdapter(memberAdapter);
    //         if (threadModel.getLeftUsers() != null && threadModel.getLeftUsers().size() > 0) {
    //             leftTitle.setVisibility(View.VISIBLE);
    //             final DirectMessageMembersAdapter leftAdapter = new DirectMessageMembersAdapter(threadModel.getLeftUsers(),
    //                                                                                             null,
    //                                                                                             basicClickListener);
    //             leftUserList.setAdapter(leftAdapter);
    //         }
    //     }
    // };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        basicClickListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof ProfileModel) {
                ProfileModel model = (ProfileModel) tag;
                final Bundle bundle = new Bundle();
                bundle.putString("username", "@" + model.getUsername());
                NavHostFragment.findNavController(this).navigate(R.id.action_global_profileFragment, bundle);
            }
        };

        clickListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof ProfileModel) {
                ProfileModel model = (ProfileModel) tag;
                final Context context = getContext();
                if (context == null) return;
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new String[]{
                        getString(R.string.open_profile),
                        getString(R.string.dms_action_kick),
                });
                final DialogInterface.OnClickListener clickListener = (d, w) -> {
                    if (w == 0) {
                        final Bundle bundle = new Bundle();
                        bundle.putString("username", "@" + model.getUsername());
                        NavHostFragment.findNavController(this).navigate(R.id.action_global_profileFragment, bundle);
                    } else if (w == 1) {
                        new ChangeSettings(titleText.getText().toString()).execute("remove_users", model.getId());
                        onRefresh();
                    }
                };
                new AlertDialog.Builder(context)
                        .setAdapter(adapter, clickListener)
                        .show();
            }
        };
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final FragmentDirectMessagesSettingsBinding binding = FragmentDirectMessagesSettingsBinding.inflate(inflater, container, false);
        final LinearLayout root = binding.getRoot();
        final Context context = getContext();
        if (context == null) return root;
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        final LinearLayoutManager layoutManagerDos = new LinearLayoutManager(context) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        if (getArguments() == null) {
            return root;
        }
        threadId = DirectMessageSettingsFragmentArgs.fromBundle(getArguments()).getThreadId();
        threadTitle = DirectMessageSettingsFragmentArgs.fromBundle(getArguments()).getTitle();
        binding.swipeRefreshLayout.setEnabled(false);

        userList = binding.userList;
        userList.setHasFixedSize(true);
        userList.setLayoutManager(layoutManager);

        leftUserList = binding.leftUserList;
        leftUserList.setHasFixedSize(true);
        leftUserList.setLayoutManager(layoutManagerDos);

        leftTitle = binding.leftTitle;

        titleText = binding.titleText;
        titleText.setText(threadTitle);

        titleSend = binding.titleSend;
        titleSend.setOnClickListener(v -> new ChangeSettings(titleText.getText().toString()).execute("update_title"));

        titleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                titleSend.setVisibility(s.toString().equals(threadTitle) ? View.GONE : View.VISIBLE);
            }
        });

        final AppCompatButton btnLeave = binding.btnLeave;
        btnLeave.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle(R.string.dms_action_leave_question)
                .setPositiveButton(R.string.yes,
                                   (x, y) -> new ChangeSettings(titleText.getText().toString()).execute("leave"))
                .setNegativeButton(R.string.no, null)
                .show());

        // currentlyRunning = new DirectMessageInboxThreadFetcher(threadId, null, null, fetchListener).execute();
        return root;
    }

    @Override
    public void onRefresh() {
        stopCurrentExecutor();
        // currentlyRunning = new DirectMessageInboxThreadFetcher(threadId, null, null, fetchListener).execute();
    }

    private void stopCurrentExecutor() {
        // if (currentlyRunning != null) {
        //     try {
        //         currentlyRunning.cancel(true);
        //     } catch (final Exception e) {
        //         if (BuildConfig.DEBUG) {
        //             Log.e(TAG, "", e);
        //         }
        //     }
        // }
    }

    class ChangeSettings extends AsyncTask<String, Void, Void> {
        String action, argument;
        boolean ok = false;
        private String text;

        public ChangeSettings(final String text) {
            this.text = text;
        }

        protected Void doInBackground(String... rawAction) {
            action = rawAction[0];
            if (rawAction.length == 2) argument = rawAction[1];
            final String url = "https://i.instagram.com/api/v1/direct_v2/threads/" + threadId + "/" + action + "/";
            try {
                String urlParameters = "_csrftoken=" + cookie.split("csrftoken=")[1].split(";")[0]
                        + "&_uuid=" + Utils.settingsHelper.getString(Constants.DEVICE_UUID);
                if (action.equals("update_title")) {
                    urlParameters += "&title=" + URLEncoder.encode(text, "UTF-8")
                                                           .replaceAll("\\+", "%20").replaceAll("%21", "!").replaceAll("%27", "'")
                                                           .replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("%7E", "~");
                } else if (action.startsWith("remove_users"))
                    urlParameters += ("&user_ids=[" + argument + "]");
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
                    ok = true;
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "unsend: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            final Context context = getContext();
            if (context == null) return;
            if (ok) {
                Toast.makeText(context, R.string.dms_action_success, Toast.LENGTH_SHORT).show();
                if (action.equals("update_title")) {
                    threadTitle = titleText.getText().toString();
                    titleSend.setVisibility(View.GONE);
                    titleText.clearFocus();
                    DirectMessageThreadFragment.hasSentSomething = true;
                } else if (action.equals("leave")) {
                    context.sendBroadcast(new Intent(DMRefreshBroadcastReceiver.ACTION_REFRESH_DM));
                    NavHostFragment.findNavController(DirectMessageSettingsFragment.this).popBackStack(R.id.directMessagesInboxFragment, false);
                } else {
                    DirectMessageThreadFragment.hasSentSomething = true;
                }
            } else Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        }
    }
}
