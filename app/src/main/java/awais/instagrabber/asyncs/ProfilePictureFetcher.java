package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;
import awais.instagrabber.models.enums.ProfilePictureFetchMode;
import static awais.instagrabber.utils.Utils.logCollector;

public final class ProfilePictureFetcher extends AsyncTask<Void, Void, String> {
    private final FetchListener<String> fetchListener;
    private final String userName, userId;
    private final ProfilePictureFetchMode fetchMode;

    public ProfilePictureFetcher(final String userName, final String userId, final FetchListener<String> fetchListener,
                                 final ProfilePictureFetchMode fetchMode) {
        this.fetchListener = fetchListener;
        this.fetchMode = fetchMode;
        this.userName = userName;
        this.userId = userId;
    }

    @Override
    protected String doInBackground(final Void... voids) {
        String out = null;
        try {
            final String url;

            if (fetchMode == ProfilePictureFetchMode.INSTADP)
                url = "https://instadp.com/fullsize/" + userName;
            else if (fetchMode == ProfilePictureFetchMode.INSTA_STALKER)
                url = "https://insta-stalker.co/instadp_fullsize/?id=" + userName;
            else // select from s1, s2, s3 but s1 works fine
                url = "https://instafullsize.com/ifsapi/ig/photo/s1/" + userName + "?igid=" + userId;

            // prolly http://167.99.85.4/instagram/userid?profile-url=the.badak

            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);

            if (fetchMode == ProfilePictureFetchMode.INSTAFULLSIZE) {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "fjgt842ff582a");
            }

            final String result = conn.getResponseCode() == HttpURLConnection.HTTP_OK ? Utils.readFromConnection(conn) : null;
            conn.disconnect();

            if (!Utils.isEmpty(result)) {
                final Document doc = Jsoup.parse(result);
                boolean fallback = false;

                if (fetchMode == ProfilePictureFetchMode.INSTADP) {
                    final int imgIndex = result.indexOf("preloadImg('"), lastIndex;

                    Element element = doc.selectFirst(".instadp");
                    if (element != null && (element = element.selectFirst(".picture")) != null)
                        out = element.attr("src");
                    else if ((element = doc.selectFirst(".download-btn")) != null)
                        out = element.attr("href");
                    else if (imgIndex != -1 && (lastIndex = result.indexOf("')", imgIndex)) != -1)
                        out = result.substring(imgIndex + 12, lastIndex);
                    else fallback = true;

                } else if (fetchMode == ProfilePictureFetchMode.INSTAFULLSIZE) {
                    try {
                        final JSONObject object = new JSONObject(result);
                        out = object.getString("result");
                    } catch (final Exception e) {
                        if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                        fallback = true;
                    }

                } else {
                    final Elements elements = doc.select("img[data-src]");
                    if (elements.size() > 0) out = elements.get(0).attr("data-src");
                    else fallback = true;
                }

                if (fallback) {
                    final Elements imgs = doc.getElementsByTag("img");
                    for (final Element img : imgs) {
                        final String imgStr = img.toString();
                        if (imgStr.contains("cdninstagram.com")) return img.attr("src");
                    }
                }
            }
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_PROFILE_PICTURE_FETCHER, "doInBackground",
                        new Pair<>("fetchMode", fetchMode));
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return out;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final String result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}