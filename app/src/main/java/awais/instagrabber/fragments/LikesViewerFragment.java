package awais.instagrabber.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Collections;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.LikesAdapter;
import awais.instagrabber.databinding.FragmentLikesBinding;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.MediaService;
import awais.instagrabber.webservices.ServiceCallback;

public final class LikesViewerFragment extends BottomSheetDialogFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "LikesViewerFragment";

    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);

    private LikesAdapter likesAdapter;
    private FragmentLikesBinding binding;
    private LinearLayoutManager layoutManager;
    private Resources resources;
    private AppCompatActivity fragmentActivity;
    private LinearLayoutCompat root;
    private MediaService mediaService;
    private String postId;

    private final ServiceCallback<List<ProfileModel>> cb = new ServiceCallback<List<ProfileModel>>() {
        @Override
        public void onSuccess(final List<ProfileModel> result) {
            final LikesAdapter likesAdapter = new LikesAdapter(result, v -> {
                final Object tag = v.getTag();
                if (tag instanceof ProfileModel) {
                    ProfileModel model = (ProfileModel) tag;
                    final Bundle bundle = new Bundle();
                    bundle.putString("username", "@" + model.getUsername());
                    NavHostFragment.findNavController(LikesViewerFragment.this).navigate(R.id.action_global_profileFragment, bundle);
                }
            });
            binding.rvLikes.setAdapter(likesAdapter);
            binding.rvLikes.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onFailure(final Throwable t) {
            Log.e(TAG, "Error", t);
            try {
                final Context context = getContext();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
            catch (Exception e) {}
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        mediaService = MediaService.getInstance();
        // setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        binding = FragmentLikesBinding.inflate(getLayoutInflater());
        binding.swipeRefreshLayout.setEnabled(false);
        binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        init();
    }

    @Override
    public void onRefresh() {
        mediaService.fetchLikes(postId, cb);
    }

    private void init() {
        if (getArguments() == null) return;
        final LikesViewerFragmentArgs fragmentArgs = LikesViewerFragmentArgs.fromBundle(getArguments());
        postId = fragmentArgs.getPostId();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        binding.swipeRefreshLayout.setRefreshing(true);
        resources = getResources();
        onRefresh();
    }
}