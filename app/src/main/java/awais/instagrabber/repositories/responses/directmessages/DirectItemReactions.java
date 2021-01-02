package awais.instagrabber.repositories.responses.directmessages;

import java.util.List;

public class DirectItemReactions {
    private final List<DirectItemEmojiReaction> emojis;
    private final List<DirectItemEmojiReaction> likes;

    public DirectItemReactions(final List<DirectItemEmojiReaction> emojis,
                               final List<DirectItemEmojiReaction> likes) {
        this.emojis = emojis;
        this.likes = likes;
    }

    public List<DirectItemEmojiReaction> getEmojis() {
        return emojis;
    }

    public List<DirectItemEmojiReaction> getLikes() {
        return likes;
    }
}
