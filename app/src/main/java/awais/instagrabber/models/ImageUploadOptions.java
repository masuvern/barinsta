package awais.instagrabber.models;

import java.io.File;

public class ImageUploadOptions {
    private final File file;
    private boolean isSidecar;
    private String waterfallId;

    public static class Builder {
        private File file;
        private boolean isSidecar;
        private String waterfallId;

        public Builder(final File file) {
            this.file = file;
        }

        public Builder setFile(final File file) {
            this.file = file;
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
            return new ImageUploadOptions(file, isSidecar, waterfallId);
        }
    }

    public static Builder builder(final File file) {
        return new Builder(file);
    }

    private ImageUploadOptions(final File file,
                               final boolean isSidecar,
                               final String waterfallId) {
        this.file = file;
        this.isSidecar = isSidecar;
        this.waterfallId = waterfallId;
    }

    public File getFile() {
        return file;
    }

    public boolean isSidecar() {
        return isSidecar;
    }

    public String getWaterfallId() {
        return waterfallId;
    }
}
