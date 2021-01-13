package awais.instagrabber.utils;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.databinding.DialogUpdateBinding;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class FlavorTown {
    private static final String TAG = "FlavorTown";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static AlertDialog dialog;

    public static void updateCheck(@NonNull final AppCompatActivity context) {
        updateCheck(context, false);
    }

    @SuppressLint("PackageManagerGetSignatures")
    public static void updateCheck(@NonNull final AppCompatActivity context, final boolean force) {
        boolean isInstalledFromFdroid = false;
        final PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : packageInfo.signatures) {
                final X509Certificate cert = X509Certificate.getInstance(signature.toByteArray());
                final String fingerprint = bytesToHex(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded()));
                isInstalledFromFdroid = fingerprint.equals(Constants.FDROID_SHA1_FINGERPRINT);
                // Log.d(TAG, "fingerprint:" + fingerprint);
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException | CertificateException e) {
            Log.e(TAG, "Error", e);
        }
        if (isInstalledFromFdroid) return;
        final DialogUpdateBinding binding = DialogUpdateBinding.inflate(context.getLayoutInflater(), null, false);
        binding.skipUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (dialog == null) return;
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!isChecked);
        });
        Resources res = context.getResources();
        new UpdateChecker(version -> {
            if (force && version.equals(BuildConfig.VERSION_NAME)) {
                Toast.makeText(context, "You're already on the latest version", Toast.LENGTH_SHORT).show();
                return;
            }
            final String skippedVersion = settingsHelper.getString(Constants.SKIPPED_VERSION);
            final boolean shouldShowDialog = force || (!version.equals(BuildConfig.VERSION_NAME) && !BuildConfig.DEBUG && !skippedVersion
                    .equals(version));
            if (!shouldShowDialog) return;
            dialog = new AlertDialog.Builder(context)
                    .setTitle(res.getString(R.string.update_available, version))
                    .setView(binding.getRoot())
                    .setNeutralButton(R.string.cancel, (dialog, which) -> {
                        if (binding.skipUpdate.isChecked()) {
                            settingsHelper.putString(Constants.SKIPPED_VERSION, version);
                        }
                        dialog.dismiss();
                    })
                    .setPositiveButton(R.string.action_github, (dialog1, which) -> {
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("https://github.com/austinhuang0131/instagrabber/releases/latest")));
                        } catch (final ActivityNotFoundException e) {
                            // do nothing
                        }
                    })
                    // if we don't show dialog for fdroid users, is the below required?
                    .setNegativeButton(R.string.action_fdroid, (dialog, which) -> {
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("https://f-droid.org/packages/me.austinhuang.instagrabber/")));
                        } catch (final ActivityNotFoundException e) {
                            // do nothing
                        }
                    })
                    .show();
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void changelogCheck(@NonNull final Context context) {
        if (settingsHelper.getInteger(Constants.PREV_INSTALL_VERSION) < BuildConfig.VERSION_CODE) {
            final String langCode = settingsHelper.getString(Constants.APP_LANGUAGE);
            final String lang = LocaleUtils.getCorrespondingLanguageCode(langCode);
            final int appUaCode = settingsHelper.getInteger(Constants.APP_UA_CODE);
            final String appUa = UserAgentUtils.generateAppUA(appUaCode, lang);
            settingsHelper.putString(Constants.APP_UA, appUa);
            final int browserUaCode = settingsHelper.getInteger(Constants.BROWSER_UA_CODE);
            final String browserUa = UserAgentUtils.generateBrowserUA(browserUaCode);
            settingsHelper.putString(Constants.BROWSER_UA, browserUa);
            Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show();
            settingsHelper.putInteger(Constants.PREV_INSTALL_VERSION, BuildConfig.VERSION_CODE);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}