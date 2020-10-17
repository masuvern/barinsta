package awais.instagrabber.adapters.viewholder.feed;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.databinding.ItemFeedSliderBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedSliderViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedSliderViewHolder";
    private static final boolean shouldAutoPlay = settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS);

    private final ItemFeedSliderBinding binding;
    private final DefaultDataSourceFactory dataSourceFactory;

    private final PlayerChangeListener playerChangeListener = (position, player) -> {
        pagerPlayer = player;
        playerPosition = position;
    };

    private CacheDataSourceFactory cacheDataSourceFactory;
    private SimpleExoPlayer pagerPlayer;
    private int playerPosition = 0;

    public FeedSliderViewHolder(@NonNull final ItemFeedSliderBinding binding,
                                final MentionClickListener mentionClickListener,
                                final View.OnClickListener clickListener,
                                final View.OnLongClickListener longClickListener) {
        super(binding.getRoot(), binding.itemFeedTop, binding.itemFeedBottom, mentionClickListener, clickListener, longClickListener);
        this.binding = binding;
        binding.itemFeedBottom.videoViewsContainer.setVisibility(View.GONE);
        binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
        final ViewGroup.LayoutParams layoutParams = binding.mediaList.getLayoutParams();
        layoutParams.height = Utils.displayMetrics.widthPixels + 1;
        binding.mediaList.setLayoutParams(layoutParams);
        final Context context = binding.getRoot().getContext();
        dataSourceFactory = new DefaultDataSourceFactory(context, "instagram");
        final SimpleCache simpleCache = Utils.getSimpleCacheInstance(context);
        if (simpleCache != null) {
            cacheDataSourceFactory = new CacheDataSourceFactory(simpleCache, dataSourceFactory);
        }
    }

    @Override
    public void bindItem(final FeedModel feedModel,
                         final FeedAdapterV2.OnPostClickListener postClickListener) {
        final List<PostChild> sliderItems = feedModel.getSliderItems();
        final int sliderItemLen = sliderItems != null ? sliderItems.size() : 0;
        if (sliderItemLen <= 0) return;
        final String text = "1/" + sliderItemLen;
        binding.mediaCounter.setText(text);
        binding.mediaList.setOffscreenPageLimit(1);
        SliderItemsAdapter adapter = (SliderItemsAdapter) binding.mediaList.getAdapter();
        if (adapter == null) {
            adapter = new SliderItemsAdapter();
        }
        // adapter.setSpanCount(spanCount);
        binding.mediaList.setAdapter(adapter);
        binding.mediaList.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                if (position >= sliderItemLen) return;
                setDimensions(binding.mediaList, sliderItems.get(position));
            }
        });
        setDimensions(binding.mediaList, sliderItems.get(0));


        //noinspection deprecation
        // binding.mediaList.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        //     private int prevPos = 0;
        //
        //     @Override
        //     public void onPageSelected(final int position) {
        //         ViewerPostModel sliderItem = sliderItems.get(prevPos);
        //         if (sliderItem != null) {
        //             sliderItem.setSelected(false);
        //             if (sliderItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
        //                 // stop playing prev video
        //                 final ViewSwitcher prevChild = (ViewSwitcher) binding.mediaList.getChildAt(prevPos);
        //                 if (prevChild == null || prevChild.getTag() == null || !(prevChild.getTag() instanceof SimpleExoPlayer)) {
        //                     return;
        //                 }
        //                 ((SimpleExoPlayer) prevChild.getTag()).setPlayWhenReady(false);
        //             }
        //         }
        //         sliderItem = sliderItems.get(position);
        //         if (sliderItem == null) return;
        //         sliderItem.setSelected(true);
        //         final String text = (position + 1) + "/" + sliderItemLen;
        //         binding.mediaCounter.setText(text);
        //         prevPos = position;
        //         if (sliderItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
        //             binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
        //             if (shouldAutoPlay) {
        //                 autoPlay(position);
        //             }
        //         } else binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
        //     }
        // });
        adapter.submitList(sliderItems);
        final View.OnClickListener muteClickListener = v -> {
            final int currentItem = binding.mediaList.getCurrentItem();
            if (currentItem < 0 || currentItem >= binding.mediaList.getChildCount()) {
                return;
            }
            final PostChild sliderItem = sliderItems.get(currentItem);
            if (sliderItem.getItemType() != MediaItemType.MEDIA_TYPE_VIDEO) {
                return;
            }
            final View currentView = binding.mediaList.getChildAt(currentItem);
            if (!(currentView instanceof ViewSwitcher)) {
                return;
            }
            final ViewSwitcher viewSwitcher = (ViewSwitcher) currentView;
            final Object tag = viewSwitcher.getTag();
            if (!(tag instanceof SimpleExoPlayer)) {
                return;
            }
            final SimpleExoPlayer player = (SimpleExoPlayer) tag;
            final float intVol = player.getVolume() == 0f ? 1f : 0f;
            player.setVolume(intVol);
            binding.itemFeedBottom.btnMute.setImageResource(intVol == 0f ? R.drawable.ic_volume_up_24 : R.drawable.ic_volume_off_24);
            Utils.sessionVolumeFull = intVol == 1f;
        };
        final PostChild firstItem = sliderItems.get(0);
        if (firstItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
            binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
        }
        binding.itemFeedBottom.btnMute.setImageResource(Utils.sessionVolumeFull ? R.drawable.ic_volume_off_24 : R.drawable.ic_volume_up_24);
        binding.itemFeedBottom.btnMute.setOnClickListener(muteClickListener);
    }

    private void setDimensions(final View view, final PostChild model) {
        final ViewGroup.LayoutParams layoutParams = binding.mediaList.getLayoutParams();
        int requiredWidth = layoutParams.width;
        if (requiredWidth <= 0) {
            final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    setLayoutParamDimens(binding.mediaList, model);
                    return true;
                }
            };
            view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            return;
        }
        setLayoutParamDimens(binding.mediaList, model);
    }

    private void setLayoutParamDimens(final View view, final PostChild model) {
        final int requiredWidth = view.getMeasuredWidth();
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        final int spanHeight = NumberUtils.getResultingHeight(requiredWidth, model.getHeight(), model.getWidth());
        layoutParams.height = spanHeight == 0 ? requiredWidth + 1 : spanHeight;
        view.requestLayout();
    }

    private void autoPlay(final int position) {
        // if (!shouldAutoPlay) {
        //     return;
        // }
        // final ChildMediaItemsAdapter adapter = (ChildMediaItemsAdapter) binding.mediaList.getAdapter();
        // if (adapter == null) {
        //     return;
        // }
        // final ViewerPostModel sliderItem = adapter.getItemAtPosition(position);
        // if (sliderItem.getItemType() != MediaItemType.MEDIA_TYPE_VIDEO) {
        //     return;
        // }
        // final ViewSwitcher viewSwitcher = (ViewSwitcher) binding.mediaList.getChildAt(position);
        // loadPlayer(binding.getRoot().getContext(),
        //            position, sliderItem.getDisplayUrl(),
        //            viewSwitcher,
        //            cacheDataSourceFactory != null ? cacheDataSourceFactory : dataSourceFactory,
        //            playerChangeListener);
    }

    public void startPlayingVideo() {
        autoPlay(playerPosition);
    }

    public void stopPlayingVideo() {
        if (pagerPlayer == null) {
            return;
        }
        pagerPlayer.setPlayWhenReady(false);
    }

    private interface PlayerChangeListener {
        void playerChanged(final int position, final SimpleExoPlayer player);
    }

    private static void loadPlayer(final Context context,
                                   final int position,
                                   final String displayUrl,
                                   final ViewSwitcher viewSwitcher,
                                   final DataSource.Factory factory,
                                   final PlayerChangeListener playerChangeListener) {
        if (viewSwitcher == null) {
            return;
        }
        SimpleExoPlayer player = (SimpleExoPlayer) viewSwitcher.getTag();
        if (player != null) {
            player.setPlayWhenReady(true);
            return;
        }
        player = new SimpleExoPlayer.Builder(context).build();
        final PlayerView playerView = (PlayerView) viewSwitcher.getChildAt(1);
        playerView.setPlayer(player);
        if (viewSwitcher.getDisplayedChild() == 0) {
            viewSwitcher.showNext();
        }
        playerView.setControllerShowTimeoutMs(1000);
        float vol = settingsHelper.getBoolean(Constants.MUTED_VIDEOS) ? 0f : 1f;
        if (vol == 0f && Utils.sessionVolumeFull) vol = 1f;
        player.setVolume(vol);
        player.setPlayWhenReady(Utils.settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));
        final MediaItem mediaItem = MediaItem.fromUri(displayUrl);
        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setVolume(vol);
        playerChangeListener.playerChanged(position, player);
        viewSwitcher.setTag(player);
    }

}
