package awais.instagrabber.adapters;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;

import awais.instagrabber.R;

public final class SuggestionsAdapter extends CursorAdapter {
    private final LayoutInflater layoutInflater;
    private final View.OnClickListener onClickListener;
    private final RequestManager glideRequestManager;

    public SuggestionsAdapter(final Context context, final View.OnClickListener onClickListener) {
        super(context, null, FLAG_REGISTER_CONTENT_OBSERVER);
        this.glideRequestManager = Glide.with(context);
        this.layoutInflater = LayoutInflater.from(context);
        this.onClickListener = onClickListener;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        return layoutInflater.inflate(R.layout.item_suggestion, parent, false);
    }

    @Override
    public void bindView(@NonNull final View view, final Context context, @NonNull final Cursor cursor) {
        // i, username, fullname, type, picUrl, verified
        // 0, 1       , 2       , 3   , 4     , 5

        final String fullname = cursor.getString(2);
        String username = cursor.getString(1);
        final String picUrl = cursor.getString(4);
        final boolean verified = cursor.getString(5).charAt(0) == 't';

        if ("TYPE_HASHTAG".equals(cursor.getString(3))) username = '#' + username;

        view.setOnClickListener(onClickListener);
        view.setTag(username);

        view.findViewById(R.id.isVerified).setVisibility(verified ? View.VISIBLE : View.GONE);

        ((TextView) view.findViewById(R.id.tvUsername)).setText(username);
        ((TextView) view.findViewById(R.id.tvFullName)).setText(fullname);

        glideRequestManager.applyDefaultRequestOptions(new RequestOptions().skipMemoryCache(true))
                .load(picUrl).into((ImageView) view.findViewById(R.id.ivProfilePic));
    }
}