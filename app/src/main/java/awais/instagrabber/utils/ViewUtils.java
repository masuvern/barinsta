package awais.instagrabber.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;

public final class ViewUtils {

    public static final int MATCH_PARENT = -1;
    public static final int WRAP_CONTENT = -2;

    public static Drawable createRoundRectDrawableWithIcon(final Context context, int rad, int iconRes) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(0xffffffff);
        final Drawable d = ResourcesCompat.getDrawable(context.getResources(), iconRes, null);
        if (d == null) return null;
        Drawable drawable = d.mutate();
        return new CombinedDrawable(defaultDrawable, drawable);
    }

    public static Drawable createRoundRectDrawable(int rad, int defaultColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        return defaultDrawable;
    }

    public static FrameLayout.LayoutParams createFrame(int width,
                                                       float height,
                                                       int gravity,
                                                       float leftMargin,
                                                       float topMargin,
                                                       float rightMargin,
                                                       float bottomMargin) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(getSize(width), getSize(height), gravity);
        layoutParams.setMargins(Utils.convertDpToPx(leftMargin), Utils.convertDpToPx(topMargin), Utils.convertDpToPx(rightMargin),
                                Utils.convertDpToPx(bottomMargin));
        return layoutParams;
    }

    public static GradientDrawable createGradientDrawable(final GradientDrawable.Orientation orientation,
                                                          @ColorInt final int[] colors) {
        final GradientDrawable drawable = new GradientDrawable(orientation, colors);
        drawable.setShape(GradientDrawable.RECTANGLE);
        return drawable;
    }

    private static int getSize(float size) {
        return (int) (size < 0 ? size : Utils.convertDpToPx(size));
    }

    public static Pair<Integer, Integer> measure(@NonNull final View view, @NonNull final View parent) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED)
        );
        return new Pair<>(view.getMeasuredHeight(), view.getMeasuredWidth());
    }

    public static float getTextViewValueWidth(final TextView textView, final String text) {
        return textView.getPaint().measureText(text);
    }
}
