package awais.instagrabber.interfaces;

import awais.instagrabber.customviews.RamboTextView;

@Deprecated
public interface MentionClickListener {
    void onClick(final RamboTextView view,
                 final String text,
                 final boolean isHashtag,
                 final boolean isLocation);
}