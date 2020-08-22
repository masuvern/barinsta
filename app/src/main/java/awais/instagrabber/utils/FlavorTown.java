package awais.instagrabber.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.interfaces.FetchListener;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class FlavorTown {
    public static void updateCheck(@NonNull final Context context) {
        Resources res = context.getResources();
        new UpdateChecker(version -> {
            if (!version.equals(BuildConfig.VERSION_NAME) && !BuildConfig.DEBUG) {
                new AlertDialog.Builder(context)
                        .setTitle(res.getString(R.string.update_available, version))
                        .setMessage(R.string.update_notice)
                        .setNeutralButton(R.string.cancel, null)
                        .setNegativeButton(R.string.action_github, (dialog, which) -> {
                            try {
                                context.startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                        Uri.parse("https://github.com/austinhuang0131/instagrabber/releases/latest")));
                            } catch (final ActivityNotFoundException e) {
                                // do nothing
                            }
                        })
                        .setPositiveButton(R.string.action_fdroid, (dialog, which) -> {
                            try {
                                context.startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                        Uri.parse("https://f-droid.org/packages/me.austinhuang.instagrabber/")));
                            } catch (final ActivityNotFoundException e) {
                                // do nothing
                            }
                        })
                        .show();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void changelogCheck(@NonNull final Context context) {
        if (settingsHelper.getInteger(Constants.PREV_INSTALL_VERSION) < BuildConfig.VERSION_CODE) {
            Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show();
            settingsHelper.putInteger(Constants.PREV_INSTALL_VERSION, BuildConfig.VERSION_CODE);
        }
    }
}