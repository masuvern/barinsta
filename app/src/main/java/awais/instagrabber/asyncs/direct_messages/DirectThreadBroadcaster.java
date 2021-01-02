// package awais.instagrabber.asyncs.direct_messages;
//
// import android.os.AsyncTask;
// import android.util.Log;
//
// import org.json.JSONObject;
//
// import java.io.BufferedReader;
// import java.io.DataOutputStream;
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.net.HttpURLConnection;
// import java.net.URL;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.UUID;
//
// import awais.instagrabber.repositories.requests.directmessages.BroadcastOptions;
// import awais.instagrabber.repositories.responses.directmessages.DirectThreadBroadcastResponse;
// import awais.instagrabber.utils.Constants;
// import awais.instagrabber.utils.CookieUtils;
// import awais.instagrabber.utils.Utils;
//
// import static awais.instagrabber.utils.Utils.settingsHelper;
//
// public class DirectThreadBroadcaster extends AsyncTask<BroadcastOptions, Void, DirectThreadBroadcastResponse> {
//     private static final String TAG = "DirectThreadBroadcaster";
//
//     private final String threadId;
//
//     private OnBroadcastCompleteListener listener;
//
//     public DirectThreadBroadcaster(String threadId) {
//         this.threadId = threadId;
//     }
//
//     @Override
//     protected DirectThreadBroadcastResponse doInBackground(final BroadcastOptions... broadcastOptionsArray) {
//         if (broadcastOptionsArray == null || broadcastOptionsArray.length == 0 || broadcastOptionsArray[0] == null) {
//             return null;
//         }
//         final BroadcastOptions broadcastOptions = broadcastOptionsArray[0];
//         final String cookie = settingsHelper.getString(Constants.COOKIE);
//         final String cc = UUID.randomUUID().toString();
//         final Map<String, String> form = new HashMap<>();
//         form.put("_csrftoken", CookieUtils.getCsrfTokenFromCookie(cookie));
//         form.put("_uid", CookieUtils.getUserIdFromCookie(cookie));
//         form.put("__uuid", settingsHelper.getString(Constants.DEVICE_UUID));
//         form.put("client_context", cc);
//         form.put("mutation_token", cc);
//         form.putAll(broadcastOptions.getFormMap());
//         form.put("thread_id", threadId);
//         form.put("action", "send_item");
//         final String message = new JSONObject(form).toString();
//         final String content = Utils.sign(message);
//         final String url = "https://i.instagram.com/api/v1/direct_v2/threads/broadcast/" + broadcastOptions.getItemType().getValue() + "/";
//         HttpURLConnection connection = null;
//         DataOutputStream outputStream = null;
//         BufferedReader r = null;
//         try {
//             connection = (HttpURLConnection) new URL(url).openConnection();
//             connection.setRequestMethod("POST");
//             connection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
//             connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//             if (content != null) {
//                 connection.setRequestProperty("Content-Length", "" + content.getBytes().length);
//             }
//             connection.setUseCaches(false);
//             connection.setDoOutput(true);
//             outputStream = new DataOutputStream(connection.getOutputStream());
//             outputStream.writeBytes(content);
//             outputStream.flush();
//             final int responseCode = connection.getResponseCode();
//             if (responseCode != HttpURLConnection.HTTP_OK) {
//                 Log.d(TAG, responseCode + ": " + content + ": " + cookie);
//                 return new DirectThreadBroadcastResponse(responseCode, null);
//             }
//             r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//             final StringBuilder builder = new StringBuilder();
//             for (String line = r.readLine(); line != null; line = r.readLine()) {
//                 if (builder.length() != 0) {
//                     builder.append("\n");
//                 }
//                 builder.append(line);
//             }
//             return new DirectThreadBroadcastResponse(responseCode, new JSONObject(builder.toString()));
//         } catch (Exception e) {
//             Log.e(TAG, "Error", e);
//         } finally {
//             if (r != null) {
//                 try {
//                     r.close();
//                 } catch (IOException ignored) {
//                 }
//             }
//             if (outputStream != null) {
//                 try {
//                     outputStream.close();
//                 } catch (IOException ignored) {
//                 }
//             }
//             if (connection != null) {
//                 connection.disconnect();
//             }
//         }
//         return null;
//     }
//
//     @Override
//     protected void onPostExecute(final DirectThreadBroadcastResponse result) {
//         if (listener != null) {
//             listener.onTaskComplete(result);
//         }
//     }
//
//     public void setOnTaskCompleteListener(final OnBroadcastCompleteListener listener) {
//         if (listener != null) {
//             this.listener = listener;
//         }
//     }
//
//     public interface OnBroadcastCompleteListener {
//         void onTaskComplete(DirectThreadBroadcastResponse response);
//     }
// }
