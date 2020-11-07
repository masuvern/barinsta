package awais.instagrabber.customviews;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.NonNull;

public class VerticalDragHelper {
    private static final String TAG = "VerticalDragHelper";
    private static final float PIXELS_PER_SECOND = 10;

    private final View view;

    private GestureDetector gestureDetector;
    private Context context;
    private float flingVelocity;
    private OnVerticalDragListener onVerticalDragListener;

    private final GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            view.performClick();
            return true;
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
            float maxFlingVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
            float velocityPercentY = velocityY / maxFlingVelocity;
            float normalizedVelocityY = velocityPercentY * PIXELS_PER_SECOND;
            if (Math.abs(normalizedVelocityY) > 4) {
                flingVelocity = normalizedVelocityY;
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }


    };

    private final GestureDetector.OnGestureListener dragPreventionGestureListener = new GestureDetector.SimpleOnGestureListener() {
        float prevDistanceY = 0;

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            Log.d(TAG, "onScroll: distanceX: " + distanceX + ", distanceY: " + distanceY);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            Log.d(TAG, "onSingleTapUp");
            return super.onSingleTapUp(e);
        }
    };

    private float prevRawY;
    private boolean isDragging;
    private float prevRawX;
    private float dX;
    private float prevDY;
    private GestureDetector dragPreventionGestureDetector;

    public VerticalDragHelper(@NonNull final View view) {
        this.view = view;
        final Context context = view.getContext();
        if (context == null) return;
        this.context = context;
        init();
    }

    public void setOnVerticalDragListener(@NonNull final OnVerticalDragListener onVerticalDragListener) {
        this.onVerticalDragListener = onVerticalDragListener;
    }

    protected void init() {
        gestureDetector = new GestureDetector(context, gestureListener);
        dragPreventionGestureDetector = new GestureDetector(context, dragPreventionGestureListener);
    }

    public boolean onDragTouch(final MotionEvent event) {
        if (onVerticalDragListener == null) {
            return false;
        }
        // dragPreventionGestureDetector.onTouchEvent(event);
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                boolean handled = false;
                final float rawY = event.getRawY();
                final float dY = rawY - prevRawY;
                if (!isDragging) {
                    final float rawX = event.getRawX();
                    if (prevRawX != 0) {
                        dX = rawX - prevRawX;
                    }
                    prevRawX = rawX;
                    if (prevRawY != 0) {
                        final float dYAbs = Math.abs(dY - prevDY);
                        if (!isDragging && dYAbs < 50) {
                            final float abs = Math.abs(dY) - Math.abs(dX);
                            if (abs > 0) {
                                isDragging = true;
                            }
                        }
                    }
                }
                if (isDragging) {
                    final ViewParent parent = view.getParent();
                    parent.requestDisallowInterceptTouchEvent(true);
                    onVerticalDragListener.onDrag(dY);
                    handled = true;
                }
                prevDY = dY;
                prevRawY = rawY;
                return handled;
            case MotionEvent.ACTION_UP:
                // Log.d(TAG, "onDragTouch: reset prevRawY");
                prevRawY = 0;
                if (flingVelocity != 0) {
                    onVerticalDragListener.onFling(flingVelocity);
                    flingVelocity = 0;
                    isDragging = false;
                    return true;
                }
                if (isDragging) {
                    onVerticalDragListener.onDragEnd();
                    isDragging = false;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    public boolean isDragging() {
        return isDragging;
    }

    public boolean onGestureTouchEvent(final MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final static int DIRECTION_UP = 0;
    private final static int DIRECTION_DOWN = 1;
    float prevY = -1;
    int edgeHitCount = 0;
    float prevDirection = -1;


    // private boolean shouldPreventDrag(final MotionEvent event) {
    //     switch (event.getAction()) {
    //         case MotionEvent.ACTION_DOWN:
    //             if (!firstDrag) {
    //                 firstDrag = true;
    //             }
    //             return false;
    //         case MotionEvent.ACTION_MOVE:
    //             float y = event.getY();
    //             int direction = -2;
    //             if (prevY != -1) {
    //                 final float dy = y - prevY;
    //                 // Log.d(TAG, "shouldPreventDrag: dy: " + dy);
    //                 if (dy > 0) {
    //                     direction = DIRECTION_DOWN;
    //                     // move direction is down
    //                 } else {
    //                     direction = DIRECTION_UP;
    //                     // move direction is up
    //                 }
    //             }
    //             prevY = y;
    //             if (prevDirection == direction) {
    //                 edgeHitCount++;
    //             } else {
    //                 edgeHitCount = 1;
    //             }
    //             if (edgeHitCount >= 2) {
    //                 return false;
    //             }
    //             return true;
    //             break;
    //     }
    // }

    public interface OnVerticalDragListener {
        void onDrag(final float dY);

        void onDragEnd();

        void onFling(final float flingVelocity);
    }
}
