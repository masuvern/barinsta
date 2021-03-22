package awais.instagrabber.customviews.emoji;

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

import awais.instagrabber.R;
import awais.instagrabber.customviews.emoji.EmojiPicker.OnBackspaceClickListener;
import awais.instagrabber.customviews.emoji.EmojiPicker.OnEmojiClickListener;
import awais.instagrabber.utils.Utils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * https://stackoverflow.com/a/33897583/1436766
 */
public class EmojiPopupWindow extends PopupWindow {

    private int keyBoardHeight = 0;
    private Boolean pendingOpen = false;
    private Boolean isOpened = false;
    private final View rootView;
    private final Context context;
    private final OnEmojiClickListener onEmojiClickListener;
    private final OnBackspaceClickListener onBackspaceClickListener;

    private OnSoftKeyboardOpenCloseListener onSoftKeyboardOpenCloseListener;


    /**
     * Constructor
     *
     * @param rootView The top most layout in your view hierarchy. The difference of this view and the screen height will be used to calculate the keyboard height.
     */
    public EmojiPopupWindow(final View rootView,
                            final OnEmojiClickListener onEmojiClickListener,
                            final OnBackspaceClickListener onBackspaceClickListener) {
        super(rootView.getContext());
        this.rootView = rootView;
        this.context = rootView.getContext();
        this.onEmojiClickListener = onEmojiClickListener;
        this.onBackspaceClickListener = onBackspaceClickListener;
        View customView = createCustomView();
        setContentView(customView);
        setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        //default size
        setSize((int) context.getResources().getDimension(R.dimen.keyboard_height), MATCH_PARENT);
    }

    /**
     * Set the listener for the event of keyboard opening or closing.
     */
    public void setOnSoftKeyboardOpenCloseListener(OnSoftKeyboardOpenCloseListener listener) {
        this.onSoftKeyboardOpenCloseListener = listener;
    }

    /**
     * Use this function to show the emoji popup.
     * NOTE: Since, the soft keyboard sizes are variable on different android devices, the
     * library needs you to open the soft keyboard atleast once before calling this function.
     * If that is not possible see showAtBottomPending() function.
     */
    public void showAtBottom() {
        showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
    }

    /**
     * Use this function when the soft keyboard has not been opened yet. This
     * will show the emoji popup after the keyboard is up next time.
     * Generally, you will be calling InputMethodManager.showSoftInput function after
     * calling this function.
     */
    public void showAtBottomPending() {
        if (isKeyBoardOpen())
            showAtBottom();
        else
            pendingOpen = true;
    }

    /**
     * @return Returns true if the soft keyboard is open, false otherwise.
     */
    public Boolean isKeyBoardOpen() {
        return isOpened;
    }

    /**
     * Dismiss the popup
     */
    @Override
    public void dismiss() {
        super.dismiss();
    }

    /**
     * Call this function to resize the emoji popup according to your soft keyboard size
     */
    public void setSizeForSoftKeyboard() {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);

            int screenHeight = getUsableScreenHeight();
            int heightDifference = screenHeight - (r.bottom - r.top);
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                heightDifference -= context.getResources()
                                           .getDimensionPixelSize(resourceId);
            }
            if (heightDifference > 100) {
                keyBoardHeight = heightDifference;
                setSize(MATCH_PARENT, keyBoardHeight);
                if (!isOpened) {
                    if (onSoftKeyboardOpenCloseListener != null)
                        onSoftKeyboardOpenCloseListener.onKeyboardOpen(keyBoardHeight);
                }
                isOpened = true;
                if (pendingOpen) {
                    showAtBottom();
                    pendingOpen = false;
                }
            } else {
                isOpened = false;
                if (onSoftKeyboardOpenCloseListener != null)
                    onSoftKeyboardOpenCloseListener.onKeyboardClose();
            }
        });
    }

    private int getUsableScreenHeight() {
        return Utils.displayMetrics.heightPixels;
    }

    /**
     * Manually set the popup window size
     *
     * @param width  Width of the popup
     * @param height Height of the popup
     */
    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    private View createCustomView() {
        final EmojiPicker emojiPicker = new EmojiPicker(context);
        final LayoutParams layoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        emojiPicker.setLayoutParams(layoutParams);
        emojiPicker.init(rootView, onEmojiClickListener, onBackspaceClickListener);
        return emojiPicker;
    }


    public interface OnSoftKeyboardOpenCloseListener {
        void onKeyboardOpen(int keyBoardHeight);

        void onKeyboardClose();
    }
}

