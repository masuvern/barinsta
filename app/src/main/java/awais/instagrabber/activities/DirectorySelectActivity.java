package awais.instagrabber.activities;

import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import awais.instagrabber.databinding.ActivityDirectorySelectBinding;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public class DirectorySelectActivity extends BaseLanguageActivity {
    private static final String TAG = DirectorySelectActivity.class.getSimpleName();

    public static final int SELECT_DIR_REQUEST_CODE = 1090;

    private Uri initialUri;
    private ActivityDirectorySelectBinding binding;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDirectorySelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.selectDir.setOnClickListener(v -> openDirectoryChooser());
        setInitialUri();
    }

    private void setInitialUri() {
        AppExecutors.getInstance().mainThread().execute(() -> {
            final Intent intent = getIntent();
            if (intent == null) {
                setMessage();
                return;
            }
            final Parcelable initialUriParcelable = intent.getParcelableExtra(Constants.EXTRA_INITIAL_URI);
            if (!(initialUriParcelable instanceof Uri)) {
                setMessage();
                return;
            }
            initialUri = (Uri) initialUriParcelable;
            setMessage();
        });
    }

    private void setMessage() {
        if (initialUri == null) {
            // default message
            binding.message.setText("Select a directory which Barinsta will use for downloads and temp files");
            return;
        }

        if (!initialUri.toString().startsWith("content")) {
            final String message = String.format("Android has changed the way apps can access files and directories on storage.\n\n" +
                                                         "Please re-select the directory '%s' after clicking the button below",
                                                 initialUri.toString());
            binding.message.setText(message);
            return;
        }

        final List<UriPermission> existingPermissions = getContentResolver().getPersistedUriPermissions();
        final boolean anyMatch = existingPermissions.stream().anyMatch(uriPermission -> uriPermission.getUri().equals(initialUri));
        if (!anyMatch) {
            // permission revoked message
            final String message = "Permissions for the previously selected directory '%s' were revoked by the system.\n\n" +
                    "Re-select the directory or select a new directory.";
            final DocumentFile documentFile = DocumentFile.fromSingleUri(this, initialUri);
            String path;
            try {
                path = URLDecoder.decode(initialUri.toString(), StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                path = initialUri.toString();
            }
            if (documentFile != null) {
                try {
                    final File file = Utils.getDocumentFileRealPath(this, documentFile);
                    if (file != null) {
                        path = file.getAbsolutePath();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "setMessage: ", e);
                }
            }
            binding.message.setText(String.format(message, path));
        }
    }

    private void openDirectoryChooser() {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SELECT_DIR_REQUEST_CODE) return;
        if (resultCode != RESULT_OK) {
            // Show error
            return;
        }
        if (data == null || data.getData() == null) {
            // show error
            return;
        }
        try {
            Utils.setupSelectedDir(this, data);
        } catch (Exception e) {
            // show error
        }
    }
}
