package awais.instagrabber.customviews.helpers;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public final class RecyclerLazyLoaderAtBottom extends RecyclerView.OnScrollListener {

    @NonNull
    private final RecyclerView.LayoutManager layoutManager;
    private final LazyLoadListener lazyLoadListener;
    private int currentPage;
    private int previousItemCount;
    private boolean loading;

    public RecyclerLazyLoaderAtBottom(@NonNull final RecyclerView.LayoutManager layoutManager,
                                      final LazyLoadListener lazyLoadListener) {
        this.layoutManager = layoutManager;
        this.lazyLoadListener = lazyLoadListener;
    }

    @Override
    public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        final int itemCount = layoutManager.getItemCount();
        if (itemCount > previousItemCount) {
            loading = false;
        }
        if (!recyclerView.canScrollVertically(RecyclerView.SCROLL_AXIS_HORIZONTAL) && newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (!loading && lazyLoadListener != null) {
                loading = true;
                new Handler().postDelayed(() -> lazyLoadListener.onLoadMore(++currentPage), 500);
            }
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void resetState() {
        currentPage = 0;
        previousItemCount = 0;
        loading = true;
    }

    public interface LazyLoadListener {
        void onLoadMore(final int page);
    }
}