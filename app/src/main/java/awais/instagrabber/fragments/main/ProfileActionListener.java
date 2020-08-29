package awais.instagrabber.fragments.main;

import android.content.Intent;
import android.view.View;

import awais.instagrabber.MainHelper;
import awais.instagrabber.activities.SavedViewer;
import awais.instagrabber.databinding.FragmentProfileBinding;
import awais.instagrabber.models.LocationModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.Utils;

public class ProfileActionListener implements View.OnClickListener {

    private String cookie;
    private boolean isLoggedIn;
    private ProfileModel profileModel;
    private String userQuery;
    private FragmentProfileBinding binding;
    private LocationModel locationModel;

    public ProfileActionListener(final String cookie, final boolean isLoggedIn, final ProfileModel profileModel, final String userQuery, final FragmentProfileBinding binding, final LocationModel locationModel) {
        this.cookie = cookie;
        this.isLoggedIn = isLoggedIn;
        this.profileModel = profileModel;
        this.userQuery = userQuery;
        this.binding = binding;
        this.locationModel = locationModel;
    }

    @Override
    public void onClick(final View v) {



        // else if (v == binding.btnFollow) {
        //
        // } else if (v == mainActivity.mainBinding.profileView.btnRestrict && isLoggedIn) {
        //     new ProfileAction().execute("restrict");
        // } else if (v == mainActivity.mainBinding.profileView.btnSaved && !isSelf) {
        //     new ProfileAction().execute("block");
        // } else if (v == mainActivity.mainBinding.profileView.btnFollowTag) {
        //     new ProfileAction().execute("followtag");
        // } else if (v == mainActivity.mainBinding.profileView.btnTagged || (v == mainActivity.mainBinding.profileView.btnRestrict && !isLoggedIn)) {
        //     mainActivity.startActivity(new Intent(mainActivity, SavedViewer.class)
        //             .putExtra(Constants.EXTRAS_INDEX, "%" + mainActivity.profileModel.getId())
        //             .putExtra(Constants.EXTRAS_USER, "@" + mainActivity.profileModel.getUsername())
        //     );
        // } else if (v == mainActivity.mainBinding.profileView.btnSaved) {
        //
        // } else if (v == mainActivity.mainBinding.profileView.btnLiked) {
        //
        // }
    }
}
