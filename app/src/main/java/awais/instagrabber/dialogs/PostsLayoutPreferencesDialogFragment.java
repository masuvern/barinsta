package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import awais.instagrabber.R;
import awais.instagrabber.databinding.DialogPostLayoutPreferencesBinding;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.utils.Constants;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class PostsLayoutPreferencesDialogFragment extends DialogFragment {

    private final PostsLayoutPreferences.Builder preferencesBuilder;
    @NonNull
    private final OnApplyListener onApplyListener;
    private DialogPostLayoutPreferencesBinding binding;
    private Context context;

    public PostsLayoutPreferencesDialogFragment(@NonNull final OnApplyListener onApplyListener) {
        final PostsLayoutPreferences preferences = PostsLayoutPreferences.fromJson(settingsHelper.getString(Constants.PREF_POSTS_LAYOUT));
        this.preferencesBuilder = PostsLayoutPreferences.builder().mergeFrom(preferences);
        this.onApplyListener = onApplyListener;
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        binding = DialogPostLayoutPreferencesBinding.inflate(LayoutInflater.from(context), null, false);
        init();
        return new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    final PostsLayoutPreferences preferences = preferencesBuilder.build();
                    final String json = preferences.getJson();
                    settingsHelper.putString(Constants.PREF_POSTS_LAYOUT, json);
                    onApplyListener.onApply(preferences);
                })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        window.setWindowAnimations(R.style.dialog_window_animation);
    }

    private void init() {
        initLayoutToggle();
        if (preferencesBuilder.getType() != PostsLayoutPreferences.PostsLayoutType.LINEAR) {
            initStaggeredOrGridOptions();
        }
    }

    private void initStaggeredOrGridOptions() {
        initColCountToggle();
        initNamesToggle();
        initAvatarsToggle();
        initCornersToggle();
        initGapToggle();
    }

    private void initLayoutToggle() {
        binding.layoutToggle.check(getSelectedLayoutId());
        // binding.staggeredOrGridOptions.setVisibility(getSelectedLayoutId() != R.id.layout_linear ? View.VISIBLE : View.GONE);
        binding.layoutToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                switch (checkedId) {
                    case R.id.layout_linear:
                        preferencesBuilder.setType(PostsLayoutPreferences.PostsLayoutType.LINEAR);
                        binding.staggeredOrGridOptions.setVisibility(View.GONE);
                        break;
                    case R.id.layout_staggered:
                        preferencesBuilder.setType(PostsLayoutPreferences.PostsLayoutType.STAGGERED_GRID);
                        binding.staggeredOrGridOptions.setVisibility(View.VISIBLE);
                        initStaggeredOrGridOptions();
                        break;
                    case R.id.layout_grid:
                    default:
                        preferencesBuilder.setType(PostsLayoutPreferences.PostsLayoutType.GRID);
                        binding.staggeredOrGridOptions.setVisibility(View.VISIBLE);
                        initStaggeredOrGridOptions();
                        break;
                }
            }
        });
    }

    private void initColCountToggle() {
        binding.colCountToggle.check(getSelectedColCountId());
        binding.colCountToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            switch (checkedId) {
                case R.id.col_count_two:
                    preferencesBuilder.setColCount(2);
                    break;
                case R.id.col_count_three:
                default:
                    preferencesBuilder.setColCount(3);
                    break;
            }
        });
    }

    private void initAvatarsToggle() {
        binding.showAvatarToggle.setChecked(preferencesBuilder.isAvatarVisible());
        binding.avatarSizeToggle.check(getSelectedAvatarSizeId());
        binding.showAvatarToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesBuilder.setAvatarVisible(isChecked);
            binding.labelAvatarSize.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.avatarSizeToggle.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        binding.labelAvatarSize.setVisibility(preferencesBuilder.isAvatarVisible() ? View.VISIBLE : View.GONE);
        binding.avatarSizeToggle.setVisibility(preferencesBuilder.isAvatarVisible() ? View.VISIBLE : View.GONE);
        binding.avatarSizeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            switch (checkedId) {
                case R.id.avatar_size_tiny:
                    preferencesBuilder.setProfilePicSize(PostsLayoutPreferences.ProfilePicSize.TINY);
                    break;
                case R.id.avatar_size_small:
                    preferencesBuilder.setProfilePicSize(PostsLayoutPreferences.ProfilePicSize.SMALL);
                    break;
                case R.id.avatar_size_regular:
                default:
                    preferencesBuilder.setProfilePicSize(PostsLayoutPreferences.ProfilePicSize.REGULAR);
                    break;
            }
        });
    }

    private void initNamesToggle() {
        binding.showNamesToggle.setChecked(preferencesBuilder.isNameVisible());
        binding.showNamesToggle.setOnCheckedChangeListener((buttonView, isChecked) -> preferencesBuilder.setNameVisible(isChecked));
    }

    private void initCornersToggle() {
        binding.cornersToggle.check(getSelectedCornersId());
        binding.cornersToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.corners_round) {
                preferencesBuilder.setHasRoundedCorners(true);
                return;
            }
            preferencesBuilder.setHasRoundedCorners(false);
        });
    }

    private void initGapToggle() {
        binding.showGapToggle.setChecked(preferencesBuilder.getHasGap());
        binding.showGapToggle.setOnCheckedChangeListener((buttonView, isChecked) -> preferencesBuilder.setHasGap(isChecked));
    }

    private int getSelectedLayoutId() {
        switch (preferencesBuilder.getType()) {
            case STAGGERED_GRID:
                return R.id.layout_staggered;
            case LINEAR:
                return R.id.layout_linear;
            default:
            case GRID:
                return R.id.layout_grid;
        }
    }

    private int getSelectedColCountId() {
        switch (preferencesBuilder.getColCount()) {
            case 2:
                return R.id.col_count_two;
            case 3:
            default:
                return R.id.col_count_three;
        }
    }

    private int getSelectedCornersId() {
        if (preferencesBuilder.getHasRoundedCorners()) {
            return R.id.corners_round;
        }
        return R.id.corners_square;
    }

    private int getSelectedAvatarSizeId() {
        switch (preferencesBuilder.getProfilePicSize()) {
            case TINY:
                return R.id.avatar_size_tiny;
            case SMALL:
                return R.id.avatar_size_small;
            case REGULAR:
            default:
                return R.id.avatar_size_regular;
        }
    }

    public interface OnApplyListener {
        void onApply(final PostsLayoutPreferences preferences);
    }
}
