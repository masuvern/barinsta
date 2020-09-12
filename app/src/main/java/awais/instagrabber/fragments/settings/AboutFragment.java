package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.TimeSettingsDialog;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DirectoryChooser;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class AboutFragment extends BasePreferencesFragment {
    private static AppCompatTextView customPathTextView;

    @Override
    void setupPreferenceScreen(final PreferenceScreen screen) {
        final PreferenceCategory generalCategory = new PreferenceCategory(requireContext());
        screen.addPreference(generalCategory);
        generalCategory.setTitle(R.string.pref_category_general);
        generalCategory.setIconSpaceReserved(false);
        generalCategory.addPreference(getDocsPreference());
        generalCategory.addPreference(getRepoPreference());
        generalCategory.addPreference(getFeedbackPreference());

        final PreferenceCategory thirdPartyCategory = new PreferenceCategory(requireContext());
        screen.addPreference(thirdPartyCategory);
        thirdPartyCategory.setTitle(R.string.about_category_3pt);
        thirdPartyCategory.setSummary(R.string.about_category_3pt_summary);
        thirdPartyCategory.setIconSpaceReserved(false);
        // alphabetical order!!!
        thirdPartyCategory.addPreference(getExoPlayerPreference());
        thirdPartyCategory.addPreference(getFrescoPreference());
        thirdPartyCategory.addPreference(getJsoupPreference());
        thirdPartyCategory.addPreference(getRetrofitPreference());

        final PreferenceCategory licenseCategory = new PreferenceCategory(requireContext());
        screen.addPreference(licenseCategory);
        licenseCategory.setTitle(R.string.about_category_license);
        licenseCategory.setIconSpaceReserved(false);
        licenseCategory.addPreference(getLicensePreference());
        licenseCategory.addPreference(getLiabilityPreference());
    }

    private Preference getDocsPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle(R.string.about_documentation);
        preference.setSummary(R.string.about_documentation_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://instagrabber.austinhuang.me"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getRepoPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle(R.string.about_repository);
        preference.setSummary(R.string.about_repository_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/austinhuang0131/instagrabber"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getFeedbackPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle(R.string.about_feedback);
        preference.setSummary(R.string.about_feedback_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse(getString(R.string.about_feedback_summary)));
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getRetrofitPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle("Retrofit");
        preference.setSummary("Copyright 2013 Square, Inc. Apache Version 2.0.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://square.github.io/retrofit/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getJsoupPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle("jsoup");
        preference.setSummary("Copyright (c) 2009-2020 Jonathan Hedley. MIT License.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://jsoup.org/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getFrescoPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle("Fresco");
        preference.setSummary("Copyright (c) Facebook, Inc. and its affiliates. MIT License.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://frescolib.org/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getExoPlayerPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle("ExoPlayer");
        preference.setSummary("Copyright (C) 2016 The Android Open Source Project. Apache Version 2.0.");
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://exoplayer.dev/"));
            startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getLicensePreference() {
        final Preference preference = new Preference(requireContext());
        preference.setSummary(R.string.license);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_outline_info_24);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getLiabilityPreference() {
        final Preference preference = new Preference(requireContext());
        preference.setSummary(R.string.liability);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_warning);
        preference.setIconSpaceReserved(true);
        return preference;
    }
}
