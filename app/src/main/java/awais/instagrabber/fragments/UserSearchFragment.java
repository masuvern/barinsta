package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.TransitionManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.UserSearchResultsAdapter;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.databinding.FragmentUserSearchBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.ViewUtils;
import awais.instagrabber.viewmodels.UserSearchViewModel;

public class UserSearchFragment extends Fragment {
    private static final String TAG = UserSearchFragment.class.getSimpleName();

    private FragmentUserSearchBinding binding;
    private UserSearchViewModel viewModel;
    private UserSearchResultsAdapter resultsAdapter;
    private int paddingOffset;

    private final int windowWidth = Utils.displayMetrics.widthPixels;
    private final int minInputWidth = Utils.convertDpToPx(50);
    private String actionLabel;
    private String title;
    private boolean multiple;
    private long[] hideUserIds;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        binding = FragmentUserSearchBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(UserSearchViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        paddingOffset = binding.search.getPaddingStart() + binding.search.getPaddingEnd() + binding.group
                .getPaddingStart() + binding.group.getPaddingEnd() + binding.group.getChipSpacingHorizontal();
        init();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.cleanup();
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments != null) {
            final UserSearchFragmentArgs fragmentArgs = UserSearchFragmentArgs.fromBundle(arguments);
            actionLabel = fragmentArgs.getActionLabel();
            title = fragmentArgs.getTitle();
            multiple = fragmentArgs.getMultiple();
            viewModel.setHideUserIds(fragmentArgs.getHideUserIds());
        }
        setupTitles();
        setupInput();
        setupResults();
        setupObservers();
    }

    private void setupTitles() {
        if (!TextUtils.isEmpty(actionLabel)) {
            binding.done.setText(actionLabel);
        }
        if (!TextUtils.isEmpty(title)) {
            final MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                final ActionBar actionBar = activity.getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
            }
        }
    }

    private void setupResults() {
        final Context context = getContext();
        if (context == null) return;
        binding.results.setLayoutManager(new LinearLayoutManager(context));
        resultsAdapter = new UserSearchResultsAdapter(multiple, (position, user, selected) -> {
            if (!multiple) {
                final NavController navController = NavHostFragment.findNavController(this);
                final NavBackStackEntry navBackStackEntry = navController.getPreviousBackStackEntry();
                if (navBackStackEntry == null) return;
                final SavedStateHandle savedStateHandle = navBackStackEntry.getSavedStateHandle();
                savedStateHandle.set("result", user);
                navController.navigateUp();
                return;
            }
            viewModel.setSelectedUser(user, !selected);
            resultsAdapter.setSelectedUser(user.getPk(), !selected);
            if (!selected) {
                createUserChip(user);
                return;
            }
            final View chip = findChip(user.getPk());
            if (chip == null) return;
            removeChip(chip);
        });
        binding.results.setAdapter(resultsAdapter);
        binding.done.setOnClickListener(v -> {
            final NavController navController = NavHostFragment.findNavController(this);
            final NavBackStackEntry navBackStackEntry = navController.getPreviousBackStackEntry();
            if (navBackStackEntry == null) return;
            final SavedStateHandle savedStateHandle = navBackStackEntry.getSavedStateHandle();
            savedStateHandle.set("result", viewModel.getSelectedUsers());
            navController.navigateUp();
        });
    }

    private void setupInput() {
        binding.search.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s)) {
                    viewModel.cancelSearch();
                    viewModel.clearResults();
                    return;
                }
                viewModel.search(s.toString().trim());
            }
        });
        binding.search.setOnKeyListener((v, keyCode, event) -> {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                final View chip = getLastChip();
                if (chip == null) return false;
                removeSelectedUser(chip);
            }
            return false;
        });
        binding.group.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(final View parent, final View child) {}

            @Override
            public void onChildViewRemoved(final View parent, final View child) {
                binding.group.post(() -> {
                    TransitionManager.beginDelayedTransition(binding.getRoot());
                    calculateInputWidth(0);
                });
            }
        });

    }

    private void setupObservers() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), results -> {
            if (results == null) return;
            switch (results.status) {
                case SUCCESS:
                    resultsAdapter.submitList(results.data);
                    break;
                case ERROR:
                    if (results.message != null) {
                        Snackbar.make(binding.getRoot(), results.message, Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case LOADING:
                    break;
            }
        });
        viewModel.showAction().observe(getViewLifecycleOwner(), showAction -> binding.done.setVisibility(showAction ? View.VISIBLE : View.GONE));
    }

    private void createUserChip(final User user) {
        final Context context = getContext();
        if (context == null) return;
        final Chip chip = new Chip(context);
        chip.setTag(user);
        chip.setText(user.getFullName());
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> removeSelectedUser(chip));
        binding.group.post(() -> {
            final Pair<Integer, Integer> measure = ViewUtils.measure(chip, binding.group);
            TransitionManager.beginDelayedTransition(binding.getRoot());
            calculateInputWidth(measure.second != null ? measure.second : 0);
            binding.group.addView(chip, binding.group.getChildCount() - 1);
        });
    }

    private void removeSelectedUser(final View chip) {
        final User user = (User) chip.getTag();
        if (user == null) return;
        viewModel.setSelectedUser(user, false);
        resultsAdapter.setSelectedUser(user.getPk(), false);
        removeChip(chip);
    }

    private View findChip(final long userId) {
        final int childCount = binding.group.getChildCount();
        if (childCount == 0) {
            return null;
        }
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = binding.group.getChildAt(i);
            if (child == null) continue;
            final User user = (User) child.getTag();
            if (user != null && user.getPk() == userId) {
                return child;
            }
        }
        return null;
    }

    private void removeChip(final View chip) {
        binding.group.post(() -> {
            TransitionManager.beginDelayedTransition(binding.getRoot());
            binding.group.removeView(chip);
        });
    }

    private void calculateInputWidth(final int newChipWidth) {
        final View lastChip = getLastChip();
        int lastRight = lastChip != null ? lastChip.getRight() : 0;
        final int remainingSpaceInRow = windowWidth - lastRight;
        if (remainingSpaceInRow < newChipWidth) {
            // next chip will go to the next row, so assume no chips present
            lastRight = 0;
        }
        final int newRight = lastRight + newChipWidth;
        final int newInputWidth = windowWidth - newRight - paddingOffset;
        binding.search.getLayoutParams().width = newInputWidth < minInputWidth ? windowWidth : newInputWidth;
        binding.search.requestLayout();
    }

    private View getLastChip() {
        final int childCount = binding.group.getChildCount();
        if (childCount == 0) {
            return null;
        }
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = binding.group.getChildAt(i);
            if (child instanceof Chip) {
                return child;
            }
        }
        return null;
    }
}
