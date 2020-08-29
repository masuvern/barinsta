package awais.instagrabber.services;

import awais.instagrabber.repositories.ProfileRepository;
import retrofit2.Retrofit;

public class ProfileService extends BaseService {
    private static final String TAG = "ProfileService";

    private final ProfileRepository repository;

    private static ProfileService instance;

    private ProfileService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(ProfileRepository.class);
    }

    public static ProfileService getInstance() {
        if (instance == null) {
            instance = new ProfileService();
        }
        return instance;
    }

    public void followProfile(final String username) {
        // final String url = "https://www.instagram.com/web/" + (action.equals("followtag") && mainActivity.hashtagModel != null ? "tags/" + (mainActivity.hashtagModel.getFollowing() ? "unfollow/" : "follow/") + mainActivity.hashtagModel.getName() + "/" : (action.equals("restrict") && mainActivity.profileModel != null ? "restrict_action" : "friendships/" + mainActivity.profileModel.getId()) + "/" + (action.equals("follow") ?
        //         mainActivity.profileModel.getFollowing() || mainActivity.profileModel.getRequested()
        //                 ? "unfollow/" : "follow/" :
        //         action.equals("restrict") ?
        //                 mainActivity.profileModel.getRestricted() ? "unrestrict/" : "restrict/" :
        //                 mainActivity.profileModel.getBlocked() ? "unblock/" : "block/"));
    }
}
