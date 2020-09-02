package awais.instagrabber.fragments.main.viewmodels;

import androidx.lifecycle.MutableLiveData;

import java.util.List;

import awais.instagrabber.models.ViewerPostModel;

public class ViewerPostViewModel extends BasePostViewModel<ViewerPostModel> {
    private MutableLiveData<List<ViewerPostModel>> list;

    public MutableLiveData<List<ViewerPostModel>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}
