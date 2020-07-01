package awais.instagrabber.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import awais.instagrabber.R;

import static awais.instagrabber.utils.Constants.PROFILE_FETCH_MODE;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class ProfileSettingsDialog extends BottomSheetDialogFragment implements AdapterView.OnItemSelectedListener {
    private int fetchIndex;
    private Activity activity;
    private Spinner spProfileFetchMode;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);

        final Context context = getContext();
        activity = context instanceof Activity ? (Activity) context : getActivity();

        final View contentView = View.inflate(activity, R.layout.dialog_profile_settings, null);

        spProfileFetchMode = contentView.findViewById(R.id.spProfileFetchMode);

        fetchIndex = Math.min(2, Math.max(0, settingsHelper.getInteger(PROFILE_FETCH_MODE)));
        spProfileFetchMode.setSelection(fetchIndex);
        spProfileFetchMode.setOnItemSelectedListener(this);

        dialog.setContentView(contentView);

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (activity != null && (spProfileFetchMode == null || fetchIndex != spProfileFetchMode.getSelectedItemPosition()))
            activity.recreate();
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
        settingsHelper.putInteger(PROFILE_FETCH_MODE, position);
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) { }
}