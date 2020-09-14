package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.BasePostModel;

public class BasePostViewModel<T extends BasePostModel> extends ViewModel {
    private MutableLiveData<List<T>> list;

    public MutableLiveData<List<T>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}