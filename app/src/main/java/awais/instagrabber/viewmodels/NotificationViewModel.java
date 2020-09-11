package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.NotificationModel;

public class NotificationViewModel extends ViewModel {
    private MutableLiveData<List<NotificationModel>> list;

    public MutableLiveData<List<NotificationModel>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}
