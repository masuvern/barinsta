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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.interfaces.FetchListener;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class FlavorTown {
    public static void updateCheck(@NonNull final Context context) {
        new UpdateChecker(versionUrl -> {
            new AlertDialog.Builder(context).setTitle(R.string.update_available).setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.action_download, (dialog, which) -> {
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(versionUrl)));
                        } catch (final ActivityNotFoundException e) {
                            // do nothing
                        }
                    }).show();
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void changelogCheck(@NonNull final Context context) {
        if (settingsHelper.getInteger(Constants.PREV_INSTALL_VERSION) < BuildConfig.VERSION_CODE) {
            new ChangelogFetcher(new FetchListener<CharSequence>() {
                private AlertDialog alertDialog;
                private TextView textView;

                @Override
                public void doBefore() {
                    final ViewGroup rootView = (ViewGroup) View.inflate(context, R.layout.layout_changelog_textview, null);
                    textView = (TextView) rootView.getChildAt(0);
                    textView.setMovementMethod(new LinkMovementMethod());
                    alertDialog = new AlertDialog.Builder(context).setTitle(R.string.title_changelog).setView(rootView).create();
                }

                @Override
                public void onResult(final CharSequence result) {
                    if (alertDialog != null && textView != null && !Utils.isEmpty(result)) {
                        final Resources resources = context.getResources();

                        final SpannableStringBuilder stringBuilder = new SpannableStringBuilder(
                                resources.getString(R.string.curr_version, BuildConfig.VERSION_NAME))
                                .append('\n');

                        stringBuilder.setSpan(new RelativeSizeSpan(1.3f), 0, stringBuilder.length() - 1, 0);

                        final int resLen = result.length();
                        int versionTimes = 0;

                        for (int i = 0; i < resLen; ++i) {
                            final char c = result.charAt(i);

                            if (c == 'v' && i > 0) {
                                final char c1 = result.charAt(i - 1);
                                if (c1 == '\r' || c1 == '\n') {
                                    if (++versionTimes == 4) break;
                                }
                            }

                            stringBuilder.append(c);
                        }

                        final String strReadMore = resources.getString(R.string.read_more);
                        stringBuilder.append('\n').append(strReadMore);

                        final int sbLen = stringBuilder.length();
                        stringBuilder.setSpan(new URLSpan("https://gitlab.com/AwaisKing/instagrabber/-/blob/master/CHANGELOG"),
                                sbLen - strReadMore.length(), sbLen, 0);

                        textView.setText(stringBuilder, TextView.BufferType.SPANNABLE);

                        alertDialog.show();
                    }

                    settingsHelper.putInteger(Constants.PREV_INSTALL_VERSION, BuildConfig.VERSION_CODE);
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}