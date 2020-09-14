package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.ViewerPostModelWrapper;

public class ViewerPostViewModel extends ViewModel {
    private MutableLiveData<List<ViewerPostModelWrapper>> list;

    public MutableLiveData<List<ViewerPostModelWrapper>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}
