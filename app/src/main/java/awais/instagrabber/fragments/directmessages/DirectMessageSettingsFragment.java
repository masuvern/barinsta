package awais.instagrabber.fragments.directmessages;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectUsersAdapter;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.databinding.FragmentDirectMessagesSettingsBinding;
import awais.instagrabber.dialogs.MultiOptionDialogFragment;
import awais.instagrabber.dialogs.MultiOptionDialogFragment.Option;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectUser;
import awais.instagrabber.viewmodels.DirectInboxViewModel;
import awais.instagrabber.viewmodels.DirectSettingsViewModel;

public class DirectMessageSettingsFragment extends Fragment {
    private static final String TAG = DirectMessageSettingsFragment.class.getSimpleName();

    private FragmentDirectMessagesSettingsBinding binding;
    private DirectSettingsViewModel viewModel;
    private DirectUsersAdapter usersAdapter;
    private List<Option<String>> options;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final NavController navController = NavHostFragment.findNavController(this);
        final ViewModelStoreOwner viewModelStoreOwner = navController.getViewModelStoreOwner(R.id.direct_messages_nav_graph);
        final DirectInboxViewModel inboxViewModel = new ViewModelProvider(viewModelStoreOwner).get(DirectInboxViewModel.class);
        final List<DirectThread> threads = inboxViewModel.getThreads().getValue();
        final Bundle arguments = getArguments();
        if (arguments == null) {
            navController.navigateUp();
            return;
        }
        final DirectMessageSettingsFragmentArgs fragmentArgs = DirectMessageSettingsFragmentArgs.fromBundle(arguments);
        final String threadId = fragmentArgs.getThreadId();
        final Optional<DirectThread> first = threads != null ? threads.stream()
                                                                      .filter(thread -> thread.getThreadId().equals(threadId))
                                                                      .findFirst()
                                                             : Optional.empty();
        if (!first.isPresent()) {
            navController.navigateUp();
            return;
        }
        viewModel = new ViewModelProvider(this).get(DirectSettingsViewModel.class);
        viewModel.setViewer(inboxViewModel.getViewer());
        viewModel.setThread(first.get());
        // basicClickListener = v -> {
        //     final Object tag = v.getTag();
        //     if (tag instanceof ProfileModel) {
        //         ProfileModel model = (ProfileModel) tag;
        //         final Bundle bundle = new Bundle();
        //         bundle.putString("username", "@" + model.getUsername());
        //         NavHostFragment.findNavController(this).navigate(R.id.action_global_profileFragment, bundle);
        //     }
        // };
        //
        // clickListener = v -> {
        //     final Object tag = v.getTag();
        //     if (tag instanceof ProfileModel) {
        //         ProfileModel model = (ProfileModel) tag;
        //         final Context context = getContext();
        //         if (context == null) return;
        //         final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new String[]{
        //                 getString(R.string.open_profile),
        //                 getString(R.string.dms_action_kick),
        //         });
        //         final DialogInterface.OnClickListener clickListener = (d, w) -> {
        //             if (w == 0) {
        //                 final Bundle bundle = new Bundle();
        //                 bundle.putString("username", "@" + model.getUsername());
        //                 NavHostFragment.findNavController(this).navigate(R.id.action_global_profileFragment, bundle);
        //             } else if (w == 1) {
        //                 new ChangeSettings(titleText.getText().toString()).execute("remove_users", model.getId());
        //                 onRefresh();
        //             }
        //         };
        //         new AlertDialog.Builder(context)
        //                 .setAdapter(adapter, clickListener)
        //                 .show();
        //     }
        // };
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = FragmentDirectMessagesSettingsBinding.inflate(inflater, container, false);
        // final String threadId = DirectMessageSettingsFragmentArgs.fromBundle(getArguments()).getThreadId();
        // threadTitle = DirectMessageSettingsFragmentArgs.fromBundle(getArguments()).getTitle();
        // binding.swipeRefreshLayout.setEnabled(false);

        // final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        // if (actionBar != null) {
        //     actionBar.setTitle(threadTitle);
        // }

        // titleSend.setOnClickListener(v -> new ChangeSettings(titleText.getText().toString()).execute("update_title"));

        // binding.titleText.addTextChangedListener(new TextWatcherAdapter() {
        //     @Override
        //     public void onTextChanged(CharSequence s, int start, int before, int count) {
        //         binding.titleSend.setVisibility(s.toString().equals(threadTitle) ? View.GONE : View.VISIBLE);
        //     }
        // });

        // final AppCompatButton btnLeave = binding.btnLeave;
        // btnLeave.setOnClickListener(v -> new AlertDialog.Builder(context)
        //         .setTitle(R.string.dms_action_leave_question)
        //         .setPositiveButton(R.string.yes, (x, y) -> new ChangeSettings(titleText.getText().toString()).execute("leave"))
        //         .setNegativeButton(R.string.no, null)
        //         .show());

        // currentlyRunning = new DirectMessageInboxThreadFetcher(threadId, null, null, fetchListener).execute();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        init();
        setupObservers();
    }

    private void setupObservers() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (usersAdapter == null) return;
            usersAdapter.submitUsers(users.first, users.second);
        });
        viewModel.getTitle().observe(getViewLifecycleOwner(), title -> binding.titleEdit.setText(title));
        viewModel.getAdminUserIds().observe(getViewLifecycleOwner(), adminUserIds -> {
            if (usersAdapter == null) return;
            usersAdapter.setAdminUserIds(adminUserIds);
        });
        final NavController navController = NavHostFragment.findNavController(this);
        final NavBackStackEntry backStackEntry = navController.getCurrentBackStackEntry();
        if (backStackEntry != null) {
            final MutableLiveData<Object> resultLiveData = backStackEntry.getSavedStateHandle().getLiveData("result");
            resultLiveData.observe(getViewLifecycleOwner(), result -> {
                LiveData<Resource<Object>> detailsChangeResourceLiveData = null;
                if ((result instanceof DirectUser)) {
                    // Log.d(TAG, "result: " + result);
                    detailsChangeResourceLiveData = viewModel.addMembers(Collections.singleton((DirectUser) result));
                } else if ((result instanceof Set)) {
                    try {
                        // Log.d(TAG, "result: " + result);
                        //noinspection unchecked
                        detailsChangeResourceLiveData = viewModel.addMembers((Set<DirectUser>) result);
                    } catch (Exception e) {
                        Log.e(TAG, "search users result: ", e);
                    }
                }
                if (detailsChangeResourceLiveData != null) {
                    observeDetailsChange(detailsChangeResourceLiveData);
                }
            });
        }
    }

    private void init() {
        setupSettings();
        setupMembers();
    }

    private void setupSettings() {
        binding.groupSettings.setVisibility(viewModel.isGroup() ? View.VISIBLE : View.GONE);
        if (!viewModel.isGroup()) return;
        binding.titleEdit.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (s.toString().trim().equals(viewModel.getTitle().getValue())) {
                    binding.titleEditInputLayout.setSuffixText(null);
                    return;
                }
                binding.titleEditInputLayout.setSuffixText(getString(R.string.save));
            }
        });
        binding.titleEditInputLayout.getSuffixTextView().setOnClickListener(v -> {
            final Editable text = binding.titleEdit.getText();
            if (text == null) return;
            final String newTitle = text.toString().trim();
            if (newTitle.equals(viewModel.getTitle().getValue())) return;
            observeDetailsChange(viewModel.updateTitle(newTitle));
        });
        binding.addMembers.setOnClickListener(v -> {
            if (!isAdded()) return;
            final NavController navController = NavHostFragment.findNavController(this);
            final NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination == null) return;
            if (currentDestination.getId() != R.id.directMessagesSettingsFragment) return;
            final Pair<List<DirectUser>, List<DirectUser>> users = viewModel.getUsers().getValue();
            final long[] currentUserIds;
            if (users != null && users.first != null) {
                final List<DirectUser> currentMembers = users.first;
                currentUserIds = currentMembers.stream()
                                               .mapToLong(DirectUser::getPk)
                                               .sorted()
                                               .toArray();
            } else {
                currentUserIds = new long[0];
            }
            final NavDirections directions = DirectMessageSettingsFragmentDirections.actionGlobalUserSearch(
                    true,
                    "Add users",
                    "Add",
                    currentUserIds
            );
            navController.navigate(directions);
        });
    }

    private void setupMembers() {
        final Context context = getContext();
        if (context == null) return;
        binding.users.setLayoutManager(new LinearLayoutManager(context));
        final DirectUser inviter = viewModel.getThread().getInviter();
        usersAdapter = new DirectUsersAdapter(
                inviter != null ? inviter.getPk() : -1,
                (position, user, selected) -> {
                    // navigate to profile
                },
                (position, user) -> {
                    final ArrayList<Option<String>> options = viewModel.createUserOptions(user);
                    if (options == null || options.isEmpty()) return true;
                    final MultiOptionDialogFragment<String> fragment = MultiOptionDialogFragment.newInstance(-1, options);
                    fragment.setSingleCallback(new MultiOptionDialogFragment.MultiOptionDialogSingleCallback<String>() {
                        @Override
                        public void onSelect(final String action) {
                            if (action == null) return;
                            observeDetailsChange(viewModel.doAction(user, action));
                        }

                        @Override
                        public void onCancel() {}
                    });
                    final FragmentManager fragmentManager = getChildFragmentManager();
                    fragment.show(fragmentManager, "actions");
                    return true;
                }
        );
        binding.users.setAdapter(usersAdapter);
    }

    private void observeDetailsChange(@NonNull final LiveData<Resource<Object>> detailsChangeResourceLiveData) {
        detailsChangeResourceLiveData.observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case SUCCESS:
                case LOADING:
                    break;
                case ERROR:
                    if (resource.message != null) {
                        Snackbar.make(binding.getRoot(), resource.message, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    // class ChangeSettings extends AsyncTask<String, Void, Void> {
    //     String action, argument;
    //     boolean ok = false;
    //     private final String text;
    //
    //     public ChangeSettings(final String text) {
    //         this.text = text;
    //     }
    //
    //     protected Void doInBackground(String... rawAction) {
    //         action = rawAction[0];
    //         if (rawAction.length == 2) argument = rawAction[1];
    //         final String url = "https://i.instagram.com/api/v1/direct_v2/threads/" + threadId + "/" + action + "/";
    //         try {
    //             String urlParameters = "_csrftoken=" + cookie.split("csrftoken=")[1].split(";")[0]
    //                     + "&_uuid=" + Utils.settingsHelper.getString(Constants.DEVICE_UUID);
    //             if (action.equals("update_title")) {
    //                 urlParameters += "&title=" + URLEncoder.encode(text, "UTF-8")
    //                                                        .replaceAll("\\+", "%20").replaceAll("%21", "!").replaceAll("%27", "'")
    //                                                        .replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("%7E", "~");
    //             } else if (action.startsWith("remove_users"))
    //                 urlParameters += ("&user_ids=[" + argument + "]");
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
    //                 ok = true;
    //             }
    //             urlConnection.disconnect();
    //         } catch (Throwable ex) {
    //             Log.e("austin_debug", "unsend: " + ex);
    //         }
    //         return null;
    //     }
    //
    //     @Override
    //     protected void onPostExecute(Void result) {
    //         final Context context = getContext();
    //         if (context == null) return;
    //         if (ok) {
    //             Toast.makeText(context, R.string.dms_action_success, Toast.LENGTH_SHORT).show();
    //             if (action.equals("update_title")) {
    //                 threadTitle = titleText.getText().toString();
    //                 titleSend.setVisibility(View.GONE);
    //                 titleText.clearFocus();
    //                 DirectMessageThreadFragment.hasSentSomething = true;
    //             } else if (action.equals("leave")) {
    //                 context.sendBroadcast(new Intent(DMRefreshBroadcastReceiver.ACTION_REFRESH_DM));
    //                 NavHostFragment.findNavController(DirectMessageSettingsFragment.this).popBackStack(R.id.directMessagesInboxFragment, false);
    //             } else {
    //                 DirectMessageThreadFragment.hasSentSomething = true;
    //             }
    //         } else Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    //     }
    // }
}
