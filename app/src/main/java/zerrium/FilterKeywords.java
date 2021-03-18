package zerrium;

import java.util.ArrayList;
import java.util.Arrays;

public class FilterKeywords {
    private static final ArrayList<String> FILTER_KEYWORDS = new ArrayList<>();

    public static boolean append(String keyword){
        if(keyword == null) return false;
        FILTER_KEYWORDS.add(keyword);
        return true;
    }

    public static boolean insert(String[] keywords){
        if(keywords == null) return false;
        FILTER_KEYWORDS.addAll(Arrays.asList(keywords));
        return true;
    }

    public static boolean filter(String word){
        if(word == null) return false;
        for(String s:FILTER_KEYWORDS){
            if(word.contains(s)) return true;
        }
        return false;
    }
}
