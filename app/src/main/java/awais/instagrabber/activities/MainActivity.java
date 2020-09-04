package awais.instagrabber.activities;


import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.customviews.helpers.CustomHideBottomViewOnScrollBehavior;
import awais.instagrabber.databinding.ActivityMainBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.NavigationExtensions.setupWithNavController;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class MainActivity extends BaseLanguageActivity {
    private static final String TAG = "MainActivity";

    private static final List<Integer> SHOW_BOTTOM_VIEW_DESTINATIONS = Arrays.asList(
            R.id.directMessagesInboxFragment,
            R.id.feedFragment,
            R.id.profileFragment,
            R.id.discoverFragment,
            R.id.morePreferencesFragment);
    private static final List<Integer> KEEP_SCROLL_BEHAVIOUR_DESTINATIONS = Arrays.asList(
            R.id.directMessagesInboxFragment,
            R.id.feedFragment,
            R.id.profileFragment,
            R.id.discoverFragment,
            R.id.morePreferencesFragment,
            R.id.settingsPreferencesFragment,
            R.id.hashTagFragment,
            R.id.locationFragment,
            R.id.savedViewerFragment,
            R.id.commentsViewerFragment,
            R.id.followViewerFragment);
    private static final List<Integer> REMOVE_COLLAPSING_TOOLBAR_SCROLL_DESTINATIONS = Collections.singletonList(R.id.commentsViewerFragment);
    private ActivityMainBinding binding;
    private LiveData<NavController> currentNavControllerLiveData;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        Utils.setupCookies(cookie);
        setContentView(binding.getRoot());

        final Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            setupBottomNavigationBar();
        }

        setupScrollingListener();
    }

    private void setupScrollingListener() {
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.bottomNavView.getLayoutParams();
        layoutParams.setBehavior(new CustomHideBottomViewOnScrollBehavior());
        binding.bottomNavView.requestLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void setupBottomNavigationBar() {
        final List<Integer> navList = new ArrayList<>(Arrays.asList(
                R.navigation.direct_messages_nav_graph,
                R.navigation.feed_nav_graph,
                R.navigation.profile_nav_graph,
                R.navigation.discover_nav_graph,
                R.navigation.more_nav_graph
        ));
        final LiveData<NavController> navControllerLiveData = setupWithNavController(
                binding.bottomNavView,
                navList,
                getSupportFragmentManager(),
                R.id.main_nav_host,
                getIntent(),
                0);
        navControllerLiveData.observe(this, this::setupNavigation);
        currentNavControllerLiveData = navControllerLiveData;
    }

    private void setupNavigation(final NavController navController) {
        NavigationUI.setupWithNavController(binding.toolbar, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            binding.appBarLayout.setExpanded(true, true);
            final int destinationId = destination.getId();
            binding.bottomNavView.setVisibility(SHOW_BOTTOM_VIEW_DESTINATIONS.contains(destinationId) ? View.VISIBLE : View.GONE);
            if (KEEP_SCROLL_BEHAVIOUR_DESTINATIONS.contains(destinationId)) {
                setScrollingBehaviour();
            } else {
                removeScrollingBehaviour();
            }
            if (REMOVE_COLLAPSING_TOOLBAR_SCROLL_DESTINATIONS.contains(destinationId)) {
                removeCollapsingToolbarScrollFlags();
            } else {
                setCollapsingToolbarScrollFlags();
            }
        });
    }

    private void setScrollingBehaviour() {
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.mainNavHost.getLayoutParams();
        layoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        binding.mainNavHost.requestLayout();
    }

    private void removeScrollingBehaviour() {
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) binding.mainNavHost.getLayoutParams();
        layoutParams.setBehavior(null);
        binding.mainNavHost.requestLayout();
    }

    private void setCollapsingToolbarScrollFlags() {
        final CollapsingToolbarLayout collapsingToolbarLayout = binding.collapsingToolbarLayout;
        final AppBarLayout.LayoutParams toolbarLayoutLayoutParams = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();
        toolbarLayoutLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                                         | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                                                         | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        binding.collapsingToolbarLayout.requestLayout();
    }

    private void removeCollapsingToolbarScrollFlags() {
        final CollapsingToolbarLayout collapsingToolbarLayout = binding.collapsingToolbarLayout;
        final AppBarLayout.LayoutParams toolbarLayoutLayoutParams = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();
        toolbarLayoutLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
        binding.collapsingToolbarLayout.requestLayout();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        setupBottomNavigationBar();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (currentNavControllerLiveData != null && currentNavControllerLiveData.getValue() != null) {
            return currentNavControllerLiveData.getValue().navigateUp();
        }
        return false;
    }
}