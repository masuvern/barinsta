package awais.instagrabber.interfaces;

import java.io.Serializable;
import java.util.List;

import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.enums.PostItemType;

public interface ItemGetter extends Serializable {
    List<? extends BasePostModel> get(final PostItemType postItemType);
}