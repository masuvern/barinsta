package awais.instagrabber.models;

public class ViewerPostModelWrapper {
    private int position;
    private ViewerPostModel[] viewerPostModels;

    public ViewerPostModelWrapper(final int position, final ViewerPostModel[] viewerPostModels) {
        this.position = position;
        this.viewerPostModels = viewerPostModels;
    }

    public int getPosition() {
        return position;
    }

    public ViewerPostModel[] getViewerPostModels() {
        return viewerPostModels;
    }

    public void setViewerPostModels(final ViewerPostModel[] viewerPostModels) {
        this.viewerPostModels = viewerPostModels;
    }
}
