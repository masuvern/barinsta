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
    private static final String igVersion = "177.0.0.30.119";
    private static final String igVersionCode = "276028050";
    // only pick the ones that has width 1440 for maximum download quality
    public static final String[] devices = {
            // https://github.com/dilame/instagram-private-api/blob/master/src/samples/devices.json
            "26/8.0.0; 640dpi; 1440x2768; samsung; SM-G950F; dreamlte; samsungexynos8895",
            "26/8.0.0; 560dpi; 1440x2792; samsung; SM-G955F; dream2lte; samsungexynos8895",
            "24/7.0; 640dpi; 1440x2560; samsung; SM-G925F; zerolte; samsungexynos7420",
            "24/7.0; 640dpi; 1440x2560; samsung; SM-G920F; zeroflte; samsungexynos7420",
            // https://github.com/mimmi20/BrowserDetector/tree/master
            "28/9; 560dpi; 1440x2792; samsung; SM-N960F; crownlte; samsungexynos9810",
            // mgp25
            "23/6.0.1; 640dpi; 1440x2392; LGE/lge; RS988; h1; h1",
            "24/7.0; 640dpi; 1440x2560; HUAWEI; LON-L29; HWLON; hi3660",
            "23/6.0.1; 640dpi; 1440x2560; ZTE; ZTE A2017U; ailsa_ii; qcom",
            "23/6.0.1; 640dpi; 1440x2560; samsung; SM-G935F; hero2lte; samsungexynos8890",
            "23/6.0.1; 640dpi; 1440x2560; samsung; SM-G930F; herolte; samsungexynos8890"
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
