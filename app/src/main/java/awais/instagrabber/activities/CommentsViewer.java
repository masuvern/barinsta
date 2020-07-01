package awais.instagrabber.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.asyncs.CommentsFetcher;
import awais.instagrabber.databinding.ActivityCommentsBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class CommentsViewer extends AppCompatActivity {
    private CommentsAdapter commentsAdapter;
    private CommentModel commentModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityCommentsBinding commentsBinding = ActivityCommentsBinding.inflate(getLayoutInflater());
        setContentView(commentsBinding.getRoot());

        final String shortCode;
        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_SHORTCODE)
                || Utils.isEmpty((shortCode = intent.getStringExtra(Constants.EXTRAS_SHORTCODE)))) {
            Utils.errorFinish(this);
            return;
        }

        setSupportActionBar(commentsBinding.toolbar.toolbar);
        commentsBinding.toolbar.toolbar.setTitle(R.string.title_comments);
        commentsBinding.toolbar.toolbar.setSubtitle(shortCode);

        final Resources resources = getResources();

        final ArrayAdapter<String> commmentDialogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new String[]{resources.getString(R.string.open_profile),
                        resources.getString(R.string.view_pfp),
                        resources.getString(R.string.comment_viewer_copy_user),
                        resources.getString(R.string.comment_viewer_copy_comment)});
        final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
            final ProfileModel profileModel = commentModel.getProfileModel();

            if (which == 0) {
                searchUsername(profileModel.getUsername());
            } else if (which == 1) {
                startActivity(new Intent(this, ProfileViewer.class).putExtra(Constants.EXTRAS_PROFILE, profileModel));
            } else if (which == 2) {
                Utils.copyText(this, profileModel.getUsername());
            } else if (which == 3) {
                Utils.copyText(this, commentModel.getText().toString());
            }
        };

        final View.OnClickListener clickListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof CommentModel) {
                commentModel = (CommentModel) tag;

                final String username = commentModel.getProfileModel().getUsername();
                final SpannableString title = new SpannableString(username + ":\n" + commentModel.getText());
                title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                new AlertDialog.Builder(this).setTitle(title)
                        .setAdapter(commmentDialogAdapter, profileDialogListener)
                        .setNeutralButton(R.string.cancel, null)
                        .show();
            }
        };

        final MentionClickListener mentionClickListener = (view, text, isHashtag) ->
                new AlertDialog.Builder(this).setTitle(text)
                        .setMessage(isHashtag ? R.string.comment_view_mention_hash_search : R.string.comment_view_mention_user_search)
                        .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok,
                        (dialog, which) -> searchUsername(text)).show();

        new CommentsFetcher(shortCode, new FetchListener<CommentModel[]>() {
            @Override
            public void doBefore() {
                commentsBinding.toolbar.progressCircular.setVisibility(View.VISIBLE);
            }

            @Override
            public void onResult(final CommentModel[] commentModels) {
                commentsBinding.toolbar.progressCircular.setVisibility(View.GONE);

                commentsAdapter = new CommentsAdapter(commentModels, true, clickListener, mentionClickListener);

                commentsBinding.rvComments.setAdapter(commentsAdapter);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void searchUsername(final String text) {
        if (Main.scanHack != null) {
            Main.scanHack.onResult(text);
            setResult(6969);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.follow, menu);

        final MenuItem menuSearch = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                if (commentsAdapter != null) commentsAdapter.getFilter().filter(query);
                return true;
            }
        });

        menu.findItem(R.id.action_compare).setVisible(false);

        return true;
    }
}