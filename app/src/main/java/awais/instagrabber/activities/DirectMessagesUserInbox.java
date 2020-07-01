package awais.instagrabber.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.MessageItemsAdapter;
import awais.instagrabber.asyncs.direct_messages.UserInboxFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.ActivityDmsBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.direct_messages.DirectItemModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.UserInboxDirection;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class DirectMessagesUserInbox extends AppCompatActivity {
    private final ArrayList<ProfileModel> users = new ArrayList<>();
    private final ArrayList<DirectItemModel> directItemModels = new ArrayList<>();
    private final FetchListener<InboxThreadModel> fetchListener = new FetchListener<InboxThreadModel>() {
        @Override
        public void doBefore() {
            dmsBinding.swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final InboxThreadModel result) {
            if (result == null && "MINCURSOR".equals(endCursor) || "MAXCURSOR".equals(endCursor) || Utils.isEmpty(endCursor))
                Toast.makeText(DirectMessagesUserInbox.this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();

            if (result != null) {
                endCursor = result.getPrevCursor();
                if ("MINCURSOR".equals(endCursor) || "MAXCURSOR".equals(endCursor)) endCursor = null;

                users.clear();
                users.addAll(Arrays.asList(result.getUsers()));

                final int oldSize = directItemModels.size();
                final List<DirectItemModel> itemModels = Arrays.asList(result.getItems());
                directItemModels.addAll(itemModels);
                messageItemsAdapter.notifyItemRangeInserted(oldSize, itemModels.size());
            }

            dmsBinding.swipeRefreshLayout.setRefreshing(false);
        }
    };
    private String endCursor;
    private ActivityDmsBinding dmsBinding;
    private MessageItemsAdapter messageItemsAdapter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dmsBinding = ActivityDmsBinding.inflate(getLayoutInflater());
        setContentView(dmsBinding.getRoot());

        final InboxThreadModel threadModel;
        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_THREAD_MODEL) ||
                (threadModel = (InboxThreadModel) intent.getSerializableExtra(Constants.EXTRAS_THREAD_MODEL)) == null) {
            Utils.errorFinish(this);
            return;
        }

        dmsBinding.swipeRefreshLayout.setEnabled(false);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, true);
        dmsBinding.rvDirectMessages.setLayoutManager(layoutManager);

        dmsBinding.rvDirectMessages.addOnScrollListener(new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!Utils.isEmpty(endCursor)) {
                new UserInboxFetcher(threadModel.getThreadId(), UserInboxDirection.OLDER,
                        endCursor, fetchListener).execute(); // serial because we don't want messages to be randomly ordered
            }
        }));

        dmsBinding.rvDirectMessages.setAdapter(messageItemsAdapter = new MessageItemsAdapter(directItemModels, users, v -> {
            // todo do something with clicked message
            Log.d("AWAISKING_APP", "--> " + v.getTag());
        }, (view, text, isHashtag) -> {
            // todo mention click stuff

        }));

        new UserInboxFetcher(threadModel.getThreadId(), UserInboxDirection.OLDER, null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}