package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemLink {
    private final String text;
    private final DirectItemLinkContext linkContext;
    private final String clientContext;
    private final String mutationToken;

    public DirectItemLink(final String text,
                          final DirectItemLinkContext linkContext,
                          final String clientContext,
                          final String mutationToken) {
        this.text = text;
        this.linkContext = linkContext;
        this.clientContext = clientContext;
        this.mutationToken = mutationToken;
    }

    public String getText() {
        return text;
    }

    public DirectItemLinkContext getLinkContext() {
        return linkContext;
    }

    public String getClientContext() {
        return clientContext;
    }

    public String getMutationToken() {
        return mutationToken;
    }
}
