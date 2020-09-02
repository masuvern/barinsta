package awais.instagrabber.fragments.main.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.DiscoverItemModel;

public class DiscoverItemViewModel extends ViewModel {
    private MutableLiveData<List<DiscoverItemModel>> list;

    public MutableLiveData<List<DiscoverItemModel>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}