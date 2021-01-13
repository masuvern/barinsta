package awais.instagrabber.utils;

import androidx.annotation.NonNull;

public class UserAgentUtils {

    /* GraphQL user agents (which are just standard browser UA's).
     * Go to https://www.whatismybrowser.com/guides/the-latest-user-agent/ to update it
     * Windows first (Assume win64 not wow64): Chrome, Firefox, Edge
     * Then macOS: Chrome, Firefox, Safari
     */
    public static final String[] browsers = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36 Edg/87.0.664.75",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 11.1; rv:84.0) Gecko/20100101 Firefox/84.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.2 Safari/605.1.15"
    };
    // use APKpure, assume x86
    private static final String igVersion = "169.3.0.30.135";
    private static final String igVersionCode = "264009054";
    // https://github.com/dilame/instagram-private-api/blob/master/src/samples/devices.json
    // presumed constant, so no need to update
    public static final String[] devices = {
            "25/7.1.1; 440dpi; 1080x1920; Xiaomi; Mi Note 3; jason; qcom",
            "23/6.0.1; 480dpi; 1080x1920; Xiaomi; Redmi Note 3; kenzo; qcom",
            "23/6.0; 480dpi; 1080x1920; Xiaomi; Redmi Note 4; nikel; mt6797",
            "24/7.0; 480dpi; 1080x1920; Xiaomi/xiaomi; Redmi Note 4; mido; qcom",
            "23/6.0; 480dpi; 1080x1920; Xiaomi; Redmi Note 4X; nikel; mt6797",
            "27/8.1.0; 440dpi; 1080x2030; Xiaomi/xiaomi; Redmi Note 5; whyred; qcom",
            "23/6.0.1; 480dpi; 1080x1920; Xiaomi; Redmi 4; markw; qcom",
            "27/8.1.0; 440dpi; 1080x2030; Xiaomi/xiaomi; Redmi 5 Plus; vince; qcom",
            "25/7.1.2; 440dpi; 1080x2030; Xiaomi/xiaomi; Redmi 5 Plus; vince; qcom",
            "26/8.0.0; 480dpi; 1080x1920; Xiaomi; MI 5; gemini; qcom",
            "27/8.1.0; 480dpi; 1080x1920; Xiaomi/xiaomi; Mi A1; tissot_sprout; qcom",
            "26/8.0.0; 480dpi; 1080x1920; Xiaomi; MI 6; sagit; qcom",
            "25/7.1.1; 440dpi; 1080x1920; Xiaomi; MI MAX 2; oxygen; qcom",
            "24/7.0; 480dpi; 1080x1920; Xiaomi; MI 5s; capricorn; qcom",
            "26/8.0.0; 480dpi; 1080x1920; samsung; SM-A520F; a5y17lte; samsungexynos7880",
            "26/8.0.0; 480dpi; 1080x2076; samsung; SM-G950F; dreamlte; samsungexynos8895",
            "26/8.0.0; 640dpi; 1440x2768; samsung; SM-G950F; dreamlte; samsungexynos8895",
            "26/8.0.0; 420dpi; 1080x2094; samsung; SM-G955F; dream2lte; samsungexynos8895",
            "26/8.0.0; 560dpi; 1440x2792; samsung; SM-G955F; dream2lte; samsungexynos8895",
            "24/7.0; 480dpi; 1080x1920; samsung; SM-A510F; a5xelte; samsungexynos7580",
            "26/8.0.0; 480dpi; 1080x1920; samsung; SM-G930F; herolte; samsungexynos8890",
            "26/8.0.0; 480dpi; 1080x1920; samsung; SM-G935F; hero2lte; samsungexynos8890",
            "26/8.0.0; 420dpi; 1080x2094; samsung; SM-G965F; star2lte; samsungexynos9810",
            "26/8.0.0; 480dpi; 1080x2076; samsung; SM-A530F; jackpotlte; samsungexynos7885",
            "24/7.0; 640dpi; 1440x2560; samsung; SM-G925F; zerolte; samsungexynos7420",
            "26/8.0.0; 420dpi; 1080x1920; samsung; SM-A720F; a7y17lte; samsungexynos7880",
            "24/7.0; 640dpi; 1440x2560; samsung; SM-G920F; zeroflte; samsungexynos7420",
            "24/7.0; 420dpi; 1080x1920; samsung; SM-J730FM; j7y17lte; samsungexynos7870",
            "26/8.0.0; 480dpi; 1080x2076; samsung; SM-G960F; starlte; samsungexynos9810",
            "26/8.0.0; 420dpi; 1080x2094; samsung; SM-N950F; greatlte; samsungexynos8895",
            "26/8.0.0; 420dpi; 1080x2094; samsung; SM-A730F; jackpot2lte; samsungexynos7885",
            "26/8.0.0; 420dpi; 1080x2094; samsung; SM-A605FN; a6plte; qcom",
            "26/8.0.0; 480dpi; 1080x1920; HUAWEI/HONOR; STF-L09; HWSTF; hi3660",
            "27/8.1.0; 480dpi; 1080x2280; HUAWEI/HONOR; COL-L29; HWCOL; kirin970",
            "26/8.0.0; 480dpi; 1080x2032; HUAWEI/HONOR; LLD-L31; HWLLD-H; hi6250",
            "26/8.0.0; 480dpi; 1080x2150; HUAWEI; ANE-LX1; HWANE; hi6250",
            "26/8.0.0; 480dpi; 1080x2032; HUAWEI; FIG-LX1; HWFIG-H; hi6250",
            "27/8.1.0; 480dpi; 1080x2150; HUAWEI/HONOR; COL-L29; HWCOL; kirin970",
            "26/8.0.0; 480dpi; 1080x2038; HUAWEI/HONOR; BND-L21; HWBND-H; hi6250",
            "23/6.0.1; 420dpi; 1080x1920; LeMobile/LeEco; Le X527; le_s2_ww; qcom"
    };

    @NonNull
    public static String generateBrowserUA(final int code) {
        return browsers[code - 1];
    }

    @NonNull
    public static String generateAppUA(final int code, final String lang) {
        return "Instagram " + igVersion + " Android (" + devices[code] + "; " + lang + "; " + igVersionCode + ")";
    }
}
