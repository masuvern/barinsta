package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import static android.app.Activity.RESULT_OK;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class DownloadsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = DownloadsPreferencesFragment.class.getSimpleName();
    private static final int SELECT_DIR_REQUEST_CODE = 1;
    private SaveToCustomFolderPreference.ResultCallback resultCallback;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final Context context = getContext();
        if (context == null) return;
        screen.addPreference(getDownloadUserFolderPreference(context));
        screen.addPreference(getSaveToCustomFolderPreference(context));
        screen.addPreference(getPrependUsernameToFilenamePreference(context));
    }

    private Preference getDownloadUserFolderPreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DOWNLOAD_USER_FOLDER);
        preference.setTitle(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getSaveToCustomFolderPreference(@NonNull final Context context) {
        return new SaveToCustomFolderPreference(context, (resultCallback) -> {
            // Choose a directory using the system's file picker.
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
            this.resultCallback = resultCallback;

            // new DirectoryChooser()
            //         .setInitialDirectory(settingsHelper.getString(FOLDER_PATH))
            //         .setInteractionListener(file -> {
            //             settingsHelper.putString(FOLDER_PATH, file.getAbsolutePath());
            //             resultCallback.onResult(file.getAbsolutePath());
            //         })
            //         .show(getParentFragmentManager(), null);
        });
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (data == null || data.getData() == null) return;
        if (resultCode != RESULT_OK || requestCode != SELECT_DIR_REQUEST_CODE) return;
        final Context context = getContext();
        if (context == null) return;
        final Uri dirUri = data.getData();
        Log.d(TAG, "onActivityResult: " + dirUri);
        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(dirUri, takeFlags);
        final DocumentFile root = DocumentFile.fromTreeUri(context, dirUri);
        settingsHelper.putString(FOLDER_PATH, data.getData().toString());
        if (resultCallback != null) {
            resultCallback.onResult(root.getName());
            resultCallback = null;
        }
        // Log.d(TAG, "onActivityResult: " + root);
    }

    private Preference getPrependUsernameToFilenamePreference(@NonNull final Context context) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(Constants.DOWNLOAD_PREPEND_USER_NAME);
        preference.setTitle(R.string.download_prepend_username);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    public static class SaveToCustomFolderPreference extends Preference {
        private AppCompatTextView customPathTextView;
        private final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener;
        private final String key;

        public SaveToCustomFolderPreference(final Context context, final OnSelectFolderButtonClickListener onSelectFolderButtonClickListener) {
            super(context);
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
                final String customPath = settingsHelper.getString(FOLDER_PATH);
                customPathTextView.setText(customPath);
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
    }
}
