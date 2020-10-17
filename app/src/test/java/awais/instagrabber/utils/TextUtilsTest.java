package awais.instagrabber.utils;

import org.junit.jupiter.api.Assertions;

class TextUtilsTest {

    @org.junit.jupiter.api.Test
    void testMillisToTimeString() {
        String timeString = TextUtils.millisToTimeString(18000000);
        Assertions.assertEquals("05:00:00", timeString);

        timeString = TextUtils.millisToTimeString(300000);
        Assertions.assertEquals("05:00", timeString);

        timeString = TextUtils.millisToTimeString(300000, true);
        Assertions.assertEquals("00:05:00", timeString);
    }
}