package awais.instagrabber.activities;

import androidx.appcompat.app.AppCompatActivity;

import awais.instagrabber.utils.LocaleUtils;

public abstract class BaseLanguageActivity extends AppCompatActivity {
    protected BaseLanguageActivity() {
        LocaleUtils.updateConfig(this);
    }
}
