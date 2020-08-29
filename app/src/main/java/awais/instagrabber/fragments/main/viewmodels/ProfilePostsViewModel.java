package awais.instagrabber.fragments.main.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.PostModel;

public class ProfilePostsViewModel extends ViewModel {
    private MutableLiveData<List<PostModel>> list;

    public MutableLiveData<List<PostModel>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}