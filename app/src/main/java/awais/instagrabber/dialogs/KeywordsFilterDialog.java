package awais.instagrabber.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;

import awais.instagrabber.R;
import awais.instagrabber.adapters.KeywordsFilterAdapter;
import awais.instagrabber.databinding.DialogKeywordsFilterBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.SettingsHelper;

public final class KeywordsFilterDialog extends DialogFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final DialogKeywordsFilterBinding dialogKeywordsFilterBinding = DialogKeywordsFilterBinding.inflate(inflater, container, false);

        final Context context = getContext();
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        final RecyclerView recyclerView = dialogKeywordsFilterBinding.recyclerKeyword;
        recyclerView.setLayoutManager(linearLayoutManager);

        final SettingsHelper settingsHelper = new SettingsHelper(context);
        final ArrayList<String> items = new ArrayList<>(settingsHelper.getStringSet(Constants.KEYWORD_FILTERS));
        final KeywordsFilterAdapter adapter = new KeywordsFilterAdapter(context, items);
        recyclerView.setAdapter(adapter);

        final EditText editText = dialogKeywordsFilterBinding.editText;

        dialogKeywordsFilterBinding.btnAdd.setOnClickListener(view ->{
            final String s = editText.getText().toString();
            if(s.isEmpty()) return;
            if(items.contains(s)) {
                editText.setText("");
                return;
            }
            items.add(s);
            settingsHelper.putStringSet(Constants.KEYWORD_FILTERS, new HashSet<>(items));
            adapter.notifyItemInserted(items.size());
            final String message = context.getString(R.string.added_keywords).replace("{0}", s);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            editText.setText("");
        });

        dialogKeywordsFilterBinding.btnOK.setOnClickListener(view ->{
            this.dismiss();
        });

        return dialogKeywordsFilterBinding.getRoot();
    }
}
