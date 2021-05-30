package awais.instagrabber.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import awais.instagrabber.models.UploadVideoOptions
import awais.instagrabber.utils.BitmapUtils.ThumbnailLoadCallback
import awais.instagrabber.webservices.interceptors.AddCookiesInterceptor
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

object MediaUploader {
    private const val HOST = "https://i.instagram.com"
    private val appExecutors = AppExecutors

    fun uploadPhoto(
        uri: Uri,
        contentResolver: ContentResolver,
        listener: OnMediaUploadCompleteListener,
    ) {
        BitmapUtils.loadBitmap(contentResolver, uri, 1000f, false, object : ThumbnailLoadCallback {
            override fun onLoad(bitmap: Bitmap?, width: Int, height: Int) {
                if (bitmap == null) {
                    listener.onFailure(RuntimeException("Bitmap result was null"))
                    return
                }
                uploadPhoto(bitmap, listener)
            }

            override fun onFailure(t: Throwable) {
                listener.onFailure(t)
            }
        })
    }

    private fun uploadPhoto(
        bitmap: Bitmap,
        listener: OnMediaUploadCompleteListener,
    ) {
        appExecutors.tasksThread.submit {
            val file: File
            val byteLength: Long
            try {
                file = BitmapUtils.convertToJpegAndSaveToFile(bitmap, null)
                byteLength = file.length()
            } catch (e: Exception) {
                listener.onFailure(e)
                return@submit
            }
            val options = createUploadPhotoOptions(byteLength)
            val headers = getUploadPhotoHeaders(options)
            val url = HOST + "/rupload_igphoto/" + options.name + "/"
            appExecutors.networkIO.execute {
                try {
                    FileInputStream(file).use { input -> upload(input, url, headers, listener) }
                } catch (e: IOException) {
                    listener.onFailure(e)
                } finally {
                    file.delete()
                }
            }
        }
    }

    @JvmStatic
    fun uploadVideo(
        uri: Uri,
        contentResolver: ContentResolver,
        options: UploadVideoOptions,
        listener: OnMediaUploadCompleteListener,
    ) {
        appExecutors.tasksThread.submit {
            val headers = getUploadVideoHeaders(options)
            val url = HOST + "/rupload_igvideo/" + options.name + "/"
            appExecutors.networkIO.execute {
                try {
                    contentResolver.openInputStream(uri).use { input ->
                        if (input == null) {
                            listener.onFailure(RuntimeException("InputStream was null"))
                            return@execute
                        }
                        upload(input, url, headers, listener)
                    }
                } catch (e: IOException) {
                    listener.onFailure(e)
                }
            }
        }
    }

    private fun upload(
        input: InputStream,
        url: String,
        headers: Map<String, String>,
        listener: OnMediaUploadCompleteListener,
    ) {
        try {
            val client = OkHttpClient.Builder()
                // .addInterceptor(new LoggingInterceptor())
                .addInterceptor(AddCookiesInterceptor())
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
            val request = Request.Builder()
                .headers(Headers.of(headers))
                .url(url)
                .post(create(MediaType.parse("application/octet-stream"), input))
                .build()
            val call = client.newCall(request)
            val response = call.execute()
            val body = response.body()
            if (!response.isSuccessful) {
                listener.onFailure(IOException("Unexpected code " + response + if (body != null) ": " + body.string() else ""))
                return
            }
            listener.onUploadComplete(MediaUploadResponse(response.code(), if (body != null) JSONObject(body.string()) else null))
        } catch (e: Exception) {
            listener.onFailure(e)
        }
    }

    private fun create(mediaType: MediaType?, inputStream: InputStream): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                return try {
                    inputStream.available().toLong()
                } catch (e: IOException) {
                    0
                }
            }

            @Throws(IOException::class)
            @Suppress("DEPRECATION_ERROR")
            override fun writeTo(sink: BufferedSink) {
                Okio.source(inputStream).use { sink.writeAll(it) }
            }
        }
    }

    interface OnMediaUploadCompleteListener {
        fun onUploadComplete(response: MediaUploadResponse)
        fun onFailure(t: Throwable)
    }

    data class MediaUploadResponse(val responseCode: Int, val response: JSONObject?)
}