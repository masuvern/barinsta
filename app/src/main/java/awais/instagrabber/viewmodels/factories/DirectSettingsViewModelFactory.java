package awais.instagrabber.viewmodels.factories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.viewmodels.DirectSettingsViewModel;

public class DirectSettingsViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final String threadId;
    private final DirectThread backup;
    private final boolean pending;
    private final User currentUser;

    public DirectSettingsViewModelFactory(@NonNull final Application application,
                                          @NonNull final String threadId,
                                          @NonNull final DirectThread backup,
                                          final boolean pending,
                                          @NonNull final User currentUser) {
        this.application = application;
        this.threadId = threadId;
        this.backup = backup;
        this.pending = pending;
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull final Class<T> modelClass) {
        //noinspection unchecked
        return (T) new DirectSettingsViewModel(application, threadId, backup, pending, currentUser);
    }
}
