package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemLinkContext {
    private final String linkUrl;
    private final String linkTitle;
    private final String linkSummary;
    private final String linkImageUrl;

    public DirectItemLinkContext(final String linkUrl,
                                 final String linkTitle,
                                 final String linkSummary,
                                 final String linkImageUrl) {
        this.linkUrl = linkUrl;
        this.linkTitle = linkTitle;
        this.linkSummary = linkSummary;
        this.linkImageUrl = linkImageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public String getLinkSummary() {
        return linkSummary;
    }

    public String getLinkImageUrl() {
        return linkImageUrl;
    }
}
