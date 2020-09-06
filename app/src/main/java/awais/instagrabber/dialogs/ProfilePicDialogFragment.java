package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;

import java.io.File;

import awais.instagrabber.R;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.asyncs.ProfilePictureFetcher;
import awais.instagrabber.databinding.DialogProfilepicBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.utils.Utils;

public class ProfilePicDialogFragment extends DialogFragment {
    private static final String TAG = "ProfilePicDlgFragment";

    private final String id;
    private final String name;
    private final String fallbackUrl;

    private DialogProfilepicBinding binding;
    private String url;

    public ProfilePicDialogFragment(final String id, final String name, final String fallbackUrl) {
        this.id = id;
        this.name = name;
        this.fallbackUrl = fallbackUrl;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = DialogProfilepicBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setLayout(width, height);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        fetchPhoto();
    }

    private void init() {
        binding.download.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
                downloadProfilePicture();
                return;
            }
            requestPermissions(Utils.PERMS, 8020);
        });
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadProfilePicture();
        }
    }

    private void fetchPhoto() {
        final FetchListener<String> fetchListener = profileUrl -> {
            url = profileUrl;
            if (Utils.isEmpty(url)) {
                url = fallbackUrl;
            }
            final DraweeController controller = Fresco
                    .newDraweeControllerBuilder()
                    .setUri(url)
                    .setOldController(binding.imageViewer.getController())
                    .setControllerListener(new BaseControllerListener<ImageInfo>() {
                        @Override
                        public void onFailure(final String id, final Throwable throwable) {
                            super.onFailure(id, throwable);
                            binding.download.setVisibility(View.GONE);
                            binding.progressView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onFinalImageSet(final String id,
                                                    final ImageInfo imageInfo,
                                                    final Animatable animatable) {
                            super.onFinalImageSet(id, imageInfo, animatable);
                            binding.download.setVisibility(View.VISIBLE);
                            binding.progressView.setVisibility(View.GONE);
                        }
                    })
                    .build();
            binding.imageViewer.setController(controller);
        };
        new ProfilePictureFetcher(name, id, fetchListener, url, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void downloadProfilePicture() {
        if (url == null) return;
        final File dir = new File(Environment.getExternalStorageDirectory(), "Download");
        if (dir.exists() || dir.mkdirs()) {
            final File saveFile = new File(dir, name + '_' + System.currentTimeMillis() + ".jpg");
            new DownloadAsync(requireContext(),
                              url,
                              saveFile,
                              result -> {
                                  final int toastRes = result != null && result.exists() ?
                                                       R.string.downloader_downloaded_in_folder : R.string.downloader_error_download_file;
                                  Toast.makeText(requireContext(), toastRes, Toast.LENGTH_SHORT).show();
                              })
                    .setItems(null, name)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }
        Toast.makeText(requireContext(), R.string.downloader_error_creating_folder, Toast.LENGTH_SHORT).show();
    }

    // private void showImageInfo() {
    //     final Drawable drawable = profileBinding.imageViewer.getDrawable();
    //     if (drawable != null) {
    //         final StringBuilder info = new StringBuilder(
    //                 getString(R.string.profile_viewer_imageinfo, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()));
    //         if (drawable instanceof BitmapDrawable) {
    //             final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
    //             if (bitmap != null) {
    //                 final String colorDepthPrefix = getString(R.string.profile_viewer_colordepth_prefix);
    //                 switch (bitmap.getConfig()) {
    //                     case ALPHA_8:
    //                         info.append(colorDepthPrefix).append(" 8-bits(A)");
    //                         break;
    //                     case RGB_565:
    //                         info.append(colorDepthPrefix).append(" 16-bits-A");
    //                         break;
    //                     case ARGB_4444:
    //                         info.append(colorDepthPrefix).append(" 16-bits+A");
    //                         break;
    //                     case ARGB_8888:
    //                         info.append(colorDepthPrefix).append(" 32-bits+A");
    //                         break;
    //                     case RGBA_F16:
    //                         info.append(colorDepthPrefix).append(" 64-bits+A");
    //                         break;
    //                     case HARDWARE:
    //                         info.append(colorDepthPrefix).append(" auto");
    //                         break;
    //                 }
    //             }
    //         }
    //         profileBinding.imageInfo.setText(info);
    //         profileBinding.imageInfo.setVisibility(View.VISIBLE);
    //     }
    // }
}
