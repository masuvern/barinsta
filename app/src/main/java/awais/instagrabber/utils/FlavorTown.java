package awais.instagrabber.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ThreadLocalRandom;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class FlavorTown {
    private static final String TAG = "FlavorTown";
    private static final UpdateChecker UPDATE_CHECKER = UpdateChecker.getInstance();

    private static boolean checking = false;

    public static void updateCheck(@NonNull final AppCompatActivity context) {
        updateCheck(context, false);
    }

    public static void updateCheck(@NonNull final AppCompatActivity context,
                                   final boolean force) {
        if (checking) return;
        checking = true;
        AppExecutors.getInstance().networkIO().execute(() -> {
            final String version = UPDATE_CHECKER.getLatestVersion();
            if (version == null) return;
            if (force && version.equals(BuildConfig.VERSION_NAME)) {
                Toast.makeText(context, "You're already on the latest version", Toast.LENGTH_SHORT).show();
                return;
            }
            final boolean shouldShowDialog = UpdateCheckCommon.shouldShowUpdateDialog(force, version);
            if (!shouldShowDialog) return;
            UpdateCheckCommon.showUpdateDialog(context, version, (dialog, which) -> {
                UPDATE_CHECKER.onDownload(context);
                dialog.dismiss();
            });
        });
    }

    public static void changelogCheck(@NonNull final Context context) {
        if (settingsHelper.getInteger(Constants.PREV_INSTALL_VERSION) < BuildConfig.VERSION_CODE) {
            int appUaCode = settingsHelper.getInteger(Constants.APP_UA_CODE);
            int browserUaCode = settingsHelper.getInteger(Constants.BROWSER_UA_CODE);
            if (browserUaCode == -1 || browserUaCode >= UserAgentUtils.browsers.length) {
                browserUaCode = ThreadLocalRandom.current().nextInt(0, UserAgentUtils.browsers.length);
                settingsHelper.putInteger(Constants.BROWSER_UA_CODE, browserUaCode);
            }
            if (appUaCode == -1 || appUaCode >= UserAgentUtils.devices.length) {
                appUaCode = ThreadLocalRandom.current().nextInt(0, UserAgentUtils.devices.length);
                settingsHelper.putInteger(Constants.APP_UA_CODE, appUaCode);
            }
            final String appUa = UserAgentUtils.generateAppUA(appUaCode, LocaleUtils.getCurrentLocale().getLanguage());
            settingsHelper.putString(Constants.APP_UA, appUa);
            final String browserUa = UserAgentUtils.generateBrowserUA(browserUaCode);
            settingsHelper.putString(Constants.BROWSER_UA, browserUa);
            Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show();
            settingsHelper.putInteger(Constants.PREV_INSTALL_VERSION, BuildConfig.VERSION_CODE);
        }
    }
}