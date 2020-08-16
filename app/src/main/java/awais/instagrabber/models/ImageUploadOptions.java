package awais.instagrabber.models;

import java.io.InputStream;

public class ImageUploadOptions {
    private InputStream inputStream;
    private long contentLength;
    private boolean isSidecar;
    private String waterfallId;

    public static class Builder {
        private InputStream inputStream;
        private long contentLength;
        private boolean isSidecar;
        private String waterfallId;

        public Builder(final InputStream inputStream, final long contentLength) {
            this.inputStream = inputStream;
            this.contentLength = contentLength;
        }

        public Builder setInputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder setContentLength(final long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder setIsSidecar(final boolean isSidecar) {
            this.isSidecar = isSidecar;
            return this;
        }

        public Builder setWaterfallId(final String waterfallId) {
            this.waterfallId = waterfallId;
            return this;
        }

        public ImageUploadOptions build() {
            return new ImageUploadOptions(inputStream, contentLength, isSidecar, waterfallId);
        }
    }

    public static Builder builder(final InputStream inputStream, final long contentLength) {
        return new Builder(inputStream, contentLength);
    }

    private ImageUploadOptions(final InputStream inputStream,
                               final long contentLength,
                               final boolean isSidecar,
                               final String waterfallId) {
        this.inputStream = inputStream;
        this.contentLength = contentLength;
        this.isSidecar = isSidecar;
        this.waterfallId = waterfallId;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isSidecar() {
        return isSidecar;
    }

    public String getWaterfallId() {
        return waterfallId;
    }
}
