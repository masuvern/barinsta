package awais.instagrabber.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.PermissionChecker;
import androidx.core.util.Pair;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.SliderCallbackAdapter;
import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.adapters.viewholder.SliderVideoViewHolder;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.databinding.DialogPostViewBinding;
import awais.instagrabber.dialogs.EditTextDialogFragment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.VideoVersion;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.PostViewV2ViewModel;

import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static awais.instagrabber.fragments.HashTagFragment.ARG_HASHTAG;
import static awais.instagrabber.utils.DownloadUtils.WRITE_PERMISSION;
import static awais.instagrabber.utils.Utils.getAttrValue;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class PostViewV2Fragment extends Fragment implements EditTextDialogFragment.EditTextDialogFragmentCallback {
    private static final String TAG = "PostViewV2Fragment";
    private static final int DETAILS_HIDE_DELAY_MILLIS = 2000;
    public static final String ARG_MEDIA = "media";
    public static final String ARG_SLIDER_POSITION = "position";
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;

    // private Media media;
    private DialogPostViewBinding binding;
    // private MediaService mediaService;
    // private Context context;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private boolean detailsVisible = true;
    private boolean video;
    private VideoPlayerViewHelper videoPlayerViewHelper;
    private SliderItemsAdapter sliderItemsAdapter;
    // private boolean wasControlsVisible;
    private int captionState = BottomSheetBehavior.STATE_HIDDEN;
    private int sliderPosition = -1;
    private boolean hasBeenToggled = false;
    private PostViewV2ViewModel viewModel;
    private PopupMenu optionsPopup;
    private EditTextDialogFragment editTextDialogFragment;
    private boolean wasDeleted;
    private MutableLiveData<Object> backStackSavedStateResultLiveData;
    private OnDeleteListener onDeleteListener;

    private final Observer<Object> backStackSavedStateObserver = result -> {
        if (result == null) return;
        if (result instanceof String) {
            final String collection = (String) result;
            handleSaveUnsaveResourceLiveData(viewModel.toggleSave(collection, viewModel.getMedia().hasViewerSaved()));
        }
        // clear result
        backStackSavedStateResultLiveData.postValue(null);
    };
    private final GestureDetector.OnGestureListener videoPlayerViewGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            binding.videoPost.playerView.performClick();
            return true;
        }
    };

    public void setOnDeleteListener(final OnDeleteListener onDeleteListener) {
        if (onDeleteListener == null) return;
        this.onDeleteListener = onDeleteListener;
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    // default constructor for fragment manager
    public PostViewV2Fragment() {}

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PostViewV2ViewModel.class);
        captionState = settingsHelper.getBoolean(Constants.SHOW_CAPTIONS) ?
                       BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_HIDDEN;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = DialogPostViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        // postponeEnterTransition();
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        // wasPaused = true;
        if (bottomSheetBehavior != null) {
            captionState = bottomSheetBehavior.getState();
        }
        if (settingsHelper.getBoolean(Constants.PLAY_IN_BACKGROUND)) return;
        final Media media = viewModel.getMedia();
        if (media == null) return;
        switch (media.getMediaType()) {
            case MEDIA_TYPE_VIDEO:
                if (videoPlayerViewHelper != null) {
                    videoPlayerViewHelper.pause();
                }
                return;
            case MEDIA_TYPE_SLIDER:
                if (sliderItemsAdapter != null) {
                    pauseSliderPlayer();
                }
            default:
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final NavController navController = NavHostFragment.findNavController(this);
        final NavBackStackEntry backStackEntry = navController.getCurrentBackStackEntry();
        if (backStackEntry != null) {
            backStackSavedStateResultLiveData = backStackEntry.getSavedStateHandle().getLiveData("collection");
            backStackSavedStateResultLiveData.observe(getViewLifecycleOwner(), backStackSavedStateObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final Media media = viewModel.getMedia();
        if (media == null) return;
        switch (media.getMediaType()) {
            case MEDIA_TYPE_VIDEO:
                if (videoPlayerViewHelper != null) {
                    videoPlayerViewHelper.releasePlayer();
                }
                return;
            case MEDIA_TYPE_SLIDER:
                if (sliderItemsAdapter != null) {
                    releaseAllSliderPlayers();
                }
            default:
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        final Media media = viewModel.getMedia();
        if (media == null) return;
        if (media.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
            outState.putInt(ARG_SLIDER_POSITION, sliderPosition);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERM_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            final Context context = getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, viewModel.getMedia(), sliderPosition);
        }
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            // dismiss();
            return;
        }
        final Serializable feedModelSerializable = arguments.getSerializable(ARG_MEDIA);
        if (feedModelSerializable == null) {
            Log.e(TAG, "onCreate: feedModelSerializable is null");
            // dismiss();
            return;
        }
        if (!(feedModelSerializable instanceof Media)) {
            // dismiss();
            return;
        }
        final Media media = (Media) feedModelSerializable;
        if (media.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
            sliderPosition = arguments.getInt(ARG_SLIDER_POSITION, 0);
        }
        viewModel.setMedia(media);
        // if (!wasPaused && (sharedProfilePicElement != null || sharedMainPostElement != null)) {
        //     binding.getRoot().getBackground().mutate().setAlpha(0);
        // }
        // setProfilePicSharedElement();
        // setupCaptionBottomSheet();
        setupCommonActions();
        setObservers();
    }

    private void setObservers() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                binding.profilePic.setVisibility(View.GONE);
                binding.title.setVisibility(View.GONE);
                binding.subtitle.setVisibility(View.GONE);
                return;
            }
            binding.profilePic.setVisibility(View.VISIBLE);
            binding.title.setVisibility(View.VISIBLE);
            binding.subtitle.setVisibility(View.VISIBLE);
            binding.getRoot().post(() -> setupProfilePic(user));
            binding.getRoot().post(() -> setupTitles(user));
        });
        viewModel.getCaption().observe(getViewLifecycleOwner(), caption -> binding.getRoot().post(() -> setupCaption(caption)));
        viewModel.getLocation().observe(getViewLifecycleOwner(), location -> binding.getRoot().post(() -> setupLocation(location)));
        viewModel.getDate().observe(getViewLifecycleOwner(), date -> binding.getRoot().post(() -> {
            if (date == null) {
                binding.date.setVisibility(View.GONE);
                return;
            }
            binding.date.setVisibility(View.VISIBLE);
            binding.date.setText(date);
        }));
        viewModel.getLikeCount().observe(getViewLifecycleOwner(), count -> {
            final long safeCount = getSafeCount(count);
            final String likesString = getResources().getQuantityString(R.plurals.likes_count, (int) safeCount, safeCount);
            binding.likesCount.setText(likesString);
        });
        if (!viewModel.getMedia().isCommentsDisabled()) {
            viewModel.getCommentCount().observe(getViewLifecycleOwner(), count -> {
                final long safeCount = getSafeCount(count);
                final String likesString = getResources().getQuantityString(R.plurals.comments_count, (int) safeCount, safeCount);
                binding.commentsCount.setText(likesString);
            });
        }
        viewModel.getViewCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) {
                binding.viewsCount.setVisibility(View.GONE);
                return;
            }
            binding.viewsCount.setVisibility(View.VISIBLE);
            final long safeCount = getSafeCount(count);
            final String viewString = getResources().getQuantityString(R.plurals.views_count, (int) safeCount, safeCount);
            binding.viewsCount.setText(viewString);
        });
        viewModel.getType().observe(getViewLifecycleOwner(), this::setupPostTypeLayout);
        viewModel.getLiked().observe(getViewLifecycleOwner(), this::setLikedResources);
        viewModel.getSaved().observe(getViewLifecycleOwner(), this::setSavedResources);
        viewModel.getOptions().observe(getViewLifecycleOwner(), options -> binding.getRoot().post(() -> {
            setupOptions(options != null && !options.isEmpty());
            createOptionsPopupMenu();
        }));
    }

    @NonNull
    private Long getSafeCount(final Long count) {
        Long safeCount = count;
        if (count == null) {
            safeCount = 0L;
        }
        return safeCount;
    }

    private void setupCommonActions() {
        setupLike();
        setupSave();
        setupDownload();
        setupComment();
        setupShare();
    }

    private void setupComment() {
        if (!viewModel.hasPk() || viewModel.getMedia().isCommentsDisabled()) {
            binding.comment.setVisibility(View.GONE);
            binding.commentsCount.setVisibility(View.GONE);
            return;
        }
        binding.comment.setVisibility(View.VISIBLE);
        binding.comment.setOnClickListener(v -> {
            final Media media = viewModel.getMedia();
            final User user = media.getUser();
            if (user == null) return;
            final NavController navController = getNavController();
            if (navController == null) return;
            final Bundle bundle = new Bundle();
            bundle.putString("shortCode", media.getCode());
            bundle.putString("postId", media.getPk());
            bundle.putLong("postUserId", user.getPk());
            try {
                navController.navigate(R.id.action_global_commentsViewerFragment, bundle);
            } catch (Exception e) {
                Log.e(TAG, "setupComment: ", e);
            }
        });
        binding.comment.setOnLongClickListener(v -> {
            final Context context = getContext();
            if (context == null) return false;
            Utils.displayToastAboveView(context, v, getString(R.string.comment));
            return true;
        });
    }

    private void setupDownload() {
        binding.download.setOnClickListener(v -> {
            final Context context = getContext();
            if (context == null) return;
            if (checkSelfPermission(context, WRITE_PERMISSION) == PermissionChecker.PERMISSION_GRANTED) {
                DownloadUtils.showDownloadDialog(context, viewModel.getMedia(), sliderPosition);
                return;
            }
            requestPermissions(DownloadUtils.PERMS, STORAGE_PERM_REQUEST_CODE);
        });
        binding.download.setOnLongClickListener(v -> {
            final Context context = getContext();
            if (context == null) return false;
            Utils.displayToastAboveView(context, v, getString(R.string.action_download));
            return true;
        });
    }

    private void setupLike() {
        final boolean likableMedia = viewModel.hasPk() /*&& viewModel.getMedia().isCommentLikesEnabled()*/;
        if (!likableMedia) {
            binding.like.setVisibility(View.GONE);
            binding.likesCount.setVisibility(View.GONE);
            return;
        }
        if (!viewModel.isLoggedIn()) {
            binding.like.setVisibility(View.GONE);
            return;
        }
        binding.like.setOnClickListener(v -> {
            v.setEnabled(false);
            handleLikeUnlikeResourceLiveData(viewModel.toggleLike());
        });
        binding.like.setOnLongClickListener(v -> {
            final NavController navController = getNavController();
            if (navController != null && viewModel.isLoggedIn()) {
                final Bundle bundle = new Bundle();
                bundle.putString("postId", viewModel.getMedia().getPk());
                bundle.putBoolean("isComment", false);
                navController.navigate(R.id.action_global_likesViewerFragment, bundle);
                return true;
            }
            return true;
        });
    }

    private void handleLikeUnlikeResourceLiveData(@NonNull final LiveData<Resource<Object>> resource) {
        resource.observe(getViewLifecycleOwner(), value -> {
            switch (value.status) {
                case SUCCESS:
                    binding.like.setEnabled(true);
                    break;
                case ERROR:
                    binding.like.setEnabled(true);
                    unsuccessfulLike();
                    break;
                case LOADING:
                    binding.like.setEnabled(false);
                    break;
            }
        });

    }

    private void unsuccessfulLike() {
        final int errorTextResId;
        final Media media = viewModel.getMedia();
        if (!media.hasLiked()) {
            Log.e(TAG, "like unsuccessful!");
            errorTextResId = R.string.like_unsuccessful;
        } else {
            Log.e(TAG, "unlike unsuccessful!");
            errorTextResId = R.string.unlike_unsuccessful;
        }
        final Snackbar snackbar = Snackbar.make(binding.getRoot(), errorTextResId, BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, null);
        snackbar.show();
    }

    private void setLikedResources(final boolean liked) {
        final int iconResource;
        final int tintResource;
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        if (resources == null) return;
        if (liked) {
            iconResource = R.drawable.ic_like;
            tintResource = resources.getColor(R.color.red_600);
            // textResId = R.string.unlike_without_count;
        } else {
            iconResource = R.drawable.ic_not_liked;
            tintResource = getAttrValue(context, R.attr.colorPrimary);
            // textResId = R.string.like_without_count;
        }
        binding.like.setIconResource(iconResource);
        binding.like.setIconTint(ColorStateList.valueOf(tintResource));
    }

    private void setupSave() {
        if (!viewModel.isLoggedIn() || !viewModel.hasPk() || !viewModel.getMedia().canViewerSave()) {
            binding.save.setVisibility(View.GONE);
            return;
        }
        binding.save.setOnClickListener(v -> {
            binding.save.setEnabled(false);
            handleSaveUnsaveResourceLiveData(viewModel.toggleSave());
        });
        binding.save.setOnLongClickListener(v -> {
            final NavController navController = NavHostFragment.findNavController(this);
            final Bundle bundle = new Bundle();
            bundle.putBoolean("isSaving", true);
            navController.navigate(R.id.action_global_savedCollectionsFragment, bundle);
            return true;
        });
    }

    private void handleSaveUnsaveResourceLiveData(@NonNull final LiveData<Resource<Object>> resource) {
        resource.observe(getViewLifecycleOwner(), value -> {
            if (value == null) return;
            switch (value.status) {
                case SUCCESS:
                    binding.save.setEnabled(true);
                    break;
                case ERROR:
                    binding.save.setEnabled(true);
                    unsuccessfulSave();
                    break;
                case LOADING:
                    binding.save.setEnabled(false);
                    break;
            }
        });
    }

    private void unsuccessfulSave() {
        final int errorTextResId;
        final Media media = viewModel.getMedia();
        if (!media.hasViewerSaved()) {
            Log.e(TAG, "save unsuccessful!");
            errorTextResId = R.string.save_unsuccessful;
        } else {
            Log.e(TAG, "save remove unsuccessful!");
            errorTextResId = R.string.save_remove_unsuccessful;
        }
        final Snackbar snackbar = Snackbar.make(binding.getRoot(), errorTextResId, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, null);
        snackbar.show();
    }

    private void setSavedResources(final boolean saved) {
        final int iconResource;
        final int tintResource;
        final Context context = getContext();
        if (context == null) return;
        final Resources resources = context.getResources();
        if (resources == null) return;
        if (saved) {
            iconResource = R.drawable.ic_class_24;
            tintResource = resources.getColor(R.color.blue_700);
            // textResId = R.string.saved;
        } else {
            iconResource = R.drawable.ic_outline_class_24;
            tintResource = getAttrValue(context, R.attr.colorPrimary);
            // textResId = R.string.save;
        }
        binding.save.setIconResource(iconResource);
        binding.save.setIconTint(ColorStateList.valueOf(tintResource));
    }

    private void setupProfilePic(final User user) {
        if (user == null) {
            binding.profilePic.setImageURI((String) null);
            return;
        }
        final String uri = user.getProfilePicUrl();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setUri(uri)
                .build();
        binding.profilePic.setController(controller);
        binding.profilePic.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
    }

    private void setupTitles(final User user) {
        if (user == null) {
            binding.title.setVisibility(View.GONE);
            binding.subtitle.setVisibility(View.GONE);
            return;
        }
        final String fullName = user.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            binding.subtitle.setVisibility(View.GONE);
        } else {
            binding.subtitle.setVisibility(View.VISIBLE);
            binding.subtitle.setText(fullName);
        }
        setUsername(user);
        binding.title.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
        binding.subtitle.setOnClickListener(v -> navigateToProfile("@" + user.getUsername()));
    }

    private void setUsername(final User user) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
        final int drawableSize = Utils.convertDpToPx(24);
        if (user.isVerified()) {
            final Context context = getContext();
            if (context == null) return;
            final Drawable verifiedDrawable = AppCompatResources.getDrawable(context, R.drawable.verified);
            VerticalImageSpan verifiedSpan = null;
            if (verifiedDrawable != null) {
                final Drawable drawable = verifiedDrawable.mutate();
                drawable.setBounds(0, 0, drawableSize, drawableSize);
                verifiedSpan = new VerticalImageSpan(drawable);
            }
            try {
                if (verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (Exception e) {
                Log.e(TAG, "setUsername: ", e);
            }
        }
        binding.title.setText(sb);
    }

    private void setupCaption(final Caption caption) {
        if (caption == null || TextUtils.isEmpty(caption.getText())) {
            binding.caption.setVisibility(View.GONE);
            binding.translate.setVisibility(View.GONE);
            return;
        }
        final String postCaption = caption.getText();
        binding.caption.addOnHashtagListener(autoLinkItem -> {
            final NavController navController = NavHostFragment.findNavController(this);
            final Bundle bundle = new Bundle();
            final String originalText = autoLinkItem.getOriginalText().trim();
            bundle.putString(ARG_HASHTAG, originalText);
            navController.navigate(R.id.action_global_hashTagFragment, bundle);
        });
        binding.caption.addOnMentionClickListener(autoLinkItem -> {
            final String originalText = autoLinkItem.getOriginalText().trim();
            navigateToProfile(originalText);
        });
        binding.caption.addOnEmailClickListener(autoLinkItem -> Utils.openEmailAddress(getContext(), autoLinkItem.getOriginalText().trim()));
        binding.caption.addOnURLClickListener(autoLinkItem -> Utils.openURL(getContext(), autoLinkItem.getOriginalText().trim()));
        binding.caption.setOnLongClickListener(v -> {
            final Context context = getContext();
            if (context == null) return false;
            Utils.copyText(context, postCaption);
            return true;
        });
        binding.caption.setText(postCaption);
        binding.translate.setOnClickListener(v -> handleTranslateCaptionResource(viewModel.translateCaption()));
    }

    private void handleTranslateCaptionResource(@NonNull final LiveData<Resource<String>> data) {
        data.observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case SUCCESS:
                    binding.translate.setVisibility(View.GONE);
                    binding.caption.setText(resource.data);
                    break;
                case ERROR:
                    binding.translate.setEnabled(true);
                    String message = resource.message;
                    if (TextUtils.isEmpty(resource.message)) {
                        message = getString(R.string.downloader_unknown_error);
                    }
                    final Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    binding.translate.setEnabled(false);
                    break;
            }
        });
    }

    private void setupLocation(final Location location) {
        if (location == null || !detailsVisible) {
            binding.location.setVisibility(View.GONE);
            return;
        }
        final String locationName = location.getName();
        if (TextUtils.isEmpty(locationName)) return;
        binding.location.setText(locationName);
        binding.location.setVisibility(View.VISIBLE);
        binding.location.setOnClickListener(v -> {
            final NavController navController = getNavController();
            if (navController == null) return;
            final Bundle bundle = new Bundle();
            bundle.putLong("locationId", location.getPk());
            navController.navigate(R.id.action_global_locationFragment, bundle);
        });
    }

    private void setupShare() {
        if (!viewModel.hasPk()) {
            binding.share.setVisibility(View.GONE);
            return;
        }
        binding.share.setVisibility(View.VISIBLE);
        binding.share.setOnLongClickListener(v -> {
            final Context context = getContext();
            if (context == null) return false;
            Utils.displayToastAboveView(context, v, getString(R.string.share));
            return true;
        });
        binding.share.setOnClickListener(v -> {
            final Media media = viewModel.getMedia();
            final User profileModel = media.getUser();
            if (profileModel == null) return;
            final boolean isPrivate = profileModel.isPrivate();
            if (isPrivate) {
                final Context context = getContext();
                if (context == null) return;
                // is this necessary?
                Toast.makeText(context, R.string.share_private_post, Toast.LENGTH_LONG).show();
            }
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/" + media.getCode());
            startActivity(Intent.createChooser(sharingIntent,
                                               isPrivate ? getString(R.string.share_private_post) : getString(R.string.share_public_post)));
        });
    }

    private void setupPostTypeLayout(final MediaItemType type) {
        if (type == null) return;
        switch (type) {
            case MEDIA_TYPE_IMAGE:
                setupPostImage();
                break;
            case MEDIA_TYPE_SLIDER:
                setupSlider();
                break;
            case MEDIA_TYPE_VIDEO:
                setupVideo();
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPostImage() {
        binding.videoPost.root.setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.GONE);
        // binding.playerControlsToggle.setVisibility(View.GONE);
        // binding.playerControls.getRoot().setVisibility(View.GONE);
        binding.mediaCounter.setVisibility(View.GONE);
        binding.postImage.setVisibility(View.VISIBLE);
        final Media media = viewModel.getMedia();
        final String imageUrl = ResponseBodyUtils.getImageUrl(media);
        if (TextUtils.isEmpty(imageUrl)) return;
        final ViewGroup.LayoutParams layoutParams = binding.postImage.getLayoutParams();
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                                                                                    media.getOriginalWidth(),
                                                                                    (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                    Utils.displayMetrics.widthPixels);
        layoutParams.height = widthHeight.second;
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .build();
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setLowResImageRequest(ImageRequest.fromUri(ResponseBodyUtils.getThumbUrl(media)))
                .setImageRequest(requestBuilder)
                .build();
        binding.postImage.setController(controller);
        // binding.postImage.setOnClickListener(v -> toggleDetails());
        final AnimatedZoomableController zoomableController = (AnimatedZoomableController) binding.postImage.getZoomableController();
        zoomableController.setMaxScaleFactor(3f);
        zoomableController.setGestureZoomEnabled(true);
        zoomableController.setEnabled(true);
        binding.postImage.setZoomingEnabled(true);
        binding.postImage.setTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(final MotionEvent e) {
                // toggleDetails();
                return true;
            }
        });
        // binding.postImage.setAllowTouchInterceptionWhileZoomed(true);
        // binding.postImage.setOnVerticalDragListener(onVerticalDragListener);
    }

    private void setupSlider() {
        final Media media = viewModel.getMedia();
        binding.postImage.setVisibility(View.GONE);
        binding.videoPost.root.setVisibility(View.GONE);
        // binding.playerControlsToggle.setVisibility(View.GONE);
        // binding.playerControls.getRoot().setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.VISIBLE);
        binding.mediaCounter.setVisibility(View.VISIBLE);
        final Pair<Integer, Integer> maxHW = media
                .getCarouselMedia()
                .stream()
                .reduce(new Pair<>(0, 0),
                        (prev, m) -> {
                            final int height = m.getOriginalHeight() > prev.first ? m.getOriginalHeight() : prev.first;
                            final int width = m.getOriginalWidth() > prev.second ? m.getOriginalWidth() : prev.second;
                            return new Pair<>(height, width);
                        },
                        (p1, p2) -> {
                            final int height = p1.first > p2.first ? p1.first : p2.first;
                            final int width = p1.second > p2.second ? p1.second : p2.second;
                            return new Pair<>(height, width);
                        });
        final ViewGroup.LayoutParams layoutParams = binding.sliderParent.getLayoutParams();
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(maxHW.first,
                                                                                    maxHW.second,
                                                                                    (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                    Utils.displayMetrics.widthPixels);
        layoutParams.height = widthHeight.second;
        final boolean hasVideo = media.getCarouselMedia()
                                      .stream()
                                      .anyMatch(postChild -> postChild.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO);
        if (hasVideo) {
            final View child = binding.sliderParent.getChildAt(0);
            if (child instanceof RecyclerView) {
                ((RecyclerView) child).setItemViewCacheSize(media.getCarouselMedia().size());
                ((RecyclerView) child).addRecyclerListener(holder -> {
                    if (holder instanceof SliderVideoViewHolder) {
                        ((SliderVideoViewHolder) holder).releasePlayer();
                    }
                });
            }
        }
        sliderItemsAdapter = new SliderItemsAdapter(null, true, new SliderCallbackAdapter() {
            @Override
            public void onThumbnailLoaded(final int position) {
                if (position != 0) return;
                startPostponedEnterTransition();
            }

            @Override
            public void onItemClicked(final int position) {
                // toggleDetails();
            }

            @Override
            public void onPlayerPlay(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                // if (!detailsVisible || hasBeenToggled) return;
                // showPlayerControls();
            }

            @Override
            public void onPlayerPause(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
                // if (detailsVisible || hasBeenToggled) return;
                // toggleDetails();
            }

            @Override
            public void onPlayerRelease(final int position) {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }
        });
        binding.sliderParent.setAdapter(sliderItemsAdapter);
        if (sliderPosition >= 0 && sliderPosition < media.getCarouselMedia().size()) {
            binding.sliderParent.setCurrentItem(sliderPosition);
        }
        binding.sliderParent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            int prevPosition = -1;

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                if (prevPosition != -1) {
                    final View view = binding.sliderParent.getChildAt(0);
                    if (view instanceof RecyclerView) {
                        pausePlayerAtPosition(prevPosition, (RecyclerView) view);
                        pausePlayerAtPosition(position, (RecyclerView) view);
                    }
                }
                if (positionOffset == 0) {
                    prevPosition = position;
                }
            }

            @Override
            public void onPageSelected(final int position) {
                final int size = media.getCarouselMedia().size();
                if (position < 0 || position >= size) return;
                sliderPosition = position;
                final String text = (position + 1) + "/" + size;
                binding.mediaCounter.setText(text);
                final Media childMedia = media.getCarouselMedia().get(position);
                // final View view = binding.sliderParent.getChildAt(0);
                // if (prevPosition != -1) {
                // if (view instanceof RecyclerView) {
                // final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(prevPosition);
                // if (viewHolder instanceof SliderVideoViewHolder) {
                //     ((SliderVideoViewHolder) viewHolder).removeCallbacks();
                // }
                // }
                // }
                video = false;
                if (childMedia.getMediaType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                    // if (view instanceof RecyclerView) {
                    // final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
                    // if (viewHolder instanceof SliderVideoViewHolder) {
                    //     ((SliderVideoViewHolder) viewHolder).resetPlayerTimeline();
                    // }
                    // }
                    // enablePlayerControls(true);
                    video = true;
                    viewModel.setViewCount(childMedia.getViewCount());
                    return;
                }
                viewModel.setViewCount(null);
                // enablePlayerControls(false);
            }

            private void pausePlayerAtPosition(final int position, final RecyclerView view) {
                final RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof SliderVideoViewHolder) {
                    ((SliderVideoViewHolder) viewHolder).pause();
                }
            }
        });
        final String text = "1/" + media.getCarouselMedia().size();
        binding.mediaCounter.setText(text);
        sliderItemsAdapter.submitList(media.getCarouselMedia());
    }

    private void pauseSliderPlayer() {
        if (binding.sliderParent.getVisibility() != View.VISIBLE) return;
        final int currentItem = binding.sliderParent.getCurrentItem();
        final View view = binding.sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(currentItem);
        if (!(viewHolder instanceof SliderVideoViewHolder)) return;
        ((SliderVideoViewHolder) viewHolder).pause();
    }

    private void releaseAllSliderPlayers() {
        if (binding.sliderParent.getVisibility() != View.VISIBLE) return;
        final View view = binding.sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        final int itemCount = sliderItemsAdapter.getItemCount();
        for (int position = itemCount - 1; position >= 0; position--) {
            final RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
            if (!(viewHolder instanceof SliderVideoViewHolder)) continue;
            ((SliderVideoViewHolder) viewHolder).releasePlayer();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupVideo() {
        video = true;
        final Media media = viewModel.getMedia();
        binding.postImage.setVisibility(View.GONE);
        binding.sliderParent.setVisibility(View.GONE);
        binding.mediaCounter.setVisibility(View.GONE);
        // binding.playerControls.getRoot().setVisibility(View.VISIBLE);
        final ViewGroup.LayoutParams layoutParams = binding.videoPost.root.getLayoutParams();
        final Pair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                                                                                    media.getOriginalWidth(),
                                                                                    (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                    Utils.displayMetrics.widthPixels);
        layoutParams.height = widthHeight.second;
        binding.videoPost.root.setVisibility(View.VISIBLE);
        // enablePlayerControls(true);
        // binding.videoPost.playerView.setOnClickListener(v -> toggleDetails());
        final Context context = getContext();
        if (context == null) return;
        final GestureDetector gestureDetector = new GestureDetector(context, videoPlayerViewGestureListener);
        binding.videoPost.playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        final float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        final VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {
            @Override
            public void onThumbnailLoaded() {
                startPostponedEnterTransition();
            }

            @Override
            public void onPlayerViewLoaded() {
                // binding.playerControls.getRoot().setVisibility(View.VISIBLE);
                final ViewGroup.LayoutParams layoutParams = binding.videoPost.playerView.getLayoutParams();
                final int requiredWidth = Utils.displayMetrics.widthPixels;
                final int resultingHeight = NumberUtils
                        .getResultingHeight(requiredWidth, media.getOriginalHeight(), media.getOriginalWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                binding.videoPost.playerView.requestLayout();
            }

            @Override
            public void onPlay() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                // if (detailsVisible) {
                //     new Handler().postDelayed(() -> toggleDetails(), DETAILS_HIDE_DELAY_MILLIS);
                // }
            }

            @Override
            public void onPause() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }

            @Override
            public void onRelease() {
                final FragmentActivity activity = getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }
        };
        final float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
        String videoUrl = null;
        final List<VideoVersion> videoVersions = media.getVideoVersions();
        if (videoVersions != null && !videoVersions.isEmpty()) {
            final VideoVersion videoVersion = videoVersions.get(0);
            if (videoVersion != null) {
                videoUrl = videoVersion.getUrl();
            }
        }
        if (videoUrl != null) {
            videoPlayerViewHelper = new VideoPlayerViewHelper(
                    binding.getRoot().getContext(),
                    binding.videoPost,
                    videoUrl,
                    vol,
                    aspectRatio,
                    ResponseBodyUtils.getThumbUrl(media),
                    true,
                    // /*binding.playerControls*/null,
                    videoPlayerCallback);
        }
    }

    // private void enablePlayerControls(final boolean enable) {
    //     video = enable;
    //     if (enable) {
    //         binding.playerControlsToggle.setVisibility(View.VISIBLE);
    //         binding.playerControlsToggle.setOnClickListener(v -> {
    //             final int visibility = binding.playerControls.getRoot().getVisibility();
    //             if (visibility == View.GONE) {
    //                 showPlayerControls();
    //                 return;
    //             }
    //             hidePlayerControls();
    //         });
    //         return;
    //     }
    //     binding.playerControlsToggle.setVisibility(View.GONE);
    //     hidePlayerControls();
    // }

    // private void hideCaption() {
    //     if (bottomSheetBehavior == null) return;
    //     bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    // }

    // private void showPlayerControls() {
    //     hideCaption();
    //     // previously invisible view
    //     View view = binding.playerControls.getRoot();
    //     if (view.getVisibility() == View.VISIBLE) {
    //         return;
    //     }
    //     if (!ViewCompat.isAttachedToWindow(view)) {
    //         view.setVisibility(View.VISIBLE);
    //         return;
    //     }
    //     // get the center for the clipping circle
    //     int cx = view.getWidth() / 2;
    //     // int cy = view.getHeight() / 2;
    //     int cy = view.getHeight();
    //
    //     // get the final radius for the clipping circle
    //     float finalRadius = (float) Math.hypot(cx, cy);
    //
    //     // create the animator for this view (the start radius is zero)
    //     Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius);
    //
    //     // make the view visible and start the animation
    //     view.setVisibility(View.VISIBLE);
    //     anim.start();
    //
    // }

    // private void hidePlayerControls() {
    //     // previously visible view
    //     final View view = binding.playerControls.getRoot();
    //     if (view.getVisibility() == View.GONE) {
    //         return;
    //     }
    //     if (!ViewCompat.isAttachedToWindow(view)) {
    //         view.setVisibility(View.GONE);
    //         return;
    //     }
    //
    //     // get the center for the clipping circle
    //     int cx = view.getWidth() / 2;
    //     // int cy = view.getHeight() / 2;
    //     int cy = view.getHeight();
    //
    //     // get the initial radius for the clipping circle
    //     float initialRadius = (float) Math.hypot(cx, cy);
    //
    //     // create the animation (the final radius is zero)
    //     Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f);
    //
    //     // make the view invisible when the animation is done
    //     anim.addListener(new AnimatorListenerAdapter() {
    //         @Override
    //         public void onAnimationEnd(Animator animation) {
    //             super.onAnimationEnd(animation);
    //             view.setVisibility(View.GONE);
    //         }
    //     });
    //
    //     // start the animation
    //     anim.start();
    // }

    private void setupOptions(final Boolean show) {
        if (!show) {
            binding.options.setVisibility(View.GONE);
            return;
        }
        binding.options.setVisibility(View.VISIBLE);
        binding.options.setOnClickListener(v -> {
            if (optionsPopup == null) return;
            optionsPopup.show();
        });
    }

    private void createOptionsPopupMenu() {
        if (optionsPopup == null) {
            final Context context = getContext();
            if (context == null) return;
            final ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
            optionsPopup = new PopupMenu(themeWrapper, binding.options);
        } else {
            optionsPopup.getMenu().clear();
        }
        optionsPopup.getMenuInflater().inflate(R.menu.post_view_menu, optionsPopup.getMenu());
        // final Menu menu = optionsPopup.getMenu();
        // final int size = menu.size();
        // for (int i = 0; i < size; i++) {
        //     final MenuItem item = menu.getItem(i);
        //     if (item == null) continue;
        //     if (options.contains(item.getItemId())) continue;
        //     menu.removeItem(item.getItemId());
        // }
        optionsPopup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.edit_caption) {
                showCaptionEditDialog();
                return true;
            }
            if (itemId == R.id.delete) {
                item.setEnabled(false);
                final LiveData<Resource<Object>> resourceLiveData = viewModel.delete();
                handleDeleteResource(resourceLiveData, item);
            }
            return true;
        });
    }

    private void handleDeleteResource(final LiveData<Resource<Object>> resourceLiveData, final MenuItem item) {
        if (resourceLiveData == null) return;
        resourceLiveData.observe(getViewLifecycleOwner(), new Observer<Resource<Object>>() {
            @Override
            public void onChanged(final Resource<Object> resource) {
                try {
                    switch (resource.status) {
                        case SUCCESS:
                            wasDeleted = true;
                            if (onDeleteListener != null) {
                                onDeleteListener.onDelete();
                            }
                            break;
                        case ERROR:
                            if (item != null) {
                                item.setEnabled(true);
                            }
                            final Snackbar snackbar = Snackbar.make(binding.getRoot(),
                                                                    R.string.delete_unsuccessful,
                                                                    Snackbar.LENGTH_INDEFINITE);
                            snackbar.setAction(R.string.ok, null);
                            snackbar.show();
                            break;
                        case LOADING:
                            if (item != null) {
                                item.setEnabled(false);
                            }
                            break;
                    }
                } finally {
                    resourceLiveData.removeObserver(this);
                }
            }
        });
    }

    private void showCaptionEditDialog() {
        final Caption caption = viewModel.getCaption().getValue();
        final String captionText = caption != null ? caption.getText() : null;
        editTextDialogFragment = EditTextDialogFragment
                .newInstance(R.string.edit_caption, R.string.confirm, R.string.cancel, captionText);
        editTextDialogFragment.show(getChildFragmentManager(), "edit_caption");
    }

    @Override
    public void onPositiveButtonClicked(final String caption) {
        handleEditCaptionResource(viewModel.updateCaption(caption));
        if (editTextDialogFragment == null) return;
        editTextDialogFragment.dismiss();
        editTextDialogFragment = null;
    }

    private void handleEditCaptionResource(final LiveData<Resource<Object>> updateCaption) {
        if (updateCaption == null) return;
        updateCaption.observe(getViewLifecycleOwner(), resource -> {
            final MenuItem item = optionsPopup.getMenu().findItem(R.id.edit_caption);
            switch (resource.status) {
                case SUCCESS:
                    if (item != null) {
                        item.setEnabled(true);
                    }
                    break;
                case ERROR:
                    if (item != null) {
                        item.setEnabled(true);
                    }
                    final Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.edit_unsuccessful, BaseTransientBottomBar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    if (item != null) {
                        item.setEnabled(false);
                    }
                    break;
            }
        });
    }

    @Override
    public void onNegativeButtonClicked() {
        if (editTextDialogFragment == null) return;
        editTextDialogFragment.dismiss();
        editTextDialogFragment = null;
    }

    private void toggleDetails() {
        hasBeenToggled = true;
        final Media media = viewModel.getMedia();
        binding.getRoot().post(() -> {
            TransitionManager.beginDelayedTransition(binding.getRoot());
            if (detailsVisible) {
                detailsVisible = false;
                if (media.getUser() != null) {
                    binding.profilePic.setVisibility(View.GONE);
                    binding.title.setVisibility(View.GONE);
                    binding.subtitle.setVisibility(View.GONE);
                    // binding.topBg.setVisibility(View.GONE);
                }
                if (media.getLocation() != null) {
                    binding.location.setVisibility(View.GONE);
                }
                // binding.captionParent.setVisibility(View.GONE);
                binding.bottomBg.setVisibility(View.GONE);
                binding.likesCount.setVisibility(View.GONE);
                binding.commentsCount.setVisibility(View.GONE);
                binding.date.setVisibility(View.GONE);
                binding.comment.setVisibility(View.GONE);
                // binding.captionToggle.setVisibility(View.GONE);
                // binding.playerControlsToggle.setVisibility(View.GONE);
                binding.like.setVisibility(View.GONE);
                binding.save.setVisibility(View.GONE);
                binding.share.setVisibility(View.GONE);
                binding.download.setVisibility(View.GONE);
                binding.mediaCounter.setVisibility(View.GONE);
                binding.viewsCount.setVisibility(View.GONE);
                final List<Integer> options = viewModel.getOptions().getValue();
                if (options != null && !options.isEmpty()) {
                    binding.options.setVisibility(View.GONE);
                }
                // wasControlsVisible = binding.playerControls.getRoot().getVisibility() == View.VISIBLE;
                // if (wasControlsVisible) {
                //     hidePlayerControls();
                // }
                return;
            }
            if (media.getUser() != null) {
                binding.profilePic.setVisibility(View.VISIBLE);
                binding.title.setVisibility(View.VISIBLE);
                binding.subtitle.setVisibility(View.VISIBLE);
                // binding.topBg.setVisibility(View.VISIBLE);
            }
            if (media.getLocation() != null) {
                binding.location.setVisibility(View.VISIBLE);
            }
            binding.bottomBg.setVisibility(View.VISIBLE);
            if (viewModel.hasPk()) {
                binding.likesCount.setVisibility(View.VISIBLE);
                binding.date.setVisibility(View.VISIBLE);
                // binding.captionParent.setVisibility(View.VISIBLE);
                // binding.captionToggle.setVisibility(View.VISIBLE);
                binding.share.setVisibility(View.VISIBLE);
            }
            if (viewModel.hasPk() && !viewModel.getMedia().isCommentsDisabled()) {
                binding.comment.setVisibility(View.VISIBLE);
                binding.commentsCount.setVisibility(View.VISIBLE);
            }
            binding.download.setVisibility(View.VISIBLE);
            final List<Integer> options = viewModel.getOptions().getValue();
            if (options != null && !options.isEmpty()) {
                binding.options.setVisibility(View.VISIBLE);
            }
            if (viewModel.isLoggedIn() && viewModel.hasPk()) {
                binding.like.setVisibility(View.VISIBLE);
                binding.save.setVisibility(View.VISIBLE);
            }
            if (video) {
                // binding.playerControlsToggle.setVisibility(View.VISIBLE);
                binding.viewsCount.setVisibility(View.VISIBLE);
            }
            // if (wasControlsVisible) {
            //     showPlayerControls();
            // }
            if (media.getMediaType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                binding.mediaCounter.setVisibility(View.VISIBLE);
            }
            detailsVisible = true;
        });
    }

    private void navigateToProfile(final String username) {
        final NavController navController = getNavController();
        if (navController == null) return;
        final Bundle bundle = new Bundle();
        bundle.putString("username", username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    @Nullable
    private NavController getNavController() {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            Log.e(TAG, "navigateToProfile", e);
        }
        return navController;
    }

    public boolean wasDeleted() {
        return wasDeleted;
    }
}