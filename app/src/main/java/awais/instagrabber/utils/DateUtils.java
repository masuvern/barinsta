package awais.instagrabber.utils;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Locale;

public final class DateUtils {
    public static int getTimezoneOffset() {
        final Calendar calendar = Calendar.getInstance(Locale.getDefault());
        return -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
    }

    public static boolean isBeforeOrEqual(@NonNull final LocalDateTime localDateTime, @NonNull final LocalDateTime comparedTo) {
        return localDateTime.isBefore(comparedTo) || localDateTime.isEqual(comparedTo);
    }
}
