package awais.instagrabber.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import awais.instagrabber.R;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.FeedModel;
import awais.instagrabber.utils.Utils;

public final class RamboTextView extends AppCompatTextView {
    private static final int highlightBackgroundSpanKey = R.id.tvComment;
    private static final RectF touchedLineBounds = new RectF();
    private ClickableSpan clickableSpanUnderTouchOnActionDown;
    private MentionClickListener mentionClickListener;
    private boolean isUrlHighlighted, isExpandable, isExpanded;

    public RamboTextView(final Context context) {
        super(context);
    }

    public RamboTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public RamboTextView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMentionClickListener(final MentionClickListener mentionClickListener) {
        this.mentionClickListener = mentionClickListener;
    }

    public void setCaptionIsExpandable(final boolean isExpandable) {
        this.isExpandable = isExpandable;
    }

    public void setCaptionIsExpanded(final boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final CharSequence text = getText();
        if (text instanceof SpannableString || text instanceof SpannableStringBuilder) {
            final Spannable spanText = (Spannable) text;
            final ClickableSpan clickableSpanUnderTouch = findClickableSpanUnderTouch(this, spanText, event);

            final int action = event.getAction();

            if (action == MotionEvent.ACTION_DOWN) clickableSpanUnderTouchOnActionDown = clickableSpanUnderTouch;
            final boolean touchStartedOverAClickableSpan = clickableSpanUnderTouchOnActionDown != null;
            final boolean isURLSpan = clickableSpanUnderTouch instanceof URLSpan;

            // feed view caption hacks
            if (isExpandable && !touchStartedOverAClickableSpan)
                return !isExpanded | super.onTouchEvent(event); // short operator, because we want two shits to work

            final Object tag = getTag();
            final FeedModel feedModel = tag instanceof FeedModel ? (FeedModel) tag : null;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (feedModel != null) feedModel.setMentionClicked(false);
                    if (clickableSpanUnderTouch != null) highlightUrl(clickableSpanUnderTouch, spanText);
                    return isURLSpan ? super.onTouchEvent(event) : touchStartedOverAClickableSpan;

                case MotionEvent.ACTION_UP:
                    if (touchStartedOverAClickableSpan && clickableSpanUnderTouch == clickableSpanUnderTouchOnActionDown) {
                        dispatchUrlClick(spanText, clickableSpanUnderTouch);
                        if (feedModel != null) feedModel.setMentionClicked(true);
                    }
                    cleanupOnTouchUp(spanText);
                    return isURLSpan ? super.onTouchEvent(event) : touchStartedOverAClickableSpan;

                case MotionEvent.ACTION_MOVE:
                    if (feedModel != null) feedModel.setMentionClicked(false);
                    if (clickableSpanUnderTouch != null) highlightUrl(clickableSpanUnderTouch, spanText);
                    else removeUrlHighlightColor(spanText);
                    return isURLSpan ? super.onTouchEvent(event) : touchStartedOverAClickableSpan;

                case MotionEvent.ACTION_CANCEL:
                    if (feedModel != null) feedModel.setMentionClicked(false);
                    cleanupOnTouchUp(spanText);
                    return super.onTouchEvent(event);
            }
        }

        return super.onTouchEvent(event);
    }

    protected void dispatchUrlClick(final Spanned s, final ClickableSpan clickableSpan) {
        if (mentionClickListener != null) {
            final int spanStart = s.getSpanStart(clickableSpan);
            final boolean ishHashtag = s.charAt(spanStart) == '#';

            final int start = ishHashtag || s.charAt(spanStart) != '@' ? spanStart : spanStart + 1;

            CharSequence subSequence = s.subSequence(start, s.getSpanEnd(clickableSpan));

            // for feed ellipsize
            final int indexOfEllipsize = Utils.indexOfChar(subSequence, '…', 0);
            if (indexOfEllipsize != -1)
                subSequence = subSequence.subSequence(0, indexOfEllipsize - 1);

            mentionClickListener.onClick(this, subSequence.toString(), ishHashtag);
        }
    }

    protected void highlightUrl(final ClickableSpan clickableSpan, final Spannable text) {
        if (!isUrlHighlighted) {
            isUrlHighlighted = true;

            final int spanStart = text.getSpanStart(clickableSpan);
            final int spanEnd = text.getSpanEnd(clickableSpan);
            final BackgroundColorSpan highlightSpan = new BackgroundColorSpan(getHighlightColor());
            text.setSpan(highlightSpan, spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            setTag(highlightBackgroundSpanKey, highlightSpan);
            Selection.setSelection(text, spanStart, spanEnd);
        }
    }

    protected void removeUrlHighlightColor(final Spannable text) {
        if (isUrlHighlighted) {
            isUrlHighlighted = false;

            final BackgroundColorSpan highlightSpan = (BackgroundColorSpan) getTag(highlightBackgroundSpanKey);
            text.removeSpan(highlightSpan);

            Selection.removeSelection(text);
        }
    }

    private void cleanupOnTouchUp(final Spannable text) {
        clickableSpanUnderTouchOnActionDown = null;
        removeUrlHighlightColor(text);
    }

    @Nullable
    private static ClickableSpan findClickableSpanUnderTouch(@NonNull final TextView textView, final Spannable text, @NonNull final MotionEvent event) {
        final int touchX = (int) (event.getX() - textView.getTotalPaddingLeft() + textView.getScrollX());
        final int touchY = (int) (event.getY() - textView.getTotalPaddingTop() + textView.getScrollY());

        final Layout layout = textView.getLayout();
        final int touchedLine = layout.getLineForVertical(touchY);
        final int touchOffset = layout.getOffsetForHorizontal(touchedLine, touchX);

        touchedLineBounds.left = layout.getLineLeft(touchedLine);
        touchedLineBounds.top = layout.getLineTop(touchedLine);
        touchedLineBounds.right = layout.getLineWidth(touchedLine) + touchedLineBounds.left;
        touchedLineBounds.bottom = layout.getLineBottom(touchedLine);

        if (touchedLineBounds.contains(touchX, touchY)) {
            final Object[] spans = text.getSpans(touchOffset, touchOffset, ClickableSpan.class);
            for (final Object span : spans)
                if (span instanceof ClickableSpan) return (ClickableSpan) span;
        }

        return null;
    }

    public boolean isCaptionExpanded() {
        return isExpanded;
    }
}