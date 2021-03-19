package awais.instagrabber.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;

import awais.instagrabber.adapters.KeywordsFilterAdapter;
import awais.instagrabber.databinding.DialogKeywordsFilterBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.SettingsHelper;

public final class KeywordsFilterDialog extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogKeywordsFilterBinding dialogKeywordsFilterBinding = DialogKeywordsFilterBinding.inflate(inflater, container, false);

        final Context context = getContext();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        final RecyclerView recyclerView = dialogKeywordsFilterBinding.recycler;
        recyclerView.setLayoutManager(linearLayoutManager);

        SettingsHelper settingsHelper = new SettingsHelper(context);
        ArrayList<String> items = new ArrayList<>(settingsHelper.getStringSet(Constants.KEYWORD_FILTERS));
        KeywordsFilterAdapter adapter = new KeywordsFilterAdapter(context, items);
        recyclerView.setAdapter(adapter);

        final EditText editText = dialogKeywordsFilterBinding.editText;

        dialogKeywordsFilterBinding.addIcon.setOnClickListener(view ->{
            final String s = editText.getText().toString();
            if(s.isEmpty()) return;
            if(items.contains(s)) {
                editText.setText("");
                return;
            }
            items.add(s);
            settingsHelper.putStringSet(Constants.KEYWORD_FILTERS, new HashSet<>(items));
            adapter.notifyDataSetChanged();
        });

        dialogKeywordsFilterBinding.btnOK.setOnClickListener(view ->{
            this.dismiss();
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
