package awais.instagrabber.adapters.viewholder.feed;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ItemFeedSliderBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
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
    public void bindItem(final FeedModel feedModel) {
        final ViewerPostModel[] sliderItems = feedModel.getSliderItems();
        final int sliderItemLen = sliderItems != null ? sliderItems.length : 0;
        if (sliderItemLen <= 0) {
            return;
        }
        final String text = "1/" + sliderItemLen;
        binding.mediaCounter.setText(text);
        binding.mediaList.setOffscreenPageLimit(Math.min(5, sliderItemLen));

        final PagerAdapter adapter = binding.mediaList.getAdapter();
        if (adapter != null) {
            final int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                adapter.destroyItem(binding.mediaList, i, binding.mediaList.getChildAt(i));
            }
        }
        final ChildMediaItemsAdapter itemsAdapter = new ChildMediaItemsAdapter(sliderItems,
                cacheDataSourceFactory != null ? cacheDataSourceFactory : dataSourceFactory,
                playerChangeListener);
        binding.mediaList.setAdapter(itemsAdapter);

        //noinspection deprecation
        binding.mediaList.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private int prevPos = 0;

            @Override
            public void onPageSelected(final int position) {
                ViewerPostModel sliderItem = sliderItems[prevPos];
                if (sliderItem != null) {
                    sliderItem.setSelected(false);
                    if (sliderItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                        // stop playing prev video
                        final ViewSwitcher prevChild = (ViewSwitcher) binding.mediaList.getChildAt(prevPos);
                        if (prevChild == null || prevChild.getTag() == null || !(prevChild.getTag() instanceof SimpleExoPlayer)) {
                            return;
                        }
                        ((SimpleExoPlayer) prevChild.getTag()).setPlayWhenReady(false);
                    }
                }
                sliderItem = sliderItems[position];
                if (sliderItem == null) return;
                sliderItem.setSelected(true);
                final String text = (position + 1) + "/" + sliderItemLen;
                binding.mediaCounter.setText(text);
                prevPos = position;
                if (sliderItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                    binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
                    if (shouldAutoPlay) {
                        autoPlay(position);
                    }
                } else binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
            }
        });

        final View.OnClickListener muteClickListener = v -> {
            final int currentItem = binding.mediaList.getCurrentItem();
            if (currentItem < 0 || currentItem >= binding.mediaList.getChildCount()) {
                return;
            }
            final ViewerPostModel sliderItem = sliderItems[currentItem];
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
            binding.itemFeedBottom.btnMute.setImageResource(intVol == 0f ? R.drawable.vol : R.drawable.mute);
            Utils.sessionVolumeFull = intVol == 1f;
        };
        final ViewerPostModel firstItem = sliderItems[0];
        if (firstItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
            binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
        }
        binding.itemFeedBottom.btnMute.setImageResource(Utils.sessionVolumeFull ? R.drawable.mute : R.drawable.vol);
        binding.itemFeedBottom.btnMute.setOnClickListener(muteClickListener);
    }

    private void autoPlay(final int position) {
        if (!shouldAutoPlay) {
            return;
        }
        final ChildMediaItemsAdapter adapter = (ChildMediaItemsAdapter) binding.mediaList.getAdapter();
        if (adapter == null) {
            return;
        }
        final ViewerPostModel sliderItem = adapter.getItemAtPosition(position);
        if (sliderItem.getItemType() != MediaItemType.MEDIA_TYPE_VIDEO) {
            return;
        }
        final ViewSwitcher viewSwitcher = (ViewSwitcher) binding.mediaList.getChildAt(position);
        loadPlayer(binding.getRoot().getContext(),
                position, sliderItem.getDisplayUrl(),
                viewSwitcher,
                cacheDataSourceFactory != null ? cacheDataSourceFactory : dataSourceFactory,
                playerChangeListener);
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
                                   final int position, final String displayUrl,
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
        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(displayUrl));
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.prepare(mediaSource);
        player.setVolume(vol);
        playerChangeListener.playerChanged(position, player);
        viewSwitcher.setTag(player);
    }

    private static final class ChildMediaItemsAdapter extends PagerAdapter {
        // private static final String TAG = "ChildMediaItemsAdapter";

        private final ViewerPostModel[] sliderItems;
        private final DataSource.Factory factory;
        private final PlayerChangeListener playerChangeListener;
        private final ViewGroup.LayoutParams layoutParams;

        private ChildMediaItemsAdapter(final ViewerPostModel[] sliderItems,
                                       final DataSource.Factory factory,
                                       final PlayerChangeListener playerChangeListener) {
            this.sliderItems = sliderItems;
            this.factory = factory;
            this.playerChangeListener = playerChangeListener;
            layoutParams = new ViewGroup.LayoutParams(Utils.displayMetrics.widthPixels, Utils.displayMetrics.widthPixels + 1);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
            final Context context = container.getContext();
            final ViewerPostModel sliderItem = sliderItems[position];

            final String displayUrl = sliderItem.getDisplayUrl();
            if (sliderItem.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                final ViewSwitcher viewSwitcher = createViewSwitcher(context, position, sliderItem.getSliderDisplayUrl(), displayUrl);
                container.addView(viewSwitcher);
                return viewSwitcher;
            }
            final GenericDraweeHierarchy hierarchy = GenericDraweeHierarchyBuilder.newInstance(container.getResources())
                                                                                  .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                                                                                  .build();
            final SimpleDraweeView photoView = new SimpleDraweeView(context, hierarchy);
            photoView.setLayoutParams(layoutParams);
            final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(displayUrl))
                                                                 .setLocalThumbnailPreviewsEnabled(true)
                                                                 .setProgressiveRenderingEnabled(true)
                                                                 .build();
            photoView.setImageRequest(imageRequest);
            container.addView(photoView);
            return photoView;
        }

        @NonNull
        private ViewSwitcher createViewSwitcher(final Context context, final int position, final String sliderDisplayUrl, final String displayUrl) {

            final ViewSwitcher viewSwitcher = new ViewSwitcher(context);
            viewSwitcher.setLayoutParams(layoutParams);

            final FrameLayout frameLayout = new FrameLayout(context);
            frameLayout.setLayoutParams(layoutParams);

            final GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(context.getResources())
                    .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                    .build();
            final SimpleDraweeView simpleDraweeView = new SimpleDraweeView(context, hierarchy);
            simpleDraweeView.setLayoutParams(layoutParams);
            simpleDraweeView.setImageURI(sliderDisplayUrl);
            frameLayout.addView(simpleDraweeView);

            final AppCompatImageView imageView = new AppCompatImageView(context);
            final int px = Utils.convertDpToPx(50);
            final FrameLayout.LayoutParams playButtonLayoutParams = new FrameLayout.LayoutParams(px, px);
            playButtonLayoutParams.gravity = Gravity.CENTER;
            imageView.setLayoutParams(playButtonLayoutParams);
            imageView.setImageResource(R.drawable.exo_icon_play);
            frameLayout.addView(imageView);

            viewSwitcher.addView(frameLayout);

            final PlayerView playerView = new PlayerView(context);
            viewSwitcher.addView(playerView);
            if (shouldAutoPlay && position == 0) {
                loadPlayer(context, position, displayUrl, viewSwitcher, factory, playerChangeListener);
            } else
                frameLayout.setOnClickListener(v -> loadPlayer(context, position, displayUrl, viewSwitcher, factory, playerChangeListener));
            return viewSwitcher;
        }

        @Override
        public void destroyItem(@NonNull final ViewGroup container, final int position, @NonNull final Object object) {
            final View view = container.getChildAt(position);
            // Log.d(TAG, "destroy position: " + position + ", view: " + view);
            if (view instanceof ViewSwitcher) {
                final Object tag = view.getTag();
                if (tag instanceof SimpleExoPlayer) {
                    final SimpleExoPlayer player = (SimpleExoPlayer) tag;
                    player.release();
                }
            }
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return sliderItems != null ? sliderItems.length : 0;
        }

        @Override
        public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
            return view == object;
        }

        public ViewerPostModel getItemAtPosition(final int position) {
            return sliderItems[0];
        }
    }
}
