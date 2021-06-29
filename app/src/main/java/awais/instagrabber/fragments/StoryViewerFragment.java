package awais.instagrabber.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.StoriesAdapter;
import awais.instagrabber.customviews.helpers.SwipeGestureListener;
import awais.instagrabber.databinding.FragmentStoryViewerBinding;
import awais.instagrabber.fragments.main.ProfileFragmentDirections;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.interfaces.SwipeEvent;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.requests.StoryViewerOptions.Type;
import awais.instagrabber.repositories.requests.directmessages.ThreadIdsOrUserIds;
import awais.instagrabber.repositories.responses.stories.*;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.ArchivesViewModel;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.viewmodels.HighlightsViewModel;
import awais.instagrabber.viewmodels.StoriesViewModel;
import awais.instagrabber.webservices.DirectMessagesRepository;
import awais.instagrabber.webservices.MediaRepository;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesRepository;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_THRESHOLD;
import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD;
import static awais.instagrabber.fragments.settings.PreferenceKeys.MARK_AS_SEEN;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class StoryViewerFragment extends Fragment {
    private static final String TAG = "StoryViewerFragment";

    private final String cookie = settingsHelper.getString(Constants.COOKIE);

    private AppCompatActivity fragmentActivity;
    private View root;
    private FragmentStoryViewerBinding binding;
    private String currentStoryUsername;
    private String highlightTitle;
    private StoriesAdapter storiesAdapter;
    private SwipeEvent swipeEvent;
    private GestureDetectorCompat gestureDetector;
    private StoriesRepository storiesRepository;
    private MediaRepository mediaRepository;
    private StoryMedia currentStory;
    private Broadcast live;
    private int slidePos;
    private int lastSlidePos;
    private String url;
    private PollSticker poll;
    private QuestionSticker question;
    private List<String> mentions = new ArrayList<String>();
    private QuizSticker quiz;
    private SliderSticker slider;
    private MenuItem menuDownload, menuDm, menuProfile;
    private SimpleExoPlayer player;
    // private boolean isHashtag;
    // private boolean isLoc;
    // private String highlight;
    private String actionBarTitle, actionBarSubtitle;
    private boolean fetching = false, sticking = false, shouldRefresh = true;
    private boolean downloadVisible = false, dmVisible = false, profileVisible = true;
    private int currentFeedStoryIndex;
    private double sliderValue;
    private StoriesViewModel storiesViewModel;
    private ViewModel viewModel;
    // private boolean isHighlight;
    // private boolean isArchive;
    // private boolean isNotification;
    private DirectMessagesRepository directMessagesRepository;
    private StoryViewerOptions options;
    private String csrfToken;
    private String deviceId;
    private long userId;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (csrfToken == null) return;
        userId = CookieUtils.getUserIdFromCookie(cookie);
        deviceId = settingsHelper.getString(Constants.DEVICE_UUID);
        fragmentActivity = (AppCompatActivity) requireActivity();
        storiesRepository = StoriesRepository.Companion.getInstance();
        mediaRepository = MediaRepository.Companion.getInstance();
        directMessagesRepository = DirectMessagesRepository.Companion.getInstance();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentStoryViewerBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.story_menu, menu);
        menuDownload = menu.findItem(R.id.action_download);
        menuDm = menu.findItem(R.id.action_dms);
        menuProfile = menu.findItem(R.id.action_profile);
        menuDownload.setVisible(downloadVisible);
        menuDm.setVisible(dmVisible);
        menuProfile.setVisible(profileVisible);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {
        // hide menu items from activity
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final Context context = getContext();
        if (context == null) return false;
        int itemId = item.getItemId();
        if (itemId == R.id.action_download) {
            downloadStory();
            return true;
        }
        if (itemId == R.id.action_dms) {
            final EditText input = new EditText(context);
            input.setHint(R.string.reply_hint);
            final AlertDialog ad = new AlertDialog.Builder(context)
                    .setTitle(R.string.reply_story)
                    .setView(input)
                    .setPositiveButton(R.string.confirm, (d, w) -> directMessagesRepository.broadcastStoryReply(
                                        csrfToken,
                                        userId,
                                        deviceId,
                                        ThreadIdsOrUserIds.Companion.ofOneUser(String.valueOf(currentStory.getUser().getPk())),
                                        input.getText().toString(),
                                        currentStory.getId(),
                                        String.valueOf(currentStory.getUser().getPk()),
                                        CoroutineUtilsKt.getContinuation(
                                                (directThreadBroadcastResponse, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                    if (throwable1 != null) {
                                                        Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                                        Log.e(TAG, "onFailure: ", throwable1);
                                                        return;
                                                    }
                                                    Toast.makeText(context, R.string.answered_story, Toast.LENGTH_SHORT).show();
                                                }), Dispatchers.getIO()
                                        )
                    ))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
                }

                @Override
                public void afterTextChanged(final Editable s) {}
            });
            return true;
        }
        if (itemId == R.id.action_profile) {
            openProfile("@" + currentStory.getUser().getPk());
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(actionBarTitle);
            actionBar.setSubtitle(actionBarSubtitle);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        // reset subtitle
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
        super.onDestroy();
    }

    private void init() {
        if (getArguments() == null) return;
        final StoryViewerFragmentArgs fragmentArgs = StoryViewerFragmentArgs.fromBundle(getArguments());
        options = fragmentArgs.getOptions();
        currentFeedStoryIndex = options.getCurrentFeedStoryIndex();
        // highlight = fragmentArgs.getHighlight();
        // isHighlight = !TextUtils.isEmpty(highlight);
        // isArchive = fragmentArgs.getIsArchive();
        // isNotification = fragmentArgs.getIsNotification();
        final Type type = options.getType();
        if (currentFeedStoryIndex >= 0) {
            switch (type) {
                case HIGHLIGHT:
                    viewModel = new ViewModelProvider(fragmentActivity).get(HighlightsViewModel.class);
                    break;
                case STORY_ARCHIVE:
                    viewModel = new ViewModelProvider(fragmentActivity).get(ArchivesViewModel.class);
                    break;
                default:
                case FEED_STORY_POSITION:
                    viewModel = new ViewModelProvider(fragmentActivity).get(FeedStoriesViewModel.class);
                    break;
            }
        }
        setupStories();
    }

    private void setupStories() {
        storiesViewModel = new ViewModelProvider(this).get(StoriesViewModel.class);
        setupListeners();
        final Context context = getContext();
        if (context == null) return;
        binding.storiesList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        storiesAdapter = new StoriesAdapter((model, position) -> {
            currentStory = model;
            slidePos = position;
            refreshStory();
        });
        binding.storiesList.setAdapter(storiesAdapter);
        storiesViewModel.getList().observe(fragmentActivity, storiesAdapter::submitList);
        resetView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        final boolean hasFeedStories;
        List<?> models = null;
        if (currentFeedStoryIndex >= 0) {
            final Type type = options.getType();
            switch (type) {
                case HIGHLIGHT:
                    final HighlightsViewModel highlightsViewModel = (HighlightsViewModel) viewModel;
                    models = highlightsViewModel.getList().getValue();
                    break;
                case FEED_STORY_POSITION:
                    final FeedStoriesViewModel feedStoriesViewModel = (FeedStoriesViewModel) viewModel;
                    models = feedStoriesViewModel.getList().getValue();
                    break;
                case STORY_ARCHIVE:
                    final ArchivesViewModel archivesViewModel = (ArchivesViewModel) viewModel;
                    models = archivesViewModel.getList().getValue();
                    break;
            }
        }
        hasFeedStories = models != null && !models.isEmpty();
        final List<?> finalModels = models;
        final Context context = getContext();
        if (context == null) return;
        swipeEvent = isRightSwipe -> {
            final List<StoryMedia> storyModels = storiesViewModel.getList().getValue();
            final int storiesLen = storyModels == null ? 0 : storyModels.size();
            if (sticking) {
                Toast.makeText(context, R.string.follower_wait_to_load, Toast.LENGTH_SHORT).show();
                return;
            }
            if (storiesLen <= 0) return;
            final boolean isLeftSwipe = !isRightSwipe;
            final boolean endOfCurrentStories = slidePos + 1 >= storiesLen;
            final boolean swipingBeyondCurrentStories = (endOfCurrentStories && isLeftSwipe) || (slidePos == 0 && isRightSwipe);
            if (swipingBeyondCurrentStories && hasFeedStories) {
                final int index = currentFeedStoryIndex;
                if ((isRightSwipe && index == 0) || (isLeftSwipe && index == finalModels.size() - 1)) {
                    Toast.makeText(context, R.string.no_more_stories, Toast.LENGTH_SHORT).show();
                    return;
                }
                removeStickers();
                final Object feedStoryModel = isRightSwipe
                                              ? finalModels.get(index - 1)
                                              : finalModels.size() == index + 1 ? null : finalModels.get(index + 1);
                paginateStories(feedStoryModel, finalModels.get(index), context, isRightSwipe, currentFeedStoryIndex == finalModels.size() - 2);
                return;
            }
            removeStickers();
            if (isRightSwipe) {
                if (--slidePos <= 0) {
                    slidePos = 0;
                }
            } else if (++slidePos >= storiesLen) {
                slidePos = storiesLen - 1;
            }
            currentStory = storyModels.get(slidePos);
            refreshStory();
        };
        gestureDetector = new GestureDetectorCompat(context, new SwipeGestureListener(swipeEvent));
        binding.playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
                final float diffX = e2.getX() - e1.getX();
                try {
                    if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && Math.abs(diffX) > SWIPE_THRESHOLD
                            && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        swipeEvent.onSwipe(diffX > 0);
                        return true;
                    }
                } catch (final Exception e) {
                    // if (logCollector != null)
                    //     logCollector.appendException(e, LogCollector.LogFile.ACTIVITY_STORY_VIEWER, "setupListeners",
                    //                                  new Pair<>("swipeEvent", swipeEvent),
                    //                                  new Pair<>("diffX", diffX));
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error", e);
                }
                return false;
            }
        };

        if (hasFeedStories) {
            binding.btnBackward.setVisibility(currentFeedStoryIndex == 0 ? View.INVISIBLE : View.VISIBLE);
            binding.btnForward.setVisibility(currentFeedStoryIndex == finalModels.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            binding.btnBackward.setOnClickListener(v -> paginateStories(finalModels.get(currentFeedStoryIndex - 1),
                                                                        finalModels.get(currentFeedStoryIndex),
                                                                        context, true, false));
            binding.btnForward.setOnClickListener(v -> paginateStories(finalModels.get(currentFeedStoryIndex + 1),
                                                                       finalModels.get(currentFeedStoryIndex),
                                                                       context, false,
                                                                       currentFeedStoryIndex == finalModels.size() - 2));
        }

        binding.imageViewer.setTapListener(simpleOnGestureListener);
        binding.spotify.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) {
                Utils.openURL(context, tag.toString());
            }
        });
        binding.swipeUp.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.swipe_up_confirmation)
                        .setMessage(tag.toString()).setPositiveButton(R.string.yes, (d, w) -> Utils.openURL(context, tag.toString()))
                        .setNegativeButton(R.string.no, (d, w) -> d.dismiss()).show();
            }
        });
        binding.viewStoryPost.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (!(tag instanceof CharSequence)) return;
            final String mediaId = tag.toString();
            final AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setView(R.layout.dialog_opening_post)
                    .create();
            alertDialog.show();
            mediaRepository.fetch(
                    Long.parseLong(mediaId),
                    CoroutineUtilsKt.getContinuation((media, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            alertDialog.dismiss();
                            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final NavController navController = NavHostFragment.findNavController(StoryViewerFragment.this);
                        final Bundle bundle = new Bundle();
                        bundle.putSerializable(PostViewV2Fragment.ARG_MEDIA, media);
                        try {
                            navController.navigate(R.id.action_global_post_view, bundle);
                            alertDialog.dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "openPostDialog: ", e);
                        }
                    }), Dispatchers.getIO())
            );
        });
        final View.OnClickListener storyActionListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof PollSticker) {
                poll = (PollSticker) tag;
                final List<Tally> tallies = poll.getTallies();
                final String[] choices = tallies.stream()
                        .map(t -> (poll.getViewerVote() == tallies.indexOf(t) ? "√ " : "")
                                + t.getText() + " (" + t.getCount() + ")" )
                        .toArray(String[]::new);
                final ArrayAdapter adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, choices);
                if (poll.getViewerVote() > -1) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.voted_story_poll)
                            .setAdapter(adapter, null)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(poll.getQuestion())
                            .setAdapter(adapter, (d, w) -> {
                                sticking = true;
                                storiesRepository.respondToPoll(
                                        csrfToken,
                                        userId,
                                        deviceId,
                                        currentStory.getId().split("_")[0],
                                        poll.getPollId(),
                                        w,
                                        CoroutineUtilsKt.getContinuation(
                                                (storyStickerResponse, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                    if (throwable != null) {
                                                        sticking = false;
                                                        Log.e(TAG, "Error responding", throwable);
                                                        try {
                                                            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                                        } catch (Exception ignored) {}
                                                        return;
                                                    }
                                                    sticking = false;
                                                    try {
                                                        poll.setViewerVote(w);
                                                        Toast.makeText(context, R.string.votef_story_poll, Toast.LENGTH_SHORT).show();
                                                    } catch (Exception ignored) {}
                                                }),
                                                Dispatchers.getIO()
                                        )
                                );
                            })
                            .setPositiveButton(R.string.cancel, null)
                            .show();
                }
            } else if (tag instanceof QuestionSticker) {
                question = (QuestionSticker) tag;
                final EditText input = new EditText(context);
                input.setHint(R.string.answer_hint);
                final AlertDialog ad = new AlertDialog.Builder(context)
                        .setTitle(question.getQuestion())
                        .setView(input)
                        .setPositiveButton(R.string.confirm, (d, w) -> {
                            sticking = true;
                            storiesRepository.respondToQuestion(
                                    csrfToken,
                                    userId,
                                    deviceId,
                                    currentStory.getId().split("_")[0],
                                    question.getQuestionId(),
                                    input.getText().toString(),
                                    CoroutineUtilsKt.getContinuation(
                                            (storyStickerResponse, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                if (throwable != null) {
                                                    sticking = false;
                                                    Log.e(TAG, "Error responding", throwable);
                                                    try {
                                                        Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                                    } catch (Exception ignored) {}
                                                    return;
                                                }
                                                sticking = false;
                                                try {
                                                    Toast.makeText(context, R.string.answered_story, Toast.LENGTH_SHORT).show();
                                                } catch (Exception ignored) {}
                                            }),
                                            Dispatchers.getIO()
                                    )
                            );
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

                    @Override
                    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                        ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s));
                    }

                    @Override
                    public void afterTextChanged(final Editable s) {}
                });
            } else if (tag instanceof String[]) {
                final String[] rawMentions = (String[]) tag;
                mentions = new ArrayList<String>(Arrays.asList(rawMentions));
                new AlertDialog.Builder(context)
                        .setTitle(R.string.story_mentions)
                        .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, rawMentions), (d, w) -> openProfile(mentions.get(w)))
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            } else if (tag instanceof QuizSticker) {
                final List<Tally> tallies = quiz.getTallies();
                final String[] choices = tallies.stream().map(
                        t -> (quiz.getViewerAnswer() == tallies.indexOf(t) ? "√ " : "") +
                                (quiz.getCorrectAnswer() == tallies.indexOf(t) ? "*** " : "") +
                                t.getText() + " (" + t.getCount() + ")"
                ).toArray(String[]::new);
                new AlertDialog.Builder(context)
                        .setTitle(quiz.getViewerAnswer() > -1 ? getString(R.string.story_quizzed) : quiz.getQuestion())
                        .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, choices), (d, w) -> {
                            if (quiz.getViewerAnswer() == -1) {
                                sticking = true;
                                storiesRepository.respondToQuiz(
                                        csrfToken,
                                        userId,
                                        deviceId,
                                        currentStory.getId().split("_")[0],
                                        quiz.getQuizId(),
                                        w,
                                        CoroutineUtilsKt.getContinuation(
                                                (storyStickerResponse, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                    if (throwable != null) {
                                                        sticking = false;
                                                        Log.e(TAG, "Error responding", throwable);
                                                        try {
                                                            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                                        } catch (Exception ignored) {}
                                                        return;
                                                    }
                                                    sticking = false;
                                                    try {
                                                        quiz.setViewerAnswer(w);
                                                        Toast.makeText(context, R.string.answered_story, Toast.LENGTH_SHORT).show();
                                                    } catch (Exception ignored) {}
                                                }),
                                                Dispatchers.getIO()
                                        )
                                );
                            }
                        })
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            } else if (tag instanceof SliderSticker) {
                slider = (SliderSticker) tag;
                NumberFormat percentage = NumberFormat.getPercentInstance();
                percentage.setMaximumFractionDigits(2);
                LinearLayout sliderView = new LinearLayout(context);
                sliderView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                sliderView.setOrientation(LinearLayout.VERTICAL);
                TextView tv = new TextView(context);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                final SeekBar input = new SeekBar(context);
                double avg = slider.getSliderVoteAverage() * 100;
                input.setProgress((int) avg);
                sliderView.addView(input);
                sliderView.addView(tv);
                if (slider.getViewerVote().isNaN() && slider.getViewerCanVote()) {
                    input.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            sliderValue = progress / 100.0;
                            tv.setText(percentage.format(sliderValue));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    new AlertDialog.Builder(context)
                            .setTitle(TextUtils.isEmpty(slider.getQuestion()) ? slider.getEmoji() : slider.getQuestion())
                            .setMessage(getResources().getQuantityString(R.plurals.slider_info,
                                                                         slider.getSliderVoteCount(),
                                                                         slider.getSliderVoteCount(),
                                                                         percentage.format(slider.getSliderVoteAverage())))
                            .setView(sliderView)
                            .setPositiveButton(R.string.confirm, (d, w) -> {
                                sticking = true;
                                storiesRepository.respondToSlider(
                                        csrfToken,
                                        userId,
                                        deviceId,
                                        currentStory.getId().split("_")[0],
                                        slider.getSliderId(),
                                        sliderValue,
                                        CoroutineUtilsKt.getContinuation(
                                                (storyStickerResponse, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                    if (throwable != null) {
                                                        sticking = false;
                                                        Log.e(TAG, "Error responding", throwable);
                                                        try {
                                                            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                                        } catch (Exception ignored) {}
                                                        return;
                                                    }
                                                    sticking = false;
                                                    try {
                                                        slider.setViewerVote(sliderValue);
                                                        Toast.makeText(context, R.string.answered_story, Toast.LENGTH_SHORT).show();
                                                    } catch (Exception ignored) {}
                                                }), Dispatchers.getIO()
                                        )
                                );
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    input.setEnabled(false);
                    tv.setText(getString(R.string.slider_answer, percentage.format(slider.getViewerVote())));
                    new AlertDialog.Builder(context)
                            .setTitle(TextUtils.isEmpty(slider.getQuestion()) ? slider.getEmoji() : slider.getQuestion())
                            .setMessage(getResources().getQuantityString(R.plurals.slider_info,
                                                                         slider.getSliderVoteCount(),
                                                                         slider.getSliderVoteCount(),
                                                                         percentage.format(slider.getSliderVoteAverage())))
                            .setView(sliderView)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                }
            }
        };
        binding.poll.setOnClickListener(storyActionListener);
        binding.answer.setOnClickListener(storyActionListener);
        binding.mention.setOnClickListener(storyActionListener);
        binding.quiz.setOnClickListener(storyActionListener);
        binding.slider.setOnClickListener(storyActionListener);
    }

    private void resetView() {
        final Context context = getContext();
        if (context == null) return;
        live = null;
        slidePos = 0;
        lastSlidePos = 0;
        if (menuDownload != null) menuDownload.setVisible(false);
        if (menuDm != null) menuDm.setVisible(false);
        if (menuProfile != null) menuProfile.setVisible(false);
        downloadVisible = false;
        dmVisible = false;
        profileVisible = false;
        binding.imageViewer.setController(null);
        releasePlayer();
        String currentStoryMediaId = null;
        final Type type = options.getType();
        StoryViewerOptions fetchOptions = null;
        switch (type) {
            case HIGHLIGHT: {
                final HighlightsViewModel highlightsViewModel = (HighlightsViewModel) viewModel;
                final List<Story> models = highlightsViewModel.getList().getValue();
                if (models == null || models.isEmpty() || currentFeedStoryIndex >= models.size() || currentFeedStoryIndex < 0) {
                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                final Story model = models.get(currentFeedStoryIndex);
                currentStoryMediaId = model.getId();
                fetchOptions = StoryViewerOptions.forHighlight(model.getId());
                highlightTitle = model.getTitle();
                break;
            }
            case FEED_STORY_POSITION: {
                final FeedStoriesViewModel feedStoriesViewModel = (FeedStoriesViewModel) viewModel;
                final List<Story> models = feedStoriesViewModel.getList().getValue();
                if (models == null || currentFeedStoryIndex >= models.size() || currentFeedStoryIndex < 0)
                    return;
                final Story model = models.get(currentFeedStoryIndex);
                currentStoryMediaId = String.valueOf(model.getUser().getPk());
                currentStoryUsername = model.getUser().getUsername();
                fetchOptions = StoryViewerOptions.forUser(model.getUser().getPk(), currentStoryUsername);
                live = model.getBroadcast();
                break;
            }
            case STORY_ARCHIVE: {
                final ArchivesViewModel archivesViewModel = (ArchivesViewModel) viewModel;
                final List<Story> models = archivesViewModel.getList().getValue();
                if (models == null || models.isEmpty() || currentFeedStoryIndex >= models.size() || currentFeedStoryIndex < 0) {
                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                final Story model = models.get(currentFeedStoryIndex);
                currentStoryMediaId = parseStoryMediaId(model.getId());
                currentStoryUsername = model.getTitle();
                fetchOptions = StoryViewerOptions.forStoryArchive(model.getId());
                break;
            }
            case USER: {
                currentStoryMediaId = String.valueOf(options.getId());
                currentStoryUsername = options.getName();
                fetchOptions = StoryViewerOptions.forUser(options.getId(), currentStoryUsername);
                break;
            }
        }
        setTitle(type);
        storiesViewModel.getList().setValue(Collections.emptyList());
        if (type == Type.STORY) {
            storiesRepository.fetch(
                    options.getId(),
                    CoroutineUtilsKt.getContinuation((storyModel, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error", throwable);
                            return;
                        }
                        fetching = false;
                        binding.storiesList.setVisibility(View.GONE);
                        if (storyModel == null) {
                            storiesViewModel.getList().setValue(Collections.emptyList());
                            currentStory = null;
                            return;
                        }
                        storiesViewModel.getList().setValue(Collections.singletonList(storyModel));
                        currentStory = storyModel;
                        refreshStory();
                    }), Dispatchers.getIO())
            );
            return;
        }
        if (currentStoryMediaId == null) return;
        if (live != null) {
            currentStory = null;
            refreshLive();
            return;
        }
        final ServiceCallback<List<StoryMedia>> storyCallback = new ServiceCallback<List<StoryMedia>>() {
            @Override
            public void onSuccess(final List<StoryMedia> storyModels) {
                fetching = false;
                if (storyModels == null || storyModels.isEmpty()) {
                    storiesViewModel.getList().setValue(Collections.emptyList());
                    currentStory = null;
                    binding.storiesList.setVisibility(View.GONE);
                    return;
                }
                binding.storiesList.setVisibility((storyModels.size() == 1 && currentFeedStoryIndex == -1) ? View.GONE : View.VISIBLE);
                if (currentFeedStoryIndex == -1) {
                    binding.btnBackward.setVisibility(View.GONE);
                    binding.btnForward.setVisibility(View.GONE);
                }
                storiesViewModel.getList().setValue(storyModels);
                currentStory = storyModels.get(0);
                refreshStory();
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error", t);
            }
        };
        storiesRepository.getStories(
                fetchOptions,
                CoroutineUtilsKt.getContinuation((storyModels, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        storyCallback.onFailure(throwable);
                        return;
                    }
                    //noinspection unchecked
                    storyCallback.onSuccess((List<StoryMedia>) storyModels);
                }), Dispatchers.getIO())
        );
    }

    private void setTitle(final Type type) {
        final boolean hasUsername = !TextUtils.isEmpty(currentStoryUsername);
        if (type == Type.HIGHLIGHT) {
            final ActionBar actionBar = fragmentActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBarTitle = highlightTitle;
                actionBar.setTitle(highlightTitle);
            }
        } else if (hasUsername) {
            currentStoryUsername = currentStoryUsername.replace("@", "");
            final ActionBar actionBar = fragmentActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBarTitle = currentStoryUsername;
                actionBar.setTitle(currentStoryUsername);
            }
        }
    }

    private synchronized void refreshLive() {
        binding.storiesList.setVisibility(View.INVISIBLE);
        binding.viewStoryPost.setVisibility(View.GONE);
        binding.spotify.setVisibility(View.GONE);
        binding.poll.setVisibility(View.GONE);
        binding.answer.setVisibility(View.GONE);
        binding.mention.setVisibility(View.GONE);
        binding.quiz.setVisibility(View.GONE);
        binding.slider.setVisibility(View.GONE);
        lastSlidePos = slidePos;
        releasePlayer();
        url = live.getDashPlaybackUrl();
        setupLive();
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        actionBarSubtitle = TextUtils.epochSecondToString(live.getPublishedTime());
        if (actionBar != null) {
            try {
                actionBar.setSubtitle(actionBarSubtitle);
            } catch (Exception e) {
                Log.e(TAG, "refreshLive: ", e);
            }
        }
    }

    private synchronized void refreshStory() {
        if (binding.storiesList.getVisibility() == View.VISIBLE) {
            final List<StoryMedia> storyModels = storiesViewModel.getList().getValue();
            if (storyModels != null && storyModels.size() > 0) {
                StoryMedia item = storyModels.get(lastSlidePos);
                if (item != null) {
                    item.setCurrentSlide(false);
                    storiesAdapter.notifyItemChanged(lastSlidePos, item);
                }
                item = storyModels.get(slidePos);
                if (item != null) {
                    item.setCurrentSlide(true);
                    storiesAdapter.notifyItemChanged(slidePos, item);
                }
            }
        }
        lastSlidePos = slidePos;

        final MediaItemType itemType = currentStory.getMediaType();

        url = itemType == MediaItemType.MEDIA_TYPE_IMAGE
                ? ResponseBodyUtils.getImageUrl(currentStory)
                : ResponseBodyUtils.getVideoUrl(currentStory);

        if (currentStory.getStoryFeedMedia() != null) {
            final String shortCode = currentStory.getStoryFeedMedia().get(0).getMediaId();
            binding.viewStoryPost.setVisibility(View.VISIBLE);
            binding.viewStoryPost.setTag(shortCode);
        }

        final StoryAppAttribution spotify = currentStory.getStoryAppAttribution();
        if (spotify != null) {
            binding.spotify.setVisibility(View.VISIBLE);
            binding.spotify.setText(spotify.getName());
            binding.spotify.setTag(spotify.getContentUrl().split("?")[0]);
        }

        if (currentStory.getStoryPolls() != null) {
            poll = currentStory.getStoryPolls().get(0).getPollSticker();
            binding.poll.setVisibility(View.VISIBLE);
            binding.poll.setTag(poll);
        }

        if (currentStory.getStoryQuestions() != null) {
            question = currentStory.getStoryQuestions().get(0).getQuestionSticker();
            binding.answer.setVisibility(View.VISIBLE);
            binding.answer.setTag(question);
        }

        mentions.clear();
        if (currentStory.getReelMentions() != null) {
            mentions.addAll(currentStory.getReelMentions().stream().map(
                    s -> s.getUser().getUsername()
            ).distinct().collect(Collectors.toList()));
        }
        if (currentStory.getStoryHashtags() != null) {
            mentions.addAll(currentStory.getStoryHashtags().stream().map(
                    s -> s.getHashtag().getName()
            ).distinct().collect(Collectors.toList()));
        }
        if (currentStory.getStoryLocations() != null) {
            mentions.addAll(currentStory.getStoryLocations().stream().map(
                    s -> s.getLocation().getShortName() + " (" + s.getLocation().getPk() + ")"
            ).distinct().collect(Collectors.toList()));
        }
        if (mentions.size() > 0) {
            binding.mention.setVisibility(View.VISIBLE);
            binding.mention.setTag(mentions.stream().toArray(String[]::new));
        }

        if (currentStory.getStoryQuizs() != null) {
            quiz = currentStory.getStoryQuizs().get(0).getQuizSticker();
            binding.quiz.setVisibility(View.VISIBLE);
            binding.quiz.setTag(quiz);
        }

        if (currentStory.getStorySliders() != null) {
            slider = currentStory.getStorySliders().get(0).getSliderSticker();
            binding.slider.setVisibility(View.VISIBLE);
            binding.slider.setTag(slider);
        }

        if (currentStory.getStoryCta() != null) {
            final StoryCta swipeUp = currentStory.getStoryCta().get(0).getLinks();
            binding.swipeUp.setVisibility(View.VISIBLE);
            binding.swipeUp.setText(currentStory.getLinkText());
            final String swipeUpUrl = swipeUp.getWebUri();
            final String actualLink = swipeUpUrl.startsWith("https://l.instagram.com/")
                ? Uri.parse(swipeUpUrl).getQueryParameter("u")
                : null;
            binding.swipeUp.setTag(actualLink == null && actualLink.startsWith("http")
                    ? swipeUpUrl : actualLink);
        }

        releasePlayer();
        final Type type = options.getType();
        if (type == Type.HASHTAG || type == Type.LOCATION) {
            final ActionBar actionBar = fragmentActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBarTitle = currentStory.getUser().getUsername();
                actionBar.setTitle(currentStory.getUser().getUsername());
            }
        }
        if (itemType == MediaItemType.MEDIA_TYPE_VIDEO) setupVideo();
        else setupImage();

        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        actionBarSubtitle = TextUtils.epochSecondToString(currentStory.getTakenAt());
        if (actionBar != null) {
            try {
                actionBar.setSubtitle(actionBarSubtitle);
            } catch (Exception e) {
                Log.e(TAG, "refreshStory: ", e);
            }
        }

        if (settingsHelper.getBoolean(MARK_AS_SEEN))
            storiesRepository.seen(
                    csrfToken,
                    userId,
                    deviceId,
                    currentStory.getId(),
                    currentStory.getTakenAt(),
                    System.currentTimeMillis() / 1000,
                    CoroutineUtilsKt.getContinuation((s, throwable) -> {}, Dispatchers.getIO())
            );
    }

    private void removeStickers() {
        binding.swipeUp.setVisibility(View.GONE);
        binding.quiz.setVisibility(View.GONE);
        binding.spotify.setVisibility(View.GONE);
        binding.mention.setVisibility(View.GONE);
        binding.viewStoryPost.setVisibility(View.GONE);
        binding.answer.setVisibility(View.GONE);
        binding.slider.setVisibility(View.GONE);
    }

    private void downloadStory() {
        final Context context = getContext();
        if (context == null) return;
        if (currentStory == null) {
            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadUtils.download(context, currentStory);
    }

    private void setupImage() {
        binding.progressView.setVisibility(View.VISIBLE);
        binding.playerView.setVisibility(View.GONE);
        binding.imageViewer.setVisibility(View.VISIBLE);
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .setProgressiveRenderingEnabled(true)
                                                               .build();
        final DraweeController controller = Fresco.newDraweeControllerBuilder()
                                                  .setImageRequest(requestBuilder)
                                                  .setOldController(binding.imageViewer.getController())
                                                  .setControllerListener(new BaseControllerListener<ImageInfo>() {

                                                      @Override
                                                      public void onFailure(final String id, final Throwable throwable) {
                                                          binding.progressView.setVisibility(View.GONE);
                                                      }

                                                      @Override
                                                      public void onFinalImageSet(final String id,
                                                                                  final ImageInfo imageInfo,
                                                                                  final Animatable animatable) {
                                                          if (menuDownload != null) {
                                                              downloadVisible = true;
                                                              menuDownload.setVisible(true);
                                                          }
                                                          if (currentStory.getCanReply() && menuDm != null) {
                                                              dmVisible = true;
                                                              menuDm.setVisible(true);
                                                          }
                                                          if (!TextUtils.isEmpty(currentStory.getUser().getUsername())) {
                                                              profileVisible = true;
                                                              menuProfile.setVisible(true);
                                                          }
                                                          binding.progressView.setVisibility(View.GONE);
                                                      }
                                                  })
                                                  .build();
        binding.imageViewer.setController(controller);
    }

    private void setupVideo() {
        binding.playerView.setVisibility(View.VISIBLE);
        binding.progressView.setVisibility(View.GONE);
        binding.imageViewer.setVisibility(View.GONE);
        binding.imageViewer.setController(null);

        final Context context = getContext();
        if (context == null) return;
        player = new SimpleExoPlayer.Builder(context).build();
        binding.playerView.setPlayer(player);
        player.setPlayWhenReady(settingsHelper.getBoolean(PreferenceKeys.AUTOPLAY_VIDEOS_STORIES));

        final Uri uri = Uri.parse(url);
        final MediaItem mediaItem = MediaItem.fromUri(uri);
        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(context, "instagram"))
                .createMediaSource(mediaItem);
        mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            @Override
            public void onLoadCompleted(final int windowIndex,
                                        @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                        @NonNull final LoadEventInfo loadEventInfo,
                                        @NonNull final MediaLoadData mediaLoadData) {
                if (menuDownload != null) {
                    downloadVisible = true;
                    menuDownload.setVisible(true);
                }
                if (currentStory.getCanReply() && menuDm != null) {
                    dmVisible = true;
                    menuDm.setVisible(true);
                }
                if (!TextUtils.isEmpty(currentStory.getUser().getUsername()) && menuProfile != null) {
                    profileVisible = true;
                    menuProfile.setVisible(true);
                }
                binding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadStarted(final int windowIndex,
                                      @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                      @NonNull final LoadEventInfo loadEventInfo,
                                      @NonNull final MediaLoadData mediaLoadData) {
                if (menuDownload != null) {
                    downloadVisible = true;
                    menuDownload.setVisible(true);
                }
                if (currentStory.getCanReply() && menuDm != null) {
                    dmVisible = true;
                    menuDm.setVisible(true);
                }
                if (!TextUtils.isEmpty(currentStory.getUser().getUsername()) && menuProfile != null) {
                    profileVisible = true;
                    menuProfile.setVisible(true);
                }
                binding.progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadCanceled(final int windowIndex,
                                       @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                       @NonNull final LoadEventInfo loadEventInfo,
                                       @NonNull final MediaLoadData mediaLoadData) {
                binding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(final int windowIndex,
                                    @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                    @NonNull final LoadEventInfo loadEventInfo,
                                    @NonNull final MediaLoadData mediaLoadData,
                                    @NonNull final IOException error,
                                    final boolean wasCanceled) {
                if (menuDownload != null) {
                    downloadVisible = false;
                    menuDownload.setVisible(false);
                }
                if (menuDm != null) {
                    dmVisible = false;
                    menuDm.setVisible(false);
                }
                if (menuProfile != null) {
                    profileVisible = false;
                    menuProfile.setVisible(false);
                }
                binding.progressView.setVisibility(View.GONE);
            }
        });
        player.setMediaSource(mediaSource);
        player.prepare();

        binding.playerView.setOnClickListener(v -> {
            if (player != null) {
                if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                player.setPlayWhenReady(player.getPlaybackState() == Player.STATE_ENDED || !player.isPlaying());
            }
        });
    }

    private void setupLive() {
        binding.playerView.setVisibility(View.VISIBLE);
        binding.progressView.setVisibility(View.GONE);
        binding.imageViewer.setVisibility(View.GONE);
        binding.imageViewer.setController(null);

        if (menuDownload != null) menuDownload.setVisible(false);
        if (menuDm != null) menuDm.setVisible(false);

        final Context context = getContext();
        if (context == null) return;
        player = new SimpleExoPlayer.Builder(context).build();
        binding.playerView.setPlayer(player);
        player.setPlayWhenReady(settingsHelper.getBoolean(PreferenceKeys.AUTOPLAY_VIDEOS_STORIES));

        final Uri uri = Uri.parse(url);
        final MediaItem mediaItem = MediaItem.fromUri(uri);
        final DashMediaSource mediaSource = new DashMediaSource.Factory(new DefaultDataSourceFactory(context, "instagram"))
                .createMediaSource(mediaItem);
        mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            @Override
            public void onLoadCompleted(final int windowIndex,
                                        @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                        @NonNull final LoadEventInfo loadEventInfo,
                                        @NonNull final MediaLoadData mediaLoadData) {
                binding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadStarted(final int windowIndex,
                                      @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                      @NonNull final LoadEventInfo loadEventInfo,
                                      @NonNull final MediaLoadData mediaLoadData) {
                binding.progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadCanceled(final int windowIndex,
                                       @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                       @NonNull final LoadEventInfo loadEventInfo,
                                       @NonNull final MediaLoadData mediaLoadData) {
                binding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(final int windowIndex,
                                    @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                    @NonNull final LoadEventInfo loadEventInfo,
                                    @NonNull final MediaLoadData mediaLoadData,
                                    @NonNull final IOException error,
                                    final boolean wasCanceled) {
                binding.progressView.setVisibility(View.GONE);
            }
        });
        player.setMediaSource(mediaSource);
        player.prepare();

        binding.playerView.setOnClickListener(v -> {
            if (player != null) {
                if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                player.setPlayWhenReady(player.getPlaybackState() == Player.STATE_ENDED || !player.isPlaying());
            }
        });
    }

    private void openProfile(final String username) {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
        final char t = username.charAt(0);
        if (t == '@') {
            final NavDirections action = HashTagFragmentDirections.actionGlobalProfileFragment(username);
            NavHostFragment.findNavController(this).navigate(action);
        } else if (t == '#') {
            final NavDirections action = HashTagFragmentDirections.actionGlobalHashTagFragment(username.substring(1));
            NavHostFragment.findNavController(this).navigate(action);
        } else {
            final NavDirections action = ProfileFragmentDirections
                    .actionGlobalLocationFragment(Long.parseLong(username.split(" \\(")[1].replace(")", "")));
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    private void releasePlayer() {
        if (player == null) return;
        try { player.stop(true); } catch (Exception ignored) { }
        try { player.release(); } catch (Exception ignored) { }
        player = null;
    }

    private void paginateStories(Object newFeedStory, Object oldFeedStory, Context context, boolean backward, boolean last) {
        if (newFeedStory != null) {
            if (fetching) {
                Toast.makeText(context, R.string.be_patient, Toast.LENGTH_SHORT).show();
                return;
            }
            if (settingsHelper.getBoolean(MARK_AS_SEEN)
                    && oldFeedStory instanceof Story
                    && viewModel instanceof FeedStoriesViewModel) {
                final FeedStoriesViewModel feedStoriesViewModel = (FeedStoriesViewModel) viewModel;
                final Story oldFeedStoryModel = (Story) oldFeedStory;
                if (oldFeedStoryModel.getSeen() == null || !oldFeedStoryModel.getSeen().equals(oldFeedStoryModel.getLatestReelMedia())) {
                    oldFeedStoryModel.setSeen(oldFeedStoryModel.getLatestReelMedia());
                    final List<Story> models = feedStoriesViewModel.getList().getValue();
                    final List<Story> modelsCopy = models == null ? new ArrayList<>() : new ArrayList<>(models);
                    modelsCopy.set(currentFeedStoryIndex, oldFeedStoryModel);
                    feedStoriesViewModel.getList().postValue(modelsCopy);
                }
            }
            fetching = true;
            binding.btnBackward.setVisibility(currentFeedStoryIndex == 1 && backward ? View.INVISIBLE : View.VISIBLE);
            binding.btnForward.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            currentFeedStoryIndex = backward ? (currentFeedStoryIndex - 1) : (currentFeedStoryIndex + 1);
            resetView();
        }
    }

    /**
     * Parses the Story's media ID. For user stories this is a number, but for archive stories
     * this is "archiveDay:" plus a number.
     */
    private static String parseStoryMediaId(String rawId) {
        final String regex = "(?:archiveDay:)?(.+)";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(rawId);

        if (matcher.matches() && matcher.groupCount() >= 1) {
            return matcher.group(1);
        }

        return rawId;
    }
}
