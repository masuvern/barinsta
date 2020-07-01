package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.utils.DataBox;

public final class SimpleAdapter<T> extends RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder> {
    private List<T> items;
    private final LayoutInflater layoutInflater;
    private final View.OnClickListener onClickListener;
    private final View.OnLongClickListener longClickListener;

    public SimpleAdapter(final Context context, final List<T> items, final View.OnClickListener onClickListener) {
        this(context, items, onClickListener, null);
    }

    public SimpleAdapter(final Context context, final List<T> items, final View.OnClickListener onClickListener,
                         final View.OnLongClickListener longClickListener) {
        this.layoutInflater = LayoutInflater.from(context);
        this.items = items;
        this.onClickListener = onClickListener;
        this.longClickListener = longClickListener;
    }

    public void setItems(final List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SimpleViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new SimpleViewHolder(layoutInflater.
                inflate(R.layout.item_dir_list, parent, false), onClickListener, longClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final SimpleViewHolder holder, final int position) {
        final T item = items.get(position);
        holder.itemView.setTag(item);
        holder.text.setText(item.toString());
        if (item instanceof DataBox.CookieModel && ((DataBox.CookieModel) item).isSelected() ||
                item instanceof String && ((String) item).toLowerCase().endsWith(".zaai"))
            holder.itemView.setBackgroundColor(0xF0_125687);
        else
            holder.itemView.setBackground(null);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static final class SimpleViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        private SimpleViewHolder(@NonNull final View itemView, final View.OnClickListener onClickListener,
                                 final View.OnLongClickListener longClickListener) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(onClickListener);
            if (longClickListener != null) itemView.setOnLongClickListener(longClickListener);
        }
    }
}
