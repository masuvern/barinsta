package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import awais.instagrabber.databinding.DialogTimeSettingsBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class TimeSettingsDialog extends DialogFragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, TextWatcher {
    private DialogTimeSettingsBinding timeSettingsBinding;
    private final Date magicDate;
    private SimpleDateFormat currentFormat;
    private String selectedFormat;

    public TimeSettingsDialog() {
        super();
        final Calendar instance = GregorianCalendar.getInstance();
        instance.set(2020, 5, 22, 8, 17, 13);
        magicDate = instance.getTime();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        timeSettingsBinding = DialogTimeSettingsBinding.inflate(LayoutInflater.from(getContext()));

        timeSettingsBinding.cbCustomFormat.setOnCheckedChangeListener(this);

        timeSettingsBinding.cbCustomFormat.setChecked(settingsHelper.getBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED));
        timeSettingsBinding.etCustomFormat.setText(settingsHelper.getString(Constants.CUSTOM_DATE_TIME_FORMAT));

        final String[] dateTimeFormat = settingsHelper.getString(Constants.DATE_TIME_SELECTION).split(";"); // output = time;separator;date
        timeSettingsBinding.spTimeFormat.setSelection(Integer.parseInt(dateTimeFormat[0]));
        timeSettingsBinding.spSeparator.setSelection(Integer.parseInt(dateTimeFormat[1]));
        timeSettingsBinding.spDateFormat.setSelection(Integer.parseInt(dateTimeFormat[2]));

        timeSettingsBinding.cbSwapTimeDate.setOnCheckedChangeListener(this);

        refreshTimeFormat();

        timeSettingsBinding.spTimeFormat.setOnItemSelectedListener(this);
        timeSettingsBinding.spDateFormat.setOnItemSelectedListener(this);
        timeSettingsBinding.spSeparator.setOnItemSelectedListener(this);

        timeSettingsBinding.etCustomFormat.addTextChangedListener(this);
        timeSettingsBinding.btnConfirm.setOnClickListener(this);
        timeSettingsBinding.btnInfo.setOnClickListener(this);

        dialog.setContentView(timeSettingsBinding.getRoot());
        return dialog;
    }

    private void refreshTimeFormat() {
        if (timeSettingsBinding.cbCustomFormat.isChecked()) {
            timeSettingsBinding.btnConfirm.setEnabled(false);
            checkCustomTimeFormat();
        } else {
            final String sepStr = String.valueOf(timeSettingsBinding.spSeparator.getSelectedItem());
            final String timeStr = String.valueOf(timeSettingsBinding.spTimeFormat.getSelectedItem());
            final String dateStr = String.valueOf(timeSettingsBinding.spDateFormat.getSelectedItem());

            final boolean isSwapTime = !timeSettingsBinding.cbSwapTimeDate.isChecked();

            selectedFormat = (isSwapTime ? timeStr : dateStr)
                    + (Utils.isEmpty(sepStr) || timeSettingsBinding.spSeparator.getSelectedItemPosition() == 0 ? " " : " '" + sepStr + "' ")
                    + (isSwapTime ? dateStr : timeStr);

            timeSettingsBinding.btnConfirm.setEnabled(true);
            timeSettingsBinding.timePreview.setText((currentFormat = new SimpleDateFormat(selectedFormat, LocaleUtils.getCurrentLocale())).format(magicDate));
        }
    }

    private void checkCustomTimeFormat() {
        try {
            //noinspection ConstantConditions
            final String string = timeSettingsBinding.etCustomFormat.getText().toString();
            if (Utils.isEmpty(string)) throw new NullPointerException();

            final String format = (currentFormat = new SimpleDateFormat(string, LocaleUtils.getCurrentLocale())).format(magicDate);
            timeSettingsBinding.timePreview.setText(format);

            timeSettingsBinding.btnConfirm.setEnabled(true);
        } catch (final Exception e) {
            timeSettingsBinding.btnConfirm.setEnabled(false);
            timeSettingsBinding.timePreview.setText(null);
        }
    }

    @Override
    public void onItemSelected(final AdapterView<?> p, final View v, final int pos, final long id) {
        refreshTimeFormat();
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if (buttonView == timeSettingsBinding.cbCustomFormat) {
            timeSettingsBinding.etCustomFormat.setEnabled(isChecked);
            timeSettingsBinding.btnInfo.setEnabled(isChecked);

            timeSettingsBinding.spTimeFormat.setEnabled(!isChecked);
            timeSettingsBinding.spDateFormat.setEnabled(!isChecked);
            timeSettingsBinding.spSeparator.setEnabled(!isChecked);
            timeSettingsBinding.cbSwapTimeDate.setEnabled(!isChecked);
        }
        refreshTimeFormat();
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        checkCustomTimeFormat();
    }

    @Override
    public void onClick(final View v) {
        if (v == timeSettingsBinding.btnConfirm) {
            final String formatSelection;

            final boolean isCustomFormat = timeSettingsBinding.cbCustomFormat.isChecked();

            if (isCustomFormat) {
                //noinspection ConstantConditions
                formatSelection = timeSettingsBinding.etCustomFormat.getText().toString();
                settingsHelper.putString(Constants.CUSTOM_DATE_TIME_FORMAT, formatSelection);
            } else {
                formatSelection = timeSettingsBinding.spTimeFormat.getSelectedItemPosition() + ";"
                        + timeSettingsBinding.spSeparator.getSelectedItemPosition() + ';'
                        + timeSettingsBinding.spDateFormat.getSelectedItemPosition(); // time;separator;date

                settingsHelper.putString(Constants.DATE_TIME_FORMAT, selectedFormat);
                settingsHelper.putString(Constants.DATE_TIME_SELECTION, formatSelection);
            }

            settingsHelper.putBoolean(Constants.CUSTOM_DATE_TIME_FORMAT_ENABLED, isCustomFormat);

            Utils.datetimeParser = (SimpleDateFormat) currentFormat.clone();
            dismiss();
        } else if (v == timeSettingsBinding.btnInfo) {
            timeSettingsBinding.customPanel.setVisibility(timeSettingsBinding.customPanel
                    .getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

        }
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) { }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) { }

    @Override
    public void afterTextChanged(final Editable s) { }
}