package awais.instagrabber.services;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import awais.instagrabber.BuildConfig;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public abstract class BaseService {

    private Retrofit.Builder builder;

    Retrofit.Builder getRetrofitBuilder() {
        if (builder == null) {
            final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .addInterceptor(new AddCookiesInterceptor())
                    .followRedirects(true)
                    .followSslRedirects(true);
            if (BuildConfig.DEBUG) {
                // clientBuilder.addInterceptor(new LoggingInterceptor());
            }
            final Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            builder = new Retrofit.Builder()
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(clientBuilder.build());
        }
        return builder;
    }
}
