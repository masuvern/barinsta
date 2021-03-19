package awais.instagrabber.adapters.viewholder.dialogs;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;

public class KeywordsFilterDialogViewHolder extends RecyclerView.ViewHolder {

    private final Button deleteButton;
    private final TextView item;

    public KeywordsFilterDialogViewHolder(@NonNull View itemView) {
        super(itemView);
        deleteButton = itemView.findViewById(R.id.keyword_delete);
        item = itemView.findViewById(R.id.keyword_text);
    }

    public Button getDeleteButton(){
        return deleteButton;
    }

    public TextView getTextView(){
        return item;
    }
}
