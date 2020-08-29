package awais.instagrabber.services;

public interface ServiceCallback<T> {
    void onSuccess(T result);

    void onFailure(Throwable t);
}
