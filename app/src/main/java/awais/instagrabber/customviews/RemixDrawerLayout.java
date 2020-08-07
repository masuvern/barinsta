package awais.instagrabber.customviews;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import awais.instagrabber.R;

public final class RemixDrawerLayout extends MouseDrawer implements MouseDrawer.DrawerListener {
    private final FrameLayout frameLayout;
    private View drawerView;
    private RecyclerView highlightsList, feedPosts, feedStories;
    private float startX;

    public RemixDrawerLayout(@NonNull final Context context) {
        this(context, null);
    }

    public RemixDrawerLayout(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemixDrawerLayout(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        super.setDrawerElevation(getDrawerElevation());

        addDrawerListener(this);

        frameLayout = new FrameLayout(context);
        frameLayout.setPadding(0, 0, 0, 0);
        super.addView(frameLayout);
    }

    @Override
    public void addView(@NonNull final View child, final ViewGroup.LayoutParams params) {
        child.setLayoutParams(params);
        addView(child);
    }

    @Override
    public void addView(@NonNull final View child) {
        if (child.getTag() != null) super.addView(child);
        else frameLayout.addView(child);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull final MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();

        // another one of my own weird hack thingies to make this app work
        if (feedPosts == null) feedPosts = findViewById(R.id.feedPosts);
        if (feedPosts != null) {
            for (int i = 0; i < feedPosts.getChildCount(); ++i) {
                final View viewHolder = feedPosts.getChildAt(i);
                final View mediaList = viewHolder.findViewById(R.id.media_list);
                if (mediaList instanceof ViewPager) {
                    final ViewPager viewPager = (ViewPager) mediaList;

                    final Rect rect = new Rect();
                    viewPager.getGlobalVisibleRect(rect);

                    final boolean touchIsInMediaList = rect.contains((int) x, (int) y);
                    if (touchIsInMediaList) {
                        final PagerAdapter adapter = viewPager.getAdapter();
                        final int count = adapter != null ? adapter.getCount() : 0;
                        if (count < 1 || viewPager.getCurrentItem() != count - 1) return false;
                        break;
                    }
                }
            }
        }

        // thanks to Fede @ https://stackoverflow.com/questions/6920137/android-viewpager-and-horizontalscrollview/7258579#7258579
        if (highlightsList == null) highlightsList = findViewById(R.id.highlightsList);
        if (highlightsList != null) {
            final Boolean result = handleHorizontalRecyclerView(ev, highlightsList);
            if (result != null) {
                return result;
            }
        }
        if (feedStories == null) feedStories = findViewById(R.id.feedStories);
        if (feedStories != null) {
            final Boolean result = handleHorizontalRecyclerView(ev, feedStories);
            if (result != null) {
                return result;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    private Boolean handleHorizontalRecyclerView(@NonNull final MotionEvent ev, final RecyclerView view) {
        final float x = ev.getX();
        final float y = ev.getY();
        final boolean touchIsInRecycler = x >= view.getLeft() && x < view.getRight()
                && y >= view.getTop() && view.getBottom() > y;

        if (touchIsInRecycler) {
            final int action = ev.getActionMasked();

            if (action == MotionEvent.ACTION_CANCEL) return super.onInterceptTouchEvent(ev);

            if (action == MotionEvent.ACTION_DOWN) startX = x;
            else if (action == MotionEvent.ACTION_MOVE) {
                final int scrollRange = view.computeHorizontalScrollRange();
                final int scrollOffset = view.computeHorizontalScrollOffset();
                final boolean scrollable = scrollRange > view.getWidth();
                final boolean draggingFromRight = startX > x;

                if (scrollOffset < 1) {
                    if (!scrollable) return super.onInterceptTouchEvent(ev);
                    else if (!draggingFromRight) return super.onInterceptTouchEvent(ev);
                } else if (scrollable && draggingFromRight && scrollRange - scrollOffset == view.computeHorizontalScrollExtent()) {
                    return super.onInterceptTouchEvent(ev);
                }

                return false;
            }
        }
        return null;
    }

    @Override
    public void onDrawerSlide(@NonNull final View view, @EdgeGravity final int gravity, final float slideOffset) {
        drawerView = view;
        final int absHorizGravity = getDrawerViewAbsoluteGravity(GravityCompat.START);
        final int childAbsGravity = getDrawerViewAbsoluteGravity(drawerView);

        final Window window = getActivity(getContext()).getWindow();
        final boolean isRtl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                || window.getDecorView().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                || getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);

        final int drawerViewWidth = drawerView.getWidth();

        // for (int i = 0; i < frameLayout.getChildCount(); i++) {
        //     final View child = frameLayout.getChildAt(i);
        //
        //     final boolean isLeftDrawer = isRtl == (childAbsGravity != absHorizGravity);
        //     float width = isLeftDrawer ? drawerViewWidth : -drawerViewWidth;
        //
        //     child.setX(width * slideOffset);
        // }

        final boolean isLeftDrawer = isRtl == (childAbsGravity != absHorizGravity);
        float width = isLeftDrawer ? drawerViewWidth : -drawerViewWidth;

        frameLayout.setX(width * (isRtl ? -slideOffset : slideOffset));
    }

    @Override
    public void openDrawer(@NonNull final View drawerView, final boolean animate) {
        super.openDrawer(drawerView, animate);
        post(() -> onDrawerSlide(drawerView, Gravity.NO_GRAVITY, isDrawerOpen(drawerView) ? 1f : 0f));
    }

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerView != null) onDrawerSlide(drawerView, Gravity.NO_GRAVITY, isDrawerOpen(drawerView) ? 1f : 0f);
    }

    private static Activity getActivity(final Context context) {
        if (context != null) {
            if (context instanceof Activity) return (Activity) context;
            if (context instanceof ContextWrapper)
                return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    final int getDrawerViewAbsoluteGravity(final int gravity) {
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this)) & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
    }

    final int getDrawerViewAbsoluteGravity(@NonNull final View drawerView) {
        final int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        return getDrawerViewAbsoluteGravity(gravity);
    }
}