package awais.instagrabber.interfaces;

import awais.instagrabber.customviews.RamboTextView;

public interface MentionClickListener {
    void onClick(final RamboTextView view, final String text, final boolean isHashtag);
}