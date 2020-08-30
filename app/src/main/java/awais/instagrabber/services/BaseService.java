package awais.instagrabber.services;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public abstract class BaseService {

    private Retrofit.Builder builder;

    Retrofit.Builder getRetrofitBuilder() {
        if (builder == null) {
            final OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AddCookiesInterceptor())
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build();
            builder = new Retrofit.Builder()
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client);
        }
        return builder;
    }
}
