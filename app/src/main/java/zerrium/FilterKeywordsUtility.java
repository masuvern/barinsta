package zerrium;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class FilterKeywordsUtility {
    private static final ArrayList<String> FILTER_KEYWORDS = new ArrayList<>();

    public static boolean append(final String keyword){
        if(keyword == null) return false;
        FILTER_KEYWORDS.add(keyword);
        return true;
    }

    public static boolean insert(final String[] keywords){
        if(keywords == null) return false;
        FILTER_KEYWORDS.addAll(Arrays.asList(keywords));
        return true;
    }

    public static boolean filter(String caption){
        if(caption == null) return false;
        caption = caption.toLowerCase(Locale.getDefault());
        for(final String s:FILTER_KEYWORDS){
            if(caption.contains(s)) return true;
        }
        return false;
    }
}
