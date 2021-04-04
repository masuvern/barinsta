package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class DownloadsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = DownloadsPreferencesFragment.class.getSimpleName();
    private SaveToCustomFolderPreference.ResultCallback resultCallback;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        screen.addPreference(getDownloadUserFolderPreference(context));
        // screen.addPreference(getSaveToCustomFolderPreference(context));
        screen.addPreference(getPrependUsernameToFilenamePreference(context));
    }

    private Preference getDownloadUserFolderPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DOWNLOAD_USER_FOLDER);
        preference.setTitle(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    // private Preference getSaveToCustomFolderPreference(@NonNull final Context context) {
    //     return new SaveToCustomFolderPreference(context, checked -> {
    //         try {
    //             DownloadUtils.init(context);
    //         } catch (DownloadUtils.ReselectDocumentTreeException e) {
    //             if (!checked) return;
    //             startDocumentSelector(e.getInitialUri());
    //         } catch (Exception e) {
    //             Log.e(TAG, "getSaveToCustomFolderPreference: ", e);
    //         }
    //     }, (resultCallback) -> {
    //         // Choose a directory using the system's file picker.
    //         startDocumentSelector(null);
    //         this.resultCallback = resultCallback;
    //
    //         // new DirectoryChooser()
    //         //         .setInitialDirectory(settingsHelper.getString(FOLDER_PATH))
    //         //         .setInteractionListener(file -> {
    //         //             settingsHelper.putString(FOLDER_PATH, file.getAbsolutePath());
    //         //             resultCallback.onResult(file.getAbsolutePath());
    //         //         })
    //         //         .show(getParentFragmentManager(), null);
    //     });
    // }

    // private void startDocumentSelector(final Uri initialUri) {
    //     final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
    //         intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
    //     }
    //     startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
    // }

    // @Override
    // public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
    //     if (requestCode != SELECT_DIR_REQUEST_CODE) return;
    //     final Context context = getContext();
    //     if (context == null) return;
    //     if (resultCode != RESULT_OK) {
    //         try {
    //             DownloadUtils.init(context, true);
    //         } catch (Exception ignored) {}
    //         return;
    //     }
    //     if (data == null || data.getData() == null) return;
    //     Utils.setupSelectedDir(context, data);
    //     if (resultCallback != null) {
    //         try {
    //             final DocumentFile root = DocumentFile.fromTreeUri(context, data.getData());
    //             resultCallback.onResult(Utils.getDocumentFileRealPath(context, root).getAbsolutePath());
    //         } catch (Exception e) {
    //             Log.e(TAG, "onActivityResult: ", e);
    //         }
    //         resultCallback = null;
    //     }
    //     // Log.d(TAG, "onActivityResult: " + root);
    // }

    private Preference getPrependUsernameToFilenamePreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DOWNLOAD_PREPEND_USER_NAME);
        preference.setTitle(R.string.download_prepend_username);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    public static class SaveToCustomFolderPreference extends Preference {
        private AppCompatTextView customPathTextView;
        private final OnSaveToChangeListener onSaveToChangeListener;
        private final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener;
        private final String key;

        public SaveToCustomFolderPreference(final Context context,
                                            final OnSaveToChangeListener onSaveToChangeListener,
                                            final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener) {
            super(context);
            this.onSaveToChangeListener = onSaveToChangeListener;
            this.onSelectFolderButtonClickListener = onSelectFolderButtonClickListener;
            key = FOLDER_SAVE_TO;
            setLayoutResource(R.layout.pref_custom_folder);
            setKey(key);
            setTitle(R.string.save_to_folder);
            setIconSpaceReserved(false);
        }

        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            final SwitchMaterial cbSaveTo = (SwitchMaterial) holder.findViewById(R.id.cbSaveTo);
            final View buttonContainer = holder.findViewById(R.id.button_container);
            customPathTextView = (AppCompatTextView) holder.findViewById(R.id.custom_path);
            cbSaveTo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsHelper.putBoolean(FOLDER_SAVE_TO, isChecked);
                buttonContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                final Context context = getContext();
                String customPath = settingsHelper.getString(FOLDER_PATH);
                if (!TextUtils.isEmpty(customPath) && customPath.startsWith("content") && context != null) {
                    final Uri uri = Uri.parse(customPath);
                    final DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
                    try {
                        customPath = Utils.getDocumentFileRealPath(context, documentFile).getAbsolutePath();
                    } catch (Exception e) {
                        Log.e(TAG, "onBindViewHolder: ", e);
                    }
                }
                customPathTextView.setText(customPath);
                if (onSaveToChangeListener != null) {
                    onSaveToChangeListener.onChange(isChecked);
                }
            });
            final boolean savedToEnabled = settingsHelper.getBoolean(key);
            holder.itemView.setOnClickListener(v -> cbSaveTo.toggle());
            cbSaveTo.setChecked(savedToEnabled);
            buttonContainer.setVisibility(savedToEnabled ? View.VISIBLE : View.GONE);
            final AppCompatButton btnSaveTo = (AppCompatButton) holder.findViewById(R.id.btnSaveTo);
            btnSaveTo.setOnClickListener(v -> {
                if (onSelectFolderButtonClickListener == null) return;
                onSelectFolderButtonClickListener.onClick(result -> {
                    if (TextUtils.isEmpty(result)) return;
                    customPathTextView.setText(result);
                });
            });
        }

        public interface ResultCallback {
            void onResult(String result);
        }

        public interface OnSelectFolderButtonClickListener {
            void onClick(ResultCallback resultCallback);
        }

        public interface OnSaveToChangeListener {
            void onChange(boolean checked);
        }
    }
}
