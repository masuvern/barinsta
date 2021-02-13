package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import awais.instagrabber.R;

public class ConfirmDialogFragment extends DialogFragment {
    private Context context;
    private ConfirmDialogFragmentCallback callback;

    @NonNull
    public static ConfirmDialogFragment newInstance(final int requestCode,
                                                    @StringRes final int title,
                                                    @StringRes final int message,
                                                    @StringRes final int positiveText,
                                                    @StringRes final int negativeText,
                                                    @StringRes final int neutralText) {
        Bundle args = new Bundle();
        args.putInt("requestCode", requestCode);
        args.putInt("title", title);
        args.putInt("message", message);
        args.putInt("positive", positiveText);
        args.putInt("negative", negativeText);
        args.putInt("neutral", neutralText);
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ConfirmDialogFragment() {}

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        try {
            callback = (ConfirmDialogFragmentCallback) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling fragment must implement ConfirmDialogFragmentCallback interface");
        }
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        int title = -1;
        int message = -1;
        int positiveButtonText = R.string.ok;
        int negativeButtonText = R.string.cancel;
        int neutralButtonText = -1;
        final int requestCode;
        if (arguments != null) {
            title = arguments.getInt("title", -1);
            message = arguments.getInt("message", -1);
            positiveButtonText = arguments.getInt("positive", R.string.ok);
            negativeButtonText = arguments.getInt("negative", R.string.cancel);
            neutralButtonText = arguments.getInt("neutral", -1);
            requestCode = arguments.getInt("requestCode", 0);
        } else {
            requestCode = 0;
        }
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setPositiveButton(positiveButtonText, (d, w) -> {
                    if (callback == null) return;
                    callback.onPositiveButtonClicked(requestCode);
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> {
                    if (callback == null) return;
                    callback.onNegativeButtonClicked(requestCode);
                });
        if (title > 0) {
            builder.setTitle(title);
        }
        if (message > 0) {
            builder.setMessage(message);
        }
        if (neutralButtonText > 0) {
            builder.setNeutralButton(neutralButtonText, (dialog, which) -> {
                if (callback == null) return;
                callback.onNeutralButtonClicked(requestCode);
            });
        }
        return builder.create();
    }

    public interface ConfirmDialogFragmentCallback {
        void onPositiveButtonClicked(int requestCode);

        void onNegativeButtonClicked(int requestCode);

        void onNeutralButtonClicked(int requestCode);
    }
}
