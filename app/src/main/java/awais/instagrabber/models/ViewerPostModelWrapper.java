package awais.instagrabber.models;

import java.util.List;

public class ViewerPostModelWrapper {
    private int position;
    private List<PostChild> viewerPostModels;

    public ViewerPostModelWrapper(final int position, final List<PostChild> viewerPostModels) {
        this.position = position;
        this.viewerPostModels = viewerPostModels;
    }

    public int getPosition() {
        return position;
    }

    public List<PostChild> getViewerPostModels() {
        return viewerPostModels;
    }

    public void setViewerPostModels(final List<PostChild> viewerPostModels) {
        this.viewerPostModels = viewerPostModels;
    }
}
