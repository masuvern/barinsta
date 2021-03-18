package zerrium;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class FilterKeywordsUtility {
    private static final List<String> FILTER_KEYWORDS = new ArrayList<>();

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
        String temp = caption.toLowerCase(Locale.getDefault());
        for(final String s:FILTER_KEYWORDS){
            if(temp.contains(s)) return true;
        }
        return false;
    }
}
