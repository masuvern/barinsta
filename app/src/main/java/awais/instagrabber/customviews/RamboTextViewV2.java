package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.armcha.autolink.AutoLinkItem;
import io.github.armcha.autolink.AutoLinkTextView;
import io.github.armcha.autolink.MODE_EMAIL;
import io.github.armcha.autolink.MODE_HASHTAG;
import io.github.armcha.autolink.MODE_MENTION;
import io.github.armcha.autolink.MODE_URL;
import io.github.armcha.autolink.Mode;

public class RamboTextViewV2 extends AutoLinkTextView {
    private final List<OnMentionClickListener> onMentionClickListeners = new ArrayList<>();
    private final List<OnHashtagClickListener> onHashtagClickListeners = new ArrayList<>();
    private final List<OnURLClickListener> onURLClickListeners = new ArrayList<>();
    private final List<OnEmailClickListener> onEmailClickListeners = new ArrayList<>();

    public RamboTextViewV2(@NonNull final Context context,
                           @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        addAutoLinkMode(MODE_HASHTAG.INSTANCE, MODE_MENTION.INSTANCE, MODE_EMAIL.INSTANCE, MODE_URL.INSTANCE);
        onAutoLinkClick(autoLinkItem -> {
            final Mode mode = autoLinkItem.getMode();
            if (mode.equals(MODE_MENTION.INSTANCE)) {
                for (final OnMentionClickListener onMentionClickListener : onMentionClickListeners) {
                    onMentionClickListener.onMentionClick(autoLinkItem);
                }
                return;
            }
            if (mode.equals(MODE_HASHTAG.INSTANCE)) {
                for (final OnHashtagClickListener onHashtagClickListener : onHashtagClickListeners) {
                    onHashtagClickListener.onHashtagClick(autoLinkItem);
                }
                return;
            }
            if (mode.equals(MODE_URL.INSTANCE)) {
                for (final OnURLClickListener onURLClickListener : onURLClickListeners) {
                    onURLClickListener.onURLClick(autoLinkItem);
                }
                return;
            }
            if (mode.equals(MODE_EMAIL.INSTANCE)) {
                for (final OnEmailClickListener onEmailClickListener : onEmailClickListeners) {
                    onEmailClickListener.onEmailClick(autoLinkItem);
                }
            }
        });
    }

    public void addOnMentionClickListener(final OnMentionClickListener onMentionClickListener) {
        if (onMentionClickListener == null) {
            return;
        }
        onMentionClickListeners.add(onMentionClickListener);
    }

    public void removeOnMentionClickListener(final OnMentionClickListener onMentionClickListener) {
        if (onMentionClickListener == null) {
            return;
        }
        onMentionClickListeners.remove(onMentionClickListener);
    }

    public void clearOnMentionClickListeners() {
        onMentionClickListeners.clear();
    }

    public void addOnHashtagListener(final OnHashtagClickListener onHashtagClickListener) {
        if (onHashtagClickListener == null) {
            return;
        }
        onHashtagClickListeners.add(onHashtagClickListener);
    }

    public void removeOnHashtagListener(final OnHashtagClickListener onHashtagClickListener) {
        if (onHashtagClickListener == null) {
            return;
        }
        onHashtagClickListeners.remove(onHashtagClickListener);
    }

    public void clearOnHashtagClickListeners() {
        onHashtagClickListeners.clear();
    }

    public void addOnURLClickListener(final OnURLClickListener onURLClickListener) {
        if (onURLClickListener == null) {
            return;
        }
        onURLClickListeners.add(onURLClickListener);
    }

    public void removeOnURLClickListener(final OnURLClickListener onURLClickListener) {
        if (onURLClickListener == null) {
            return;
        }
        onURLClickListeners.remove(onURLClickListener);
    }

    public void clearOnURLClickListeners() {
        onURLClickListeners.clear();
    }

    public void addOnEmailClickListener(final OnEmailClickListener onEmailClickListener) {
        if (onEmailClickListener == null) {
            return;
        }
        onEmailClickListeners.add(onEmailClickListener);
    }

    public void removeOnEmailClickListener(final OnEmailClickListener onEmailClickListener) {
        if (onEmailClickListener == null) {
            return;
        }
        onEmailClickListeners.remove(onEmailClickListener);
    }

    public void clearOnEmailClickListeners() {
        onEmailClickListeners.clear();
    }

    public interface OnMentionClickListener {
        void onMentionClick(final AutoLinkItem autoLinkItem);
    }

    public interface OnHashtagClickListener {
        void onHashtagClick(final AutoLinkItem autoLinkItem);
    }

    public interface OnURLClickListener {
        void onURLClick(final AutoLinkItem autoLinkItem);
    }

    public interface OnEmailClickListener {
        void onEmailClick(final AutoLinkItem autoLinkItem);
    }
}
