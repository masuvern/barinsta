package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;
import static awais.instagrabber.activities.DirectorySelectActivity.SELECT_DIR_REQUEST_CODE;
import static awais.instagrabber.fragments.settings.PreferenceKeys.PREF_BARINSTA_DIR_URI;
import static awais.instagrabber.utils.Constants.DOWNLOAD_USER_FOLDER;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class DownloadsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = DownloadsPreferencesFragment.class.getSimpleName();
    // private SaveToCustomFolderPreference.ResultCallback resultCallback;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        screen.addPreference(getDownloadUserFolderPreference(context));
        screen.addPreference(getPrependUsernameToFilenamePreference(context));
        screen.addPreference(getSaveToCustomFolderPreference(context));
    }

    private Preference getDownloadUserFolderPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(DOWNLOAD_USER_FOLDER);
        preference.setTitle(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getSaveToCustomFolderPreference(@NonNull final Context context) {
        final Preference preference = new Preference(context);
        preference.setKey(PREF_BARINSTA_DIR_URI);
        preference.setIconSpaceReserved(false);
        preference.setTitle(R.string.barinsta_folder);
        preference.setSummaryProvider(p -> {
            final String currentValue = settingsHelper.getString(PREF_BARINSTA_DIR_URI);
            if (TextUtils.isEmpty(currentValue)) return "";
            String path;
            try {
                path = URLDecoder.decode(currentValue, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                path = currentValue;
            }
            return path;
        });
        preference.setOnPreferenceClickListener(p -> {
            openDirectoryChooser(DownloadUtils.getRootDirUri());
            return true;
        });
        return preference;
        // return new SaveToCustomFolderPreference(context, checked -> {
        //     try {
        //         DownloadUtils.init(context);
        //     } catch (DownloadUtils.ReselectDocumentTreeException e) {
        //         if (!checked) return;
        //         startDocumentSelector(e.getInitialUri());
        //     } catch (Exception e) {
        //         Log.e(TAG, "getSaveToCustomFolderPreference: ", e);
        //     }
        // }, (resultCallback) -> {
        //     // Choose a directory using the system's file picker.
        //     startDocumentSelector(null);
        //     this.resultCallback = resultCallback;
        //
        //     // new DirectoryChooser()
        //     //         .setInitialDirectory(settingsHelper.getString(FOLDER_PATH))
        //     //         .setInteractionListener(file -> {
        //     //             settingsHelper.putString(FOLDER_PATH, file.getAbsolutePath());
        //     //             resultCallback.onResult(file.getAbsolutePath());
        //     //         })
        //     //         .show(getParentFragmentManager(), null);
        // });
    }

    private void openDirectoryChooser(final Uri initialUri) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (requestCode != SELECT_DIR_REQUEST_CODE) return;
        if (resultCode != RESULT_OK) return;
        if (data == null || data.getData() == null) return;
        final Context context = getContext();
        if (context == null) return;
        AppExecutors.getInstance().mainThread().execute(() -> {
            try {
                Utils.setupSelectedDir(context, data);
            } catch (Exception e) {
                // Should not come to this point.
                // If it does, we have to show this error to the user so that they can report it.
                try (final StringWriter sw = new StringWriter();
                     final PrintWriter pw = new PrintWriter(sw)) {
                    e.printStackTrace(pw);
                    final ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                            123,
                            R.string.error,
                            "Please report this error to the developers:\n\n" + sw.toString(),
                            R.string.ok,
                            0,
                            0
                    );
                    dialogFragment.show(getChildFragmentManager(), ConfirmDialogFragment.class.getSimpleName());
                } catch (IOException ioException) {
                    Log.e(TAG, "onActivityResult: ", ioException);
                }
            }
        }, 500);
    }

    private Preference getPrependUsernameToFilenamePreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DOWNLOAD_PREPEND_USER_NAME);
        preference.setTitle(R.string.download_prepend_username);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    // public static class SaveToCustomFolderPreference extends Preference {
    //     private AppCompatTextView customPathTextView;
    //     private final OnSaveToChangeListener onSaveToChangeListener;
    //     private final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener;
    //     private final String key;
    //
    //     public SaveToCustomFolderPreference(final Context context,
    //                                         final OnSaveToChangeListener onSaveToChangeListener,
    //                                         final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener) {
    //         super(context);
    //         this.onSaveToChangeListener = onSaveToChangeListener;
    //         this.onSelectFolderButtonClickListener = onSelectFolderButtonClickListener;
    //         key = FOLDER_SAVE_TO;
    //         setLayoutResource(R.layout.pref_custom_folder);
    //         setKey(key);
    //         setTitle(R.string.save_to_folder);
    //         setIconSpaceReserved(false);
    //     }
    //
    //     @Override
    //     public void onBindViewHolder(final PreferenceViewHolder holder) {
    //         super.onBindViewHolder(holder);
    //         final SwitchMaterial cbSaveTo = (SwitchMaterial) holder.findViewById(R.id.cbSaveTo);
    //         final View buttonContainer = holder.findViewById(R.id.button_container);
    //         customPathTextView = (AppCompatTextView) holder.findViewById(R.id.custom_path);
    //         cbSaveTo.setOnCheckedChangeListener((buttonView, isChecked) -> {
    //             settingsHelper.putBoolean(FOLDER_SAVE_TO, isChecked);
    //             buttonContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
    //             final Context context = getContext();
    //             String customPath = settingsHelper.getString(FOLDER_PATH);
    //             if (!TextUtils.isEmpty(customPath) && customPath.startsWith("content") && context != null) {
    //                 final Uri uri = Uri.parse(customPath);
    //                 final DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
    //                 try {
    //                     customPath = Utils.getDocumentFileRealPath(context, documentFile).getAbsolutePath();
    //                 } catch (Exception e) {
    //                     Log.e(TAG, "onBindViewHolder: ", e);
    //                 }
    //             }
    //             customPathTextView.setText(customPath);
    //             if (onSaveToChangeListener != null) {
    //                 onSaveToChangeListener.onChange(isChecked);
    //             }
    //         });
    //         final boolean savedToEnabled = settingsHelper.getBoolean(key);
    //         holder.itemView.setOnClickListener(v -> cbSaveTo.toggle());
    //         cbSaveTo.setChecked(savedToEnabled);
    //         buttonContainer.setVisibility(savedToEnabled ? View.VISIBLE : View.GONE);
    //         final AppCompatButton btnSaveTo = (AppCompatButton) holder.findViewById(R.id.btnSaveTo);
    //         btnSaveTo.setOnClickListener(v -> {
    //             if (onSelectFolderButtonClickListener == null) return;
    //             onSelectFolderButtonClickListener.onClick(result -> {
    //                 if (TextUtils.isEmpty(result)) return;
    //                 customPathTextView.setText(result);
    //             });
    //         });
    //     }
    //
    //     public interface ResultCallback {
    //         void onResult(String result);
    //     }
    //
    //     public interface OnSelectFolderButtonClickListener {
    //         void onClick(ResultCallback resultCallback);
    //     }
    //
    //     public interface OnSaveToChangeListener {
    //         void onChange(boolean checked);
    //     }
    // }
}
