package awais.instagrabber.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Resource<T> {
    public final Status status;
    public final T data;
    public final String message;

    private Resource(@NonNull Status status,
                     @Nullable T data,
                     @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    @NonNull
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    @NonNull
    public static <T> Resource<T> error(String msg, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg);
    }

    @NonNull
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}
