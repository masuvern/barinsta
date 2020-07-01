package awais.instagrabber.customviews.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.interfaces.LazyLoadListener;

// thanks to nesquena's EndlessRecyclerViewScrollListener
//   https://gist.github.com/nesquena/d09dc68ff07e845cc622
public final class RecyclerLazyLoader extends RecyclerView.OnScrollListener {
    private int currentPage = 0;            // The current offset index of data you have loaded
    private int previousTotalItemCount = 0; // The total number of items in the dataset after the last load
    private boolean loading = true;         // True if we are still waiting for the last set of data to load.
    private final int visibleThreshold;     // The minimum amount of items to have below your current scroll position before loading more.
    private final LazyLoadListener lazyLoadListener;
    private final RecyclerView.LayoutManager layoutManager;

    public RecyclerLazyLoader(@NonNull final RecyclerView.LayoutManager layoutManager, final LazyLoadListener lazyLoadListener) {
        this.layoutManager = layoutManager;
        this.lazyLoadListener = lazyLoadListener;
        if (layoutManager instanceof GridLayoutManager) {
            this.visibleThreshold = 5 * Math.max(3, ((GridLayoutManager) layoutManager).getSpanCount());
        } else if (layoutManager instanceof LinearLayoutManager) {
            this.visibleThreshold = ((LinearLayoutManager) layoutManager).getReverseLayout() ? 4 : 8;
        } else {
            this.visibleThreshold = 5;
        }
    }

    @Override
    public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
        final int totalItemCount = layoutManager.getItemCount();

        if (totalItemCount < previousTotalItemCount) {
            currentPage = 0;
            previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) loading = true;
        }

        if (loading && totalItemCount > previousTotalItemCount) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }

        final int lastVisibleItemPosition;
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager layoutManager = (GridLayoutManager) this.layoutManager;
            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        } else {
            final LinearLayoutManager layoutManager = (LinearLayoutManager) this.layoutManager;
            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        }

        if (!loading && lastVisibleItemPosition + visibleThreshold > totalItemCount) {
            if (lazyLoadListener != null) lazyLoadListener.onLoadMore(++currentPage, totalItemCount);
            loading = true;
        }
    }

    public void resetState() {
        this.currentPage = 0;
        this.previousTotalItemCount = 0;
        this.loading = true;
    }
}