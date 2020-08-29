// package awais.instagrabber.activities;
//
// import android.content.Intent;
// import android.os.AsyncTask;
// import android.os.Bundle;
// import android.view.MenuItem;
// import android.view.View;
// import android.widget.Toast;
//
// import androidx.annotation.Nullable;
// import androidx.core.view.GestureDetectorCompat;
// import androidx.recyclerview.widget.LinearLayoutManager;
//
// import awais.instagrabber.R;
// import awais.instagrabber.adapters.StoriesAdapter;
// import awais.instagrabber.asyncs.SeenAction;
// import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
// import awais.instagrabber.customviews.helpers.SwipeGestureListener;
// import awais.instagrabber.databinding.ActivityStoryViewerBinding;
// import awais.instagrabber.interfaces.SwipeEvent;
// import awais.instagrabber.models.FeedStoryModel;
// import awais.instagrabber.models.StoryModel;
// import awais.instagrabber.models.stickers.PollModel;
// import awais.instagrabber.models.stickers.QuestionModel;
// import awais.instagrabber.models.stickers.QuizModel;
// import awais.instagrabber.utils.Constants;
// import awais.instagrabber.utils.Utils;
//
// import static awais.instagrabber.utils.Constants.MARK_AS_SEEN;
// import static awais.instagrabber.utils.Utils.settingsHelper;
//
// public final class StoryViewer extends BaseLanguageActivity {
//     private final StoriesAdapter storiesAdapter = new StoriesAdapter(null, new View.OnClickListener() {
//         @Override
//         public void onClick(final View v) {
//             final Object tag = v.getTag();
//             if (tag instanceof StoryModel) {
//                 currentStory = (StoryModel) tag;
//                 slidePos = currentStory.getPosition();
//                 refreshStory();
//             }
//         }
//     });
//     private ActivityStoryViewerBinding storyViewerBinding;
//     private StoryModel[] storyModels;
//     private GestureDetectorCompat gestureDetector;
//
//     private SwipeEvent swipeEvent;
//     private MenuItem menuDownload, menuDm;
//     private PollModel poll;
//     private QuestionModel question;
//     private String[] mentions;
//     private QuizModel quiz;
//     private StoryModel currentStory;
//     private String url, username;
//     private int slidePos = 0, lastSlidePos = 0;
//     private final String cookie = settingsHelper.getString(Constants.COOKIE);
//     private boolean fetching = false;
//
//     @Override
//     protected void onCreate(@Nullable final Bundle savedInstanceState) {
//         super.onCreate(savedInstanceState);
//         storyViewerBinding = ActivityStoryViewerBinding.inflate(getLayoutInflater());
//         setContentView(storyViewerBinding.getRoot());
//
//         setSupportActionBar(storyViewerBinding.toolbar.toolbar);
//
//         final Intent intent = getIntent();
//         if (intent == null || !intent.hasExtra(Constants.EXTRAS_STORIES)
//                 || (storyModels = (StoryModel[]) intent.getSerializableExtra(Constants.EXTRAS_STORIES)) == null) {
//             Utils.errorFinish(this);
//             return;
//         }
//
//         username = intent.getStringExtra(Constants.EXTRAS_USERNAME);
//         final String highlight = intent.getStringExtra(Constants.EXTRAS_HIGHLIGHT);
//         final boolean hasUsername = !Utils.isEmpty(username);
//         final boolean hasHighlight = !Utils.isEmpty(highlight);
//
//         if (hasUsername) {
//             username = username.replace("@", "");
//             storyViewerBinding.toolbar.toolbar.setTitle(username);
//             storyViewerBinding.toolbar.toolbar.setOnClickListener(v -> {
//                 searchUsername(username);
//             });
//             if (hasHighlight) storyViewerBinding.toolbar.toolbar.setSubtitle(getString(R.string.title_highlight, highlight));
//             else storyViewerBinding.toolbar.toolbar.setSubtitle(R.string.title_user_story);
//         }
//
//         storyViewerBinding.storiesList.setVisibility(View.GONE);
//         storyViewerBinding.storiesList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//         storyViewerBinding.storiesList.setAdapter(storiesAdapter);
//
//         swipeEvent = new SwipeEvent() {
//             private final int storiesLen = storyModels != null ? storyModels.length : 0;
//
//             @Override
//             public void onSwipe(final boolean isRightSwipe) {
//                 if (storyModels != null && storiesLen > 0) {
//                     if (((slidePos + 1 >= storiesLen && isRightSwipe == false) || (slidePos == 0 && isRightSwipe == true))
//                             && intent.hasExtra(Constants.FEED)) {
//                         final FeedStoryModel[] storyFeed = (FeedStoryModel[]) intent.getSerializableExtra(Constants.FEED);
//                         final int index = intent.getIntExtra(Constants.FEED_ORDER, 1738);
//                         if (settingsHelper.getBoolean(MARK_AS_SEEN)) new SeenAction(cookie, storyModel).execute();
//                         if ((isRightSwipe == true && index == 0) || (isRightSwipe == false && index == storyFeed.length - 1))
//                             Toast.makeText(getApplicationContext(), R.string.no_more_stories, Toast.LENGTH_SHORT).show();
//                         else {
//                             final FeedStoryModel feedStoryModel = isRightSwipe ?
//                                     (index == 0 ? null : storyFeed[index - 1]) :
//                                     (storyFeed.length == index + 1 ? null : storyFeed[index + 1]);
//                             if (feedStoryModel != null) {
//                                 if (fetching) {
//                                     Toast.makeText(getApplicationContext(), R.string.be_patient, Toast.LENGTH_SHORT).show();
//                                 } else {
//                                     fetching = true;
//                                     new iStoryStatusFetcher(feedStoryModel.getStoryMediaId(), null, false, false, false, false, result -> {
//                                         if (result != null && result.length > 0) {
//                                             final Intent newIntent = new Intent(getApplicationContext(), StoryViewer.class)
//                                                     .putExtra(Constants.EXTRAS_STORIES, result)
//                                                     .putExtra(Constants.EXTRAS_USERNAME, feedStoryModel.getProfileModel().getUsername())
//                                                     .putExtra(Constants.FEED, storyFeed)
//                                                     .putExtra(Constants.FEED_ORDER, isRightSwipe ? (index - 1) : (index + 1));
//                                             newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                             startActivity(newIntent);
//                                         } else
//                                             Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
//                                     }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                                 }
//                             }
//                         }
//                     }
//                     else {
//                         if (isRightSwipe) {
//                             if (--slidePos <= 0) slidePos = 0;
//                         } else if (++slidePos >= storiesLen) slidePos = storiesLen - 1;
//                         currentStory = storyModels[slidePos];
//                         refreshStory();
//                     }
//                 }
//             }
//         };
//         gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener(swipeEvent));
//
//         viewPost();
//     }
//
//     private void searchUsername(final String text) {
//         startActivity(
//                 new Intent(getApplicationContext(), ProfileViewer.class)
//                         .putExtra(Constants.EXTRAS_USERNAME, text)
//         );
//     }
//
//
//
//     public static int indexOfIntArray(Object[] array, Object key) {
//         int returnvalue = -1;
//         for (int i = 0; i < array.length; ++i) {
//             if (key == array[i]) {
//                 returnvalue = i;
//                 break;
//             }
//         }
//         return returnvalue;
//     }
//
// }