package awais.instagrabber.fragments;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.PostViewAdapter;
import awais.instagrabber.adapters.PostViewAdapter.OnPostViewChildViewClickListener;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.asyncs.i.iPostFetcher;
import awais.instagrabber.databinding.FragmentPostViewBinding;
import awais.instagrabber.fragments.main.viewmodels.ViewerPostViewModel;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.ViewerPostModelWrapper;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.services.MediaService;
import awais.instagrabber.services.ServiceCallback;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class PostViewFragment extends Fragment {
    private static final String TAG = "PostViewFragment";
    private static final String COOKIE = settingsHelper.getString(Constants.COOKIE);

    private FragmentActivity fragmentActivity;
    private FragmentPostViewBinding binding;
    private ViewPager2 root;
    private boolean shouldRefresh = true;
    private ViewerPostViewModel viewerPostViewModel;
    private boolean isId;
    private int currentPostIndex;
    private List<String> idOrCodeList;
    private boolean hasInitialResult = false;
    private PostViewAdapter adapter;
    private boolean session;
    private MediaService mediaService;

    private FetchListener<ViewerPostModel[]> pfl = result -> {
        if (result == null) return;
        if (result.length <= 0) return;
        final List<ViewerPostModelWrapper> viewerPostModels = viewerPostViewModel.getList().getValue();
        final List<ViewerPostModelWrapper> temp = viewerPostModels == null ? new ArrayList<>(idOrCodeList.size())
                                                                           : new ArrayList<>(viewerPostModels);
        final ViewerPostModel firstPost = result[0];
        if (firstPost == null) return;
        String idOrCode = isId ? firstPost.getPostId() : firstPost.getShortCode();
        if (idOrCode == null) return;
        // some values are appended to the post/short code with `_`
        idOrCode = idOrCode.substring(0, idOrCode.indexOf('_'));
        final int index = idOrCodeList.indexOf(idOrCode);
        if (index < 0) return;
        final ViewerPostModelWrapper viewerPostModelWrapper = temp.get(index);
        viewerPostModelWrapper.setViewerPostModels(result);
        temp.set(index, viewerPostModelWrapper);
        viewerPostViewModel.getList().setValue(temp);
        adapter.notifyItemChanged(index);
        if (!hasInitialResult) {
            Log.d(TAG, "setting delayed position to: " + currentPostIndex);
            binding.getRoot()
                   .postDelayed(() -> binding.getRoot().setCurrentItem(currentPostIndex), 200);
        }
        hasInitialResult = true;
    };
    private MentionClickListener mentionListener = (view, text, isHashtag, isLocation) -> {
        if (isHashtag) {
            final NavDirections action = PostViewFragmentDirections
                    .actionGlobalHashTagFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        if (isLocation) {
            final NavDirections action = PostViewFragmentDirections
                    .actionGlobalLocationFragment(text);
            NavHostFragment.findNavController(this).navigate(action);
            return;
        }
        final NavDirections action = PostViewFragmentDirections
                .actionGlobalProfileFragment("@" + text);
        NavHostFragment.findNavController(this).navigate(action);
    };
    private OnPostViewChildViewClickListener clickListener = (v, wrapper, postPosition, childPosition) -> {
        final ViewerPostModel postModel = wrapper.getViewerPostModels()[0];
        final String username = postModel.getProfileModel().getUsername();
        final int id = v.getId();
        switch (id) {
            case R.id.viewerCaption:
                break;
            case R.id.btnComments:
                // startActivity(new Intent(requireContext(), CommentsViewerFragment.class)
                //                       .putExtra(Constants.EXTRAS_SHORTCODE, postModel.getShortCode())
                //                       .putExtra(Constants.EXTRAS_POST, postModel.getPostId())
                //                       .putExtra(Constants.EXTRAS_USER, Utils.getUserIdFromCookie(COOKIE)));
                String postId = postModel.getPostId();
                if (postId.contains("_")) postId = postId.substring(0, postId.indexOf("_"));
                final NavDirections commentsAction = PostViewFragmentDirections.actionGlobalCommentsViewerFragment(
                        postModel.getShortCode(),
                        postId,
                        postModel.getProfileModel().getId()
                );
                NavHostFragment.findNavController(this).navigate(commentsAction);
                break;
            case R.id.btnDownload:
                if (checkSelfPermission(requireContext(),
                                        Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
                    showDownloadDialog(Arrays.asList(wrapper.getViewerPostModels()),
                                       childPosition,
                                       username);
                    return;
                }
                requestPermissions(Utils.PERMS, 8020);
                break;
            case R.id.ivProfilePic:
            case R.id.title:
                mentionListener.onClick(null, username, false, false);
                break;
            case R.id.btnLike:
                if (mediaService != null) {
                    final String userId = Utils.getUserIdFromCookie(COOKIE);
                    final String csrfToken = Utils.getCsrfTokenFromCookie(COOKIE);
                    v.setEnabled(false);
                    final ServiceCallback<Boolean> likeCallback = new ServiceCallback<Boolean>() {
                        @Override
                        public void onSuccess(final Boolean result) {
                            v.setEnabled(true);
                            if (result) {
                                postModel.setManualLike(!postModel.getLike());
                                adapter.notifyItemChanged(postPosition);
                                return;
                            }
                            Log.e(TAG, "like/unlike unsuccessful!");
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            v.setEnabled(true);
                            Log.e(TAG, "Error during like/unlike", t);
                        }
                    };
                    if (!postModel.getLike()) {
                        mediaService.like(postModel.getPostId(), userId, csrfToken, likeCallback);
                    } else {
                        mediaService.unlike(postModel.getPostId(), userId, csrfToken, likeCallback);
                    }
                }
                break;
            case R.id.btnBookmark:
                if (mediaService != null) {
                    final String userId = Utils.getUserIdFromCookie(COOKIE);
                    final String csrfToken = Utils.getCsrfTokenFromCookie(COOKIE);
                    v.setEnabled(false);
                    final ServiceCallback<Boolean> saveCallback = new ServiceCallback<Boolean>() {
                        @Override
                        public void onSuccess(final Boolean result) {
                            v.setEnabled(true);
                            if (result) {
                                postModel.setBookmarked(!postModel.getBookmark());
                                adapter.notifyItemChanged(postPosition);
                                return;
                            }
                            Log.e(TAG, "save/unsave unsuccessful!");
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            v.setEnabled(true);
                            Log.e(TAG, "Error during save/unsave", t);
                        }
                    };
                    if (!postModel.getBookmark()) {
                        mediaService.save(postModel.getPostId(), userId, csrfToken, saveCallback);
                    } else {
                        mediaService.unsave(postModel.getPostId(), userId, csrfToken, saveCallback);
                    }
                }
                break;
        }
    };
    private PostViewAdapter.OnPostCaptionLongClickListener captionLongClickListener = text -> Utils.copyText(requireContext(), text);

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = getActivity();
        mediaService = MediaService.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentPostViewBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        setupViewPager();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    private void setupViewPager() {
        viewerPostViewModel = new ViewModelProvider(fragmentActivity)
                .get(ViewerPostViewModel.class);
        adapter = new PostViewAdapter(clickListener, captionLongClickListener, mentionListener);
        root.setAdapter(adapter);
        root.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(final int position) {
                // Log.d(TAG, "onPageSelected: " + position + ", hasInitialResult: " + hasInitialResult);
                if (!hasInitialResult) {
                    return;
                }
                currentPostIndex = position;
                fetchPost();
            }
        });
        viewerPostViewModel.getList().observe(fragmentActivity, list -> adapter.submitList(list));
    }

    private void init() {
        if (getArguments() == null) return;
        final PostViewFragmentArgs fragmentArgs = PostViewFragmentArgs.fromBundle(getArguments());
        final String[] idOrCodeArray = fragmentArgs.getIdOrCodeArray();
        if (idOrCodeArray.length == 0) return;
        currentPostIndex = fragmentArgs.getIndex();
        if (currentPostIndex < 0) return;
        if (currentPostIndex >= idOrCodeArray.length) return;
        idOrCodeList = Arrays.asList(idOrCodeArray);
        viewerPostViewModel.getList().setValue(createPlaceholderModels(idOrCodeArray.length));
        isId = fragmentArgs.getIsId();
        fetchPost();
    }

    private List<ViewerPostModelWrapper> createPlaceholderModels(final int size) {
        final List<ViewerPostModelWrapper> viewerPostModels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            // viewerPostModels.add(new ViewerPostModel[]{ViewerPostModel.getDefaultModel(-i, "")});
            viewerPostModels.add(new ViewerPostModelWrapper(i, null));
        }
        return viewerPostModels;
    }

    private void fetchPost() {
        // Log.d(TAG, "fetchPost, currentPostIndex: " + currentPostIndex);
        final List<ViewerPostModelWrapper> list = viewerPostViewModel.getList().getValue();
        if (list != null) {
            final ViewerPostModelWrapper viewerPostModels = list.get(currentPostIndex);
            if (viewerPostModels != null && viewerPostModels
                    .getViewerPostModels() != null && viewerPostModels
                    .getViewerPostModels().length > 0) {
                Log.d(TAG, "returning without fetching");
                return;
            }
        }
        final String idOrShortCode = idOrCodeList.get(currentPostIndex);
        if (isId) {
            new iPostFetcher(idOrShortCode, pfl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }
        new PostFetcher(idOrShortCode, pfl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showDownloadDialog(final List<ViewerPostModel> postModels,
                                    final int childPosition,
                                    final String username) {
        final List<ViewerPostModel> postModelsToDownload = new ArrayList<>();
        if (!session && postModels.size() > 1) {
            final DialogInterface.OnClickListener clickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    postModelsToDownload.addAll(postModels);
                } else if (which == DialogInterface.BUTTON_POSITIVE) {
                    postModelsToDownload.add(postModels.get(childPosition));
                } else {
                    session = true;
                    postModelsToDownload.add(postModels.get(childPosition));
                }
                if (postModelsToDownload.size() > 0) {
                    Utils.batchDownload(requireContext(),
                                        username,
                                        DownloadMethod.DOWNLOAD_POST_VIEWER,
                                        postModelsToDownload);
                }
            };
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.post_viewer_download_dialog_title)
                    .setMessage(R.string.post_viewer_download_message)
                    .setNeutralButton(R.string.post_viewer_download_session, clickListener)
                    .setPositiveButton(R.string.post_viewer_download_current, clickListener)
                    .setNegativeButton(R.string.post_viewer_download_album, clickListener).show();
        } else {
            Utils.batchDownload(requireContext(),
                                username,
                                DownloadMethod.DOWNLOAD_POST_VIEWER,
                                Collections.singletonList(postModels.get(childPosition)));
        }
    }
}