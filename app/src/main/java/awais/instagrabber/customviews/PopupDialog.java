package awais.instagrabber.customviews;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import awais.instagrabber.utils.Utils;

/**
 * https://stackoverflow.com/a/15766097/1436766
 */
public class PopupDialog extends Dialog {
    private final Context context;

    public PopupDialog(Context context) {
        super(context);
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public void showAtLocation(final IBinder token, final int gravity, int x, int y) {
        final Window window = getWindow();
        if (window == null) return;
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = gravity;
        layoutParams.x = x;
        layoutParams.y = y;
        // layoutParams.token = token;
        show();
    }

    public void showAsDropDown(View view) {
        float density = Utils.displayMetrics.density;
        final Window window = getWindow();
        if (window == null) return;
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        int[] location = new int[2];
        view.getLocationInWindow(location);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = location[0] + (int) (view.getWidth() / density);
        layoutParams.y = location[1] + (int) (view.getHeight() / density);
        show();
    }

    public void setBackgroundDrawable(final Drawable drawable) {
        final Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(drawable);
    }
}
