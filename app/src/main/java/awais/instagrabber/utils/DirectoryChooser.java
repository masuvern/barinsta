package awais.instagrabber.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.SimpleAdapter;

public final class DirectoryChooser extends DialogFragment {
    public static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";
    private static final File sdcardPathFile = Environment.getExternalStorageDirectory();
    private static final String sdcardPath = sdcardPathFile.getPath();
    private final List<String> fileNames = new ArrayList<>();
    private Context context;
    private View btnConfirm, btnNavUp, btnCancel;
    private File selectedDir;
    private String initialDirectory;
    private TextView tvSelectedFolder;
    private FileObserver fileObserver;
    private SimpleAdapter<String> listDirectoriesAdapter;
    private OnFragmentInteractionListener interactionListener;
    private boolean showZaAiConfigFiles = false;

    public DirectoryChooser() {
        super();
    }

    public DirectoryChooser setInitialDirectory(final String initialDirectory) {
        if (!Utils.isEmpty(initialDirectory))
            this.initialDirectory = initialDirectory;
        return this;
    }

    public DirectoryChooser setShowZaAiConfigFiles(final boolean showZaAiConfigFiles) {
        this.showZaAiConfigFiles = showZaAiConfigFiles;
        return this;
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);

        this.context = context;

        if (this.context instanceof OnFragmentInteractionListener)
            interactionListener = (OnFragmentInteractionListener) this.context;
        else {
            final Fragment owner = getTargetFragment();
            if (owner instanceof OnFragmentInteractionListener)
                interactionListener = (OnFragmentInteractionListener) owner;
        }
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        Context context = this.context;
        if (context == null) context = getContext();
        if (context == null) context = getActivity();
        if (context == null) context = inflater.getContext();

        final View view = inflater.inflate(R.layout.layout_directory_chooser, container, false);

        btnNavUp = view.findViewById(R.id.btnNavUp);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        tvSelectedFolder = view.findViewById(R.id.txtvSelectedFolder);

        final View.OnClickListener clickListener = v -> {
            final Object tag;
            if (v instanceof TextView && (tag = v.getTag()) instanceof CharSequence) {
                final File file = new File(selectedDir, tag.toString());
                if (file.isDirectory())
                    changeDirectory(file);
                else if (showZaAiConfigFiles && file.isFile()) {
                    if (interactionListener != null && file.canRead())
                        interactionListener.onSelectDirectory(file.getAbsolutePath());
                    dismiss();
                }

            } else if (v == btnNavUp) {
                final File parent;
                if (selectedDir != null && (parent = selectedDir.getParentFile()) != null)
                    changeDirectory(parent);

            } else if (v == btnConfirm) {
                if (interactionListener != null && isValidFile(selectedDir))
                    interactionListener.onSelectDirectory(selectedDir.getAbsolutePath());
                dismiss();
            } else if (v == btnCancel) {
                dismiss();
            }
        };

        btnNavUp.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);
        btnConfirm.setOnClickListener(clickListener);

        listDirectoriesAdapter = new SimpleAdapter<>(context, fileNames, clickListener);

        final RecyclerView directoriesList = view.findViewById(R.id.directoryList);
        directoriesList.setLayoutManager(new LinearLayoutManager(context));
        directoriesList.setAdapter(listDirectoriesAdapter);

        final File initDir = new File(initialDirectory);
        final File initialDir = !Utils.isEmpty(initialDirectory) && isValidFile(initDir) ? initDir : Environment.getExternalStorageDirectory();

        changeDirectory(initialDir);

        return view;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utils.isEmpty(initialDirectory)) {
            initialDirectory = new File(sdcardPath, "Download").getAbsolutePath();
            if (savedInstanceState != null) {
                final String savedDir = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
                if (!Utils.isEmpty(savedDir)) initialDirectory = savedDir;
            }
        }

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        return new Dialog(context, R.attr.alertDialogTheme) {
            @Override
            public void onBackPressed() {
                if (selectedDir != null) {
                    final String absolutePath = selectedDir.getAbsolutePath();
                    if (absolutePath.equals(sdcardPath) || absolutePath.equals(sdcardPathFile.getAbsolutePath()))
                        dismiss();
                    else
                        changeDirectory(selectedDir.getParentFile());
                }
            }
        };
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedDir != null) outState.putString(KEY_CURRENT_DIRECTORY, selectedDir.getAbsolutePath());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fileObserver != null) fileObserver.startWatching();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fileObserver != null) fileObserver.stopWatching();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    private void changeDirectory(final File dir) {
        if (dir != null && dir.isDirectory()) {
            final String path = dir.getAbsolutePath();

            final File[] contents = dir.listFiles();
            if (contents != null) {
                fileNames.clear();

                for (final File f : contents) {
                    final String name = f.getName();
                    if (f.isDirectory() || showZaAiConfigFiles && f.isFile() && name.toLowerCase().endsWith(".zaai"))
                        fileNames.add(name);
                }

                Collections.sort(fileNames);
                selectedDir = dir;
                tvSelectedFolder.setText(path);
                listDirectoriesAdapter.notifyDataSetChanged();
                fileObserver = new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
                    private final Runnable currentDirRefresher = () -> changeDirectory(selectedDir);

                    @Override
                    public void onEvent(final int event, final String path) {
                        if (context instanceof Activity) ((Activity) context).runOnUiThread(currentDirRefresher);
                    }
                };
                fileObserver.startWatching();
            }
        }
        refreshButtonState();
    }

    private void refreshButtonState() {
        if (selectedDir != null) {
            final String path = selectedDir.getAbsolutePath();
            toggleUpButton(!path.equals(sdcardPathFile.getAbsolutePath()) && selectedDir != sdcardPathFile);
            btnConfirm.setEnabled(isValidFile(selectedDir));
        }
    }

    private void toggleUpButton(final boolean enable) {
        if (btnNavUp != null) {
            btnNavUp.setEnabled(enable);
            btnNavUp.setAlpha(enable ? 1f : 0.617f);
        }
    }

    private boolean isValidFile(final File file) {
        return file != null && file.isDirectory() && file.canRead();
    }

    public DirectoryChooser setInteractionListener(final OnFragmentInteractionListener interactionListener) {
        this.interactionListener = interactionListener;
        return this;
    }

    public interface OnFragmentInteractionListener {
        void onSelectDirectory(final String path);
    }
}