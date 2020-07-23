package awais.instagrabber.directdownload;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import awais.instagrabber.R;
import awais.instagrabber.activities.BaseLanguageActivity;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.ViewerPostModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.models.enums.DownloadMethod;

public final class MultiDirectDialog extends BaseLanguageActivity {
    public final ArrayList<BasePostModel> selectedItems = new ArrayList<>();
    private PostsAdapter postsAdapter;
    private MenuItem btnDownload;
    private String username = null;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_direct);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ViewerPostModel[] postModels;
        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_POST)
                || (postModels = (ViewerPostModel[]) intent.getSerializableExtra(Constants.EXTRAS_POST)) == null) {
            Utils.errorFinish(this);
            return;
        }

        username = postModels[0].getUsername();
        toolbar.setTitle(username);
        toolbar.setSubtitle(postModels[0].getShortCode());

        final RecyclerView recyclerView = findViewById(R.id.mainPosts);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new GridAutofitLayoutManager(this, Utils.convertDpToPx(130)));
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));

        final ArrayList<PostModel> models = new ArrayList<>(postModels.length - 1);
        for (final ViewerPostModel postModel : postModels)
            models.add(new PostModel(postModel.getItemType(), postModel.getPostId(), postModel.getDisplayUrl(),
                    postModel.getSliderDisplayUrl(), postModel.getShortCode(), postModel.getPostCaption(), postModel.getTimestamp(),
                    postModel.getLike(), postModel.getBookmark()));

        postsAdapter = new PostsAdapter(models, v -> {
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                final PostModel postModel = (PostModel) tag;
                if (postsAdapter.isSelecting) toggleSelection(postModel);
                else {
                    Utils.batchDownload(this, username, DownloadMethod.DOWNLOAD_DIRECT, Collections.singletonList(postModel));
                    finish();
                }
            }
        }, v -> {
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                postsAdapter.isSelecting = true;
                toggleSelection((PostModel) tag);
            }
            return true;
        });

        recyclerView.setAdapter(postsAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        Utils.batchDownload(this, username, DownloadMethod.DOWNLOAD_DIRECT, selectedItems);
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        btnDownload = menu.findItem(R.id.action_download);
        menu.findItem(R.id.action_search).setVisible(false);
        return true;
    }

    private void toggleSelection(final PostModel postModel) {
        if (postModel != null && postsAdapter != null) {
            if (postModel.isSelected()) selectedItems.remove(postModel);
            else selectedItems.add(postModel);
            postModel.setSelected(!postModel.isSelected());
            notifyAdapter(postModel);
        }
    }

    private void notifyAdapter(final PostModel postModel) {
        if (selectedItems.size() < 1) postsAdapter.isSelecting = false;
        if (postModel.getPosition() < 0) postsAdapter.notifyDataSetChanged();
        else postsAdapter.notifyItemChanged(postModel.getPosition(), postModel);

        if (btnDownload != null) btnDownload.setVisible(postsAdapter.isSelecting);
    }
}