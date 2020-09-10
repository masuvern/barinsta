package awais.instagrabber.utils;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;

import androidx.annotation.NonNull;

import awais.instagrabber.customviews.CommentMentionClickSpan;

public final class TextUtils {
    @NonNull
    public static CharSequence getMentionText(@NonNull final CharSequence text) {
        final int commentLength = text.length();
        final SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text, 0, commentLength);

        for (int i = 0; i < commentLength; ++i) {
            char currChar = text.charAt(i);

            if (currChar == '@' || currChar == '#') {
                final int startLen = i;

                do {
                    if (++i == commentLength) break;
                    currChar = text.charAt(i);

                    if (currChar == '.' && i + 1 < commentLength) {
                        final char nextChar = text.charAt(i + 1);
                        if (nextChar == '.' || nextChar == ' ' || nextChar == '#' || nextChar == '@' || nextChar == '/'
                                || nextChar == '\r' || nextChar == '\n') {
                            break;
                        }
                    } else if (currChar == '.')
                        break;

                    // for merged hashtags
                    if (currChar == '#') {
                        --i;
                        break;
                    }
                } while (currChar != ' ' && currChar != '\r' && currChar != '\n' && currChar != '>' && currChar != '<'
                        && currChar != ':' && currChar != ';' && currChar != '\'' && currChar != '"' && currChar != '['
                        && currChar != ']' && currChar != '\\' && currChar != '=' && currChar != '-' && currChar != '!'
                        && currChar != '$' && currChar != '%' && currChar != '^' && currChar != '&' && currChar != '*'
                        && currChar != '(' && currChar != ')' && currChar != '{' && currChar != '}' && currChar != '/'
                        && currChar != '|' && currChar != '?' && currChar != '`' && currChar != '~'
                );

                final int endLen = currChar != '#' ? i : i + 1; // for merged hashtags
                stringBuilder.setSpan(new CommentMentionClickSpan(), startLen,
                                      Math.min(commentLength, endLen), // fixed - crash when end index is greater than comment length ( @kernoeb )
                                      Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }

        return stringBuilder;
    }

    // extracted from String class
    public static int indexOfChar(@NonNull final CharSequence sequence, final int ch, final int startIndex) {
        final int max = sequence.length();
        if (startIndex < max) {
            if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                for (int i = startIndex; i < max; i++) if (sequence.charAt(i) == ch) return i;
            } else if (Character.isValidCodePoint(ch)) {
                final char hi = (char) ((ch >>> 10) + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
                final char lo = (char) ((ch & 0x3ff) + Character.MIN_LOW_SURROGATE);
                for (int i = startIndex; i < max; i++)
                    if (sequence.charAt(i) == hi && sequence.charAt(i + 1) == lo) return i;
            }
        }
        return -1;
    }

    public static boolean hasMentions(final CharSequence text) {
        if (isEmpty(text)) return false;
        return indexOfChar(text, '@', 0) != -1 || indexOfChar(text, '#', 0) != -1;
    }

    public static CharSequence getSpannableUrl(final String url) {
        if (isEmpty(url)) return url;
        final int httpIndex = url.indexOf("http:");
        final int httpsIndex = url.indexOf("https:");
        if (httpIndex == -1 && httpsIndex == -1) return url;

        final int length = url.length();

        final int startIndex = httpIndex != -1 ? httpIndex : httpsIndex;
        final int spaceIndex = url.indexOf(' ', startIndex + 1);

        final int endIndex = (spaceIndex != -1 ? spaceIndex : length);

        final String extractUrl = url.substring(startIndex, Math.min(length, endIndex));

        final SpannableString spannableString = new SpannableString(url);
        spannableString.setSpan(new URLSpan(extractUrl), startIndex, endIndex, 0);

        return spannableString;
    }

    public static boolean isEmpty(final CharSequence charSequence) {
        if (charSequence == null || charSequence.length() < 1) return true;
        if (charSequence instanceof String) {
            String str = (String) charSequence;
            if ("".equals(str) || "null".equals(str) || str.isEmpty()) return true;
            str = str.trim();
            return "".equals(str) || "null".equals(str) || str.isEmpty();
        }
        return "null".contentEquals(charSequence) || "".contentEquals(charSequence) || charSequence.length() < 1;
    }
}
