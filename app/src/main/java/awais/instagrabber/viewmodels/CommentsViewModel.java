package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.CommentModel;

public class CommentsViewModel extends ViewModel {
    private MutableLiveData<List<CommentModel>> list;

    public MutableLiveData<List<CommentModel>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}
