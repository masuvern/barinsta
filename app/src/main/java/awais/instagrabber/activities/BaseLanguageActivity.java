package awais.instagrabber.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;

public abstract class BaseLanguageActivity extends AppCompatActivity {
    protected BaseLanguageActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        Utils.changeTheme(this);
        super.onCreate(savedInstanceState);
    }
}
