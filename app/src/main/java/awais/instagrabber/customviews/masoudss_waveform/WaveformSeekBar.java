package awais.instagrabber.customviews.masoudss_waveform;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import awais.instagrabber.R;
import awais.instagrabber.utils.Utils;

public final class WaveformSeekBar extends View {
    private final int mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private final Paint mWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mWaveRect = new RectF();
    private final Canvas mProgressCanvas = new Canvas();
    private final WaveGravity waveGravity = WaveGravity.BOTTOM;
    private final int waveBackgroundColor;
    private final int waveProgressColor;
    private final float waveWidth = Utils.convertDpToPx(3);
    private final float waveMinHeight = Utils.convertDpToPx(4);
    private final float waveCornerRadius = Utils.convertDpToPx(2);
    private final float waveGap = Utils.convertDpToPx(1);
    private int mCanvasWidth = 0;
    private int mCanvasHeight = 0;
    private float mTouchDownX = 0F;
    private int[] sample;
    private int progress = 0;
    private WaveFormProgressChangeListener progressChangeListener;

    public WaveformSeekBar(final Context context) {
        this(context, null);
    }

    public WaveformSeekBar(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformSeekBar(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.waveBackgroundColor = ContextCompat.getColor(context, R.color.text_color_light);
        this.waveProgressColor = ContextCompat.getColor(context, R.color.text_color_dark);
    }

    private int getSampleMax() {
        int max = -1;
        if (sample != null) for (final int i : sample) if (i >= max) max = i;
        return max;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (sample != null && sample.length != 0) {
            final int availableWidth = getAvailableWidth();
            final int availableHeight = getAvailableHeight();

            final float step = availableWidth / (waveGap + waveWidth) / sample.length;

            float i = 0F;
            float lastWaveRight = (float) getPaddingLeft();

            final int sampleMax = getSampleMax();
            while (i < sample.length) {
                float waveHeight = availableHeight * ((float) sample[(int) i] / sampleMax);

                if (waveHeight < waveMinHeight)
                    waveHeight = waveMinHeight;

                final float top;
                if (waveGravity == WaveGravity.TOP) {
                    top = (float) getPaddingTop();
                } else if (waveGravity == WaveGravity.CENTER) {
                    top = (float) getPaddingTop() + availableHeight / 2F - waveHeight / 2F;
                } else if (waveGravity == WaveGravity.BOTTOM) {
                    top = mCanvasHeight - (float) getPaddingBottom() - waveHeight;
                } else {
                    top = 0;
                }

                mWaveRect.set(lastWaveRight, top, lastWaveRight + waveWidth, top + waveHeight);

                if (mWaveRect.contains(availableWidth * progress / 100F, mWaveRect.centerY())) {
                    int bitHeight = (int) mWaveRect.height();
                    if (bitHeight <= 0) bitHeight = (int) waveWidth;

                    final Bitmap bitmap = Bitmap.createBitmap(availableWidth, bitHeight, Bitmap.Config.ARGB_8888);
                    mProgressCanvas.setBitmap(bitmap);

                    float fillWidth = availableWidth * progress / 100F;

                    mWavePaint.setColor(waveProgressColor);
                    mProgressCanvas.drawRect(0F, 0F, fillWidth, mWaveRect.bottom, mWavePaint);

                    mWavePaint.setColor(waveBackgroundColor);
                    mProgressCanvas.drawRect(fillWidth, 0F, (float) availableWidth, mWaveRect.bottom, mWavePaint);

                    mWavePaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                } else {
                    mWavePaint.setColor(mWaveRect.right <= availableWidth * progress / 100F ? waveProgressColor : waveBackgroundColor);
                    mWavePaint.setShader(null);
                }

                canvas.drawRoundRect(mWaveRect, waveCornerRadius, waveCornerRadius, mWavePaint);

                lastWaveRight = mWaveRect.right + waveGap;

                if (lastWaveRight + waveWidth > availableWidth + getPaddingLeft())
                    break;

                i += 1 / step;
            }
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isParentScrolling()) mTouchDownX = event.getX();
                else updateProgress(event);
                break;

            case MotionEvent.ACTION_MOVE:
                updateProgress(event);
                break;

            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - mTouchDownX) > mScaledTouchSlop)
                    updateProgress(event);

                performClick();
                break;
        }

        return true;
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCanvasWidth = w;
        mCanvasHeight = h;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private boolean isParentScrolling() {
        View parent = (View) getParent();
        final View root = getRootView();

        while (true) {
            if (parent.canScrollHorizontally(1) || parent.canScrollHorizontally(-1) ||
                    parent.canScrollVertically(1) || parent.canScrollVertically(-1))
                return true;

            if (parent == root) return false;

            parent = (View) parent.getParent();
        }
    }

    private void updateProgress(@NonNull final MotionEvent event) {
        progress = (int) (100 * event.getX() / getAvailableWidth());
        invalidate();

        if (progressChangeListener != null)
            progressChangeListener.onProgressChanged(this, Math.min(Math.max(0, progress), 100), true);
    }

    private int getAvailableWidth() {
        return mCanvasWidth - getPaddingLeft() - getPaddingRight();
    }

    private int getAvailableHeight() {
        return mCanvasHeight - getPaddingTop() - getPaddingBottom();
    }

    // public void setSampleFrom(final String path, final boolean ignoreExtension) { // was false
    //     try {
    //         final SoundParser soundFile = SoundParser.create(path, ignoreExtension);
    //         sample = soundFile.frameGains;
    //     } catch (final Exception e) {
    //         sample = null;
    //     }
    // }
    //
    // public void setSampleFrom(@NonNull final File file, final boolean ignoreExtension) { // was false
    //     setSampleFrom(file.getAbsolutePath(), ignoreExtension);
    // }

    public void setProgress(final int progress) {
        this.progress = progress;
        invalidate();
    }

    public void setProgressChangeListener(final WaveFormProgressChangeListener progressChangeListener) {
        this.progressChangeListener = progressChangeListener;
    }

    public void setSample(final int[] sample) {
        if (sample != this.sample) {
            this.sample = sample;
            invalidate();
        }
    }
}