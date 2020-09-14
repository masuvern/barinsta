package awais.instagrabber.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.SimpleAdapter;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class QuickAccessDialog extends BottomSheetDialogFragment implements DialogInterface.OnShowListener,
        View.OnClickListener, View.OnLongClickListener {
    private boolean cookieChanged, isQuery;
    private Activity activity;
    private String userQuery, displayName;
    private View btnFavorite, btnImportExport;
    private SimpleAdapter<DataBox.FavoriteModel> favoritesAdapter;
    private RecyclerView rvFavorites, rvQuickAccess;

    public QuickAccessDialog setQuery(final String userQuery, final String displayName) {
        this.userQuery = userQuery;
        this.displayName = displayName;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(this);

        final Context context = getContext();
        activity = context instanceof Activity ? (Activity) context : getActivity();

        final View contentView = View.inflate(activity, R.layout.dialog_quick_access, null);

        btnFavorite = contentView.findViewById(R.id.btnFavorite);
        btnImportExport = contentView.findViewById(R.id.importExport);

        isQuery = !TextUtils.isEmpty(userQuery);
        btnFavorite.setVisibility(isQuery ? View.VISIBLE : View.GONE);
        Utils.setTooltipText(btnImportExport, R.string.import_export);

        favoritesAdapter = new SimpleAdapter<>(activity, Utils.dataBox.getAllFavorites(), this, this);

        btnFavorite.setOnClickListener(this);
        btnImportExport.setOnClickListener(this);

        rvFavorites = contentView.findViewById(R.id.rvFavorites);
        rvQuickAccess = contentView.findViewById(R.id.rvQuickAccess);

        final DividerItemDecoration itemDecoration = new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL);
        rvFavorites.addItemDecoration(itemDecoration);
        rvFavorites.setAdapter(favoritesAdapter);

        final String cookieStr = settingsHelper.getString(Constants.COOKIE);
        if (!TextUtils.isEmpty(cookieStr)
                || Utils.dataBox.getCookieCount() > 0 // fallback for export / import
        ) {
            rvQuickAccess.addItemDecoration(itemDecoration);
            final ArrayList<DataBox.CookieModel> allCookies = Utils.dataBox.getAllCookies();
            if (!TextUtils.isEmpty(cookieStr) && allCookies != null) {
                for (final DataBox.CookieModel cookie : allCookies) {
                    if (cookieStr.equals(cookie.getCookie())) {
                        cookie.setSelected(true);
                        break;
                    }
                }
            }
            rvQuickAccess.setAdapter(new SimpleAdapter<>(activity, allCookies, this, this));
        } else {
            ((View) rvQuickAccess.getParent()).setVisibility(View.GONE);
        }

        dialog.setContentView(contentView);
        return dialog;
    }

    @Override
    public void onClick(@NonNull final View v) {
        final Object tag = v.getTag();
        if (v == btnFavorite) {
            if (isQuery) {
                Utils.dataBox.addFavorite(new DataBox.FavoriteModel(userQuery, System.currentTimeMillis(), displayName));
                favoritesAdapter.setItems(Utils.dataBox.getAllFavorites());
            }
        } else if (v == btnImportExport) {
            if (ContextCompat.checkSelfPermission(activity, DownloadUtils.PERMS[0]) == PackageManager.PERMISSION_DENIED)
                requestPermissions(DownloadUtils.PERMS, 6007);
            else Utils.showImportExportDialog(v.getContext());

        } else if (tag instanceof DataBox.FavoriteModel) {
            // if (MainActivityBackup.scanHack != null) {
            //     MainActivityBackup.scanHack.onResult(((DataBox.FavoriteModel) tag).getQuery());
            //     dismiss();
            // }

        } else if (tag instanceof DataBox.CookieModel) {
            final DataBox.CookieModel cookieModel = (DataBox.CookieModel) tag;
            if (!cookieModel.isSelected()) {
                settingsHelper.putString(Constants.COOKIE, cookieModel.getCookie());
                CookieUtils.setupCookies(cookieModel.getCookie());
                cookieChanged = true;
            }
            dismiss();
        }
    }

    @Override
    public boolean onLongClick(@NonNull final View v) {
        final Object tag = v.getTag();

        if (tag instanceof DataBox.FavoriteModel) {
            final DataBox.FavoriteModel favoriteModel = (DataBox.FavoriteModel) tag;

            new AlertDialog.Builder(activity).setPositiveButton(R.string.yes, (d, which) -> {
                Utils.dataBox.delFavorite(favoriteModel);
                favoritesAdapter.setItems(Utils.dataBox.getAllFavorites());
            })
                                             .setNegativeButton(R.string.no, null).setMessage(getString(R.string.quick_access_confirm_delete,
                                                                                                        favoriteModel.getQuery())).show();

        } else if (tag instanceof DataBox.CookieModel) {
            final DataBox.CookieModel cookieModel = (DataBox.CookieModel) tag;

            if (cookieModel.isSelected())
                Toast.makeText(v.getContext(), R.string.quick_access_cannot_delete_curr, Toast.LENGTH_SHORT).show();
            else
                new AlertDialog.Builder(activity)
                        .setMessage(getString(R.string.quick_access_confirm_delete, cookieModel.getUsername()))
                        .setPositiveButton(R.string.yes, (d, which) -> {
                            Utils.dataBox.delUserCookie(cookieModel);
                            rvQuickAccess.findViewWithTag(cookieModel).setVisibility(View.GONE);
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
        }

        return true;
    }

    @Override
    public void onDismiss(@NonNull final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (cookieChanged && activity != null) activity.recreate();
    }

    @Override
    public void onShow(final DialogInterface dialog) {
        if (settingsHelper.getBoolean(Constants.SHOW_QUICK_ACCESS_DIALOG))
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.quick_access_info_dialog)
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.dont_show_again, (d, which) ->
                            settingsHelper.putBoolean(Constants.SHOW_QUICK_ACCESS_DIALOG, false)).show();
    }
}