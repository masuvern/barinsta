package awais.instagrabber.activities;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.FollowAdapter;
import awais.instagrabber.asyncs.FollowFetcher;
import awais.instagrabber.databinding.FragmentFollowersViewerBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FollowModel;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;
import thoughtbot.expandableadapter.ExpandableGroup;

import static awais.instagrabber.utils.Utils.logCollector;

public final class FollowViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FollowViewerFragment";

    private final ArrayList<FollowModel> followModels = new ArrayList<>();
    private final ArrayList<FollowModel> followingModels = new ArrayList<>();
    private final ArrayList<FollowModel> followersModels = new ArrayList<>();
    private final ArrayList<FollowModel> allFollowing = new ArrayList<>();

    private boolean isFollowersList, isCompare = false;
    private String profileId, username, namePost, type;
    private Resources resources;
    private FollowModel model;
    private FollowAdapter adapter;
    private View.OnClickListener clickListener;
    private FragmentFollowersViewerBinding binding;
    private AsyncTask<Void, Void, FollowModel[]> currentlyExecuting;
    private SwipeRefreshLayout root;
    private boolean shouldRefresh = true;
    private AppCompatActivity fragmentActivity;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentFollowersViewerBinding.inflate(getLayoutInflater());
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
        if (getArguments() == null) return;
        final FollowViewerFragmentArgs fragmentArgs = FollowViewerFragmentArgs.fromBundle(getArguments());
        profileId = fragmentArgs.getProfileId();
        isFollowersList = fragmentArgs.getIsFollowersList();
        username = fragmentArgs.getUsername();
        namePost = username;
        if (Utils.isEmpty(username)) {
            // this usually should not occur
            username = "You";
            namePost = "You're";
        }
        setTitle(username);
        resources = getResources();
        clickListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof FollowModel) {
                model = (FollowModel) tag;
                final FollowViewerFragmentDirections.ActionFollowViewerFragmentToProfileFragment action = FollowViewerFragmentDirections
                        .actionFollowViewerFragmentToProfileFragment();
                action.setUsername("@" + model.getUsername());
                NavHostFragment.findNavController(this).navigate(action);
            }
        };
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        onRefresh();
    }

    private void setTitle(final String title) {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setTitle(title);
    }

    private void setSubtitle(final String subtitle) {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setSubtitle(subtitle);
    }

    private void setSubtitle(@SuppressWarnings("SameParameterValue") final int subtitleRes) {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setSubtitle(subtitleRes);
    }

    @Override
    public void onRefresh() {
        if (isCompare) listCompare();
        else listFollows();
    }

    private void listFollows() {
        stopCurrentExecutor();
        type = resources.getString(isFollowersList ? R.string.followers_type_followers : R.string.followers_type_following);
        setSubtitle(type);
        followModels.clear();
        final FetchListener<FollowModel[]> fetchListener = new FetchListener<FollowModel[]>() {
            @Override
            public void doBefore() {
                binding.swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onResult(final FollowModel[] result) {
                if (result == null) binding.swipeRefreshLayout.setRefreshing(false);
                else {
                    followModels.addAll(Arrays.asList(result));

                    final FollowModel model = result[result.length - 1];
                    if (model != null && model.hasNextPage()) {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(profileId, isFollowersList, model.getEndCursor(), this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        model.setPageCursor(false, null);
                    } else {
                        binding.swipeRefreshLayout.setRefreshing(false);

                        refreshAdapter(followModels, null, null, null);
                    }
                }
            }
        };
        currentlyExecuting = new FollowFetcher(profileId, isFollowersList, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void listCompare() {
        stopCurrentExecutor();
        setSubtitle(R.string.followers_compare);
        allFollowing.clear();
        followersModels.clear();
        followingModels.clear();
        final FetchListener<FollowModel[]> followingFetchListener = new FetchListener<FollowModel[]>() {
            @Override
            public void onResult(final FollowModel[] result) {
                if (result != null) {
                    followingModels.addAll(Arrays.asList(result));

                    final FollowModel model = result[result.length - 1];
                    if (model != null && model.hasNextPage()) {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(profileId, false, model.getEndCursor(), this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        model.setPageCursor(false, null);
                    } else {
                        allFollowing.addAll(followersModels);
                        allFollowing.retainAll(followingModels);

                        for (final FollowModel followModel : allFollowing) {
                            followersModels.remove(followModel);
                            followingModels.remove(followModel);
                        }

                        allFollowing.trimToSize();
                        followersModels.trimToSize();
                        followingModels.trimToSize();

                        binding.swipeRefreshLayout.setRefreshing(false);

                        refreshAdapter(null, followingModels, followersModels, allFollowing);
                    }
                } else binding.swipeRefreshLayout.setRefreshing(false);
            }
        };
        final FetchListener<FollowModel[]> followersFetchListener = new FetchListener<FollowModel[]>() {
            @Override
            public void doBefore() {
                binding.swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onResult(final FollowModel[] result) {
                if (result != null) {
                    followersModels.addAll(Arrays.asList(result));
                    final FollowModel model = result[result.length - 1];
                    if (model == null || !model.hasNextPage()) {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(profileId, false, followingFetchListener)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(profileId, true, model.getEndCursor(), this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        model.setPageCursor(false, null);
                    }
                }
            }
        };
        currentlyExecuting = new FollowFetcher(profileId, true, followersFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.follow, menu);
        final MenuItem favItem = menu.findItem(R.id.favourites);
        if (favItem != null) {
            favItem.setVisible(false);
        }
        final MenuItem menuSearch = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // private final Filter filter = new Filter() {
            //     private final ArrayList<FollowModel> searchFollowModels = new ArrayList<>(followModels.size() / 2);
            //     private final ArrayList<FollowModel> searchFollowingModels = new ArrayList<>(followingModels.size() / 2);
            //     private final ArrayList<FollowModel> searchFollowersModels = new ArrayList<>(followersModels.size() / 2);
            //     private final ArrayList<FollowModel> searchAllFollowing = new ArrayList<>(allFollowing.size() / 2);
            //
            //     @Nullable
            //     @Override
            //     protected FilterResults performFiltering(@NonNull final CharSequence constraint) {
            //         searchFollowModels.clear();
            //         searchFollowingModels.clear();
            //         searchFollowersModels.clear();
            //         searchAllFollowing.clear();
            //
            //         final int followModelsSize = followModels.size();
            //         final int followingModelsSize = followingModels.size();
            //         final int followersModelsSize = followersModels.size();
            //         final int allFollowingSize = allFollowing.size();
            //
            //         int maxSize = followModelsSize;
            //         if (maxSize < followingModelsSize) maxSize = followingModelsSize;
            //         if (maxSize < followersModelsSize) maxSize = followersModelsSize;
            //         if (maxSize < allFollowingSize) maxSize = allFollowingSize;
            //
            //         final String query = constraint.toString().toLowerCase();
            //         FollowModel followModel;
            //         while (maxSize != -1) {
            //             if (maxSize < followModelsSize) {
            //                 followModel = followModels.get(maxSize);
            //                 if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
            //                     searchFollowModels.add(followModel);
            //             }
            //
            //             if (maxSize < followingModelsSize) {
            //                 followModel = followingModels.get(maxSize);
            //                 if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
            //                     searchFollowingModels.add(followModel);
            //             }
            //
            //             if (maxSize < followersModelsSize) {
            //                 followModel = followersModels.get(maxSize);
            //                 if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
            //                     searchFollowersModels.add(followModel);
            //             }
            //
            //             if (maxSize < allFollowingSize) {
            //                 followModel = allFollowing.get(maxSize);
            //                 if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
            //                     searchAllFollowing.add(followModel);
            //             }
            //
            //             --maxSize;
            //         }
            //
            //         return null;
            //     }
            //
            //     @Override
            //     protected void publishResults(final CharSequence query, final FilterResults results) {
            //         refreshAdapter(searchFollowModels, searchFollowingModels, searchFollowersModels, searchAllFollowing);
            //     }
            // };

            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                // if (Utils.isEmpty(query)) refreshAdapter(followModels, followingModels, followersModels, allFollowing);
                // else filter.filter(query.toLowerCase());
                if (adapter != null) adapter.getFilter().filter(query);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() != R.id.action_compare) return super.onOptionsItemSelected(item);
        binding.rvFollow.setAdapter(null);
        if (isCompare) listFollows();
        else listCompare();
        isCompare = !isCompare;
        return true;
    }

    private void refreshAdapter(final ArrayList<FollowModel> followModels,
                                final ArrayList<FollowModel> followingModels,
                                final ArrayList<FollowModel> followersModels,
                                final ArrayList<FollowModel> allFollowing) {
        final ArrayList<ExpandableGroup> groups = new ArrayList<>(1);

        if (isCompare) {
            if (followingModels != null && followingModels.size() > 0)
                groups.add(new ExpandableGroup(resources.getString(R.string.followers_not_following, username), followingModels));
            if (followersModels != null && followersModels.size() > 0)
                groups.add(new ExpandableGroup(resources.getString(R.string.followers_not_follower, namePost), followersModels));
            if (allFollowing != null && allFollowing.size() > 0)
                groups.add(new ExpandableGroup(resources.getString(R.string.followers_both_following), allFollowing));
        } else {
            final ExpandableGroup group = new ExpandableGroup(type, followModels);
            groups.add(group);
        }

        adapter = new FollowAdapter(requireContext(), clickListener, groups);
        adapter.toggleGroup(0);
        binding.rvFollow.setAdapter(adapter);
    }

    public void stopCurrentExecutor() {
        if (currentlyExecuting != null) {
            try {
                currentlyExecuting.cancel(true);
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }
}