package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import awais.instagrabber.R;

public class ConfirmDialogFragment extends DialogFragment {
    private Context context;
    private ConfirmDialogFragmentCallback callback;

    private final int defaultPositiveButtonText = R.string.ok;
    // private final int defaultNegativeButtonText = R.string.cancel;

    @NonNull
    public static ConfirmDialogFragment newInstance(final int requestCode,
                                                    @StringRes final int title,
                                                    @StringRes final int message,
                                                    @StringRes final int positiveText,
                                                    @StringRes final int negativeText,
                                                    @StringRes final int neutralText) {
        return newInstance(requestCode, title, (Integer) message, positiveText, negativeText, neutralText);
    }

    @NonNull
    public static ConfirmDialogFragment newInstance(final int requestCode,
                                                    @StringRes final int title,
                                                    final String message,
                                                    @StringRes final int positiveText,
                                                    @StringRes final int negativeText,
                                                    @StringRes final int neutralText) {
        return newInstance(requestCode, title, (Object) message, positiveText, negativeText, neutralText);
    }

    @NonNull
    private static ConfirmDialogFragment newInstance(final int requestCode,
                                                     @StringRes final int title,
                                                     final Object message,
                                                     @StringRes final int positiveText,
                                                     @StringRes final int negativeText,
                                                     @StringRes final int neutralText) {
        Bundle args = new Bundle();
        args.putInt("requestCode", requestCode);
        if (title != 0) {
            args.putInt("title", title);
        }
        if (message != null) {
            if (message instanceof Integer) {
                args.putInt("message", (int) message);
            } else if (message instanceof String) {
                args.putString("message", (String) message);
            }
        }
        if (positiveText != 0) {
            args.putInt("positive", positiveText);
        }
        if (negativeText != 0) {
            args.putInt("negative", negativeText);
        }
        if (neutralText != 0) {
            args.putInt("neutral", neutralText);
        }
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        fragment.setArguments(args);
        return fragment;

    }

    public ConfirmDialogFragment() {}

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        this.context = context;
        final Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof ConfirmDialogFragmentCallback) {
            callback = (ConfirmDialogFragmentCallback) parentFragment;
            return;
        }
        final FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity instanceof ConfirmDialogFragmentCallback) {
            callback = (ConfirmDialogFragmentCallback) fragmentActivity;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        int title = 0;
        String message = null;
        int neutralButtonText = 0;
        int negativeButtonText = 0;

        final int positiveButtonText;
        final int requestCode;
        if (arguments != null) {
            title = arguments.getInt("title", 0);
            message = getMessage(arguments);
            positiveButtonText = arguments.getInt("positive", defaultPositiveButtonText);
            negativeButtonText = arguments.getInt("negative", 0);
            neutralButtonText = arguments.getInt("neutral", 0);
            requestCode = arguments.getInt("requestCode", 0);
        } else {
            requestCode = 0;
            positiveButtonText = defaultPositiveButtonText;
        }
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setPositiveButton(positiveButtonText, (d, w) -> {
                    if (callback == null) return;
                    callback.onPositiveButtonClicked(requestCode);
                });
        if (title != 0) {
            builder.setTitle(title);
        }
        if (message != null) {
            builder.setMessage(message);
        }
        if (negativeButtonText != 0) {
            builder.setNegativeButton(negativeButtonText, (dialog, which) -> {
                if (callback == null) return;
                callback.onNegativeButtonClicked(requestCode);
            });
        }
        if (neutralButtonText != 0) {
            builder.setNeutralButton(neutralButtonText, (dialog, which) -> {
                if (callback == null) return;
                callback.onNeutralButtonClicked(requestCode);
            });
        }
        return builder.create();
    }

    private String getMessage(@NonNull final Bundle arguments) {
        String message = null;
        final Object messageObject = arguments.get("message");
        if (messageObject != null) {
            if (messageObject instanceof Integer) {
                message = getString((int) messageObject);
            } else if (messageObject instanceof String) {
                message = (String) messageObject;
            }
        }
        return message;
    }

    public interface ConfirmDialogFragmentCallback {
        void onPositiveButtonClicked(int requestCode);

        void onNegativeButtonClicked(int requestCode);

        void onNeutralButtonClicked(int requestCode);
    }
}
