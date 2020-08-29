package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public interface StoriesRepository {

    @GET("graphql/query/")
    Call<String> getStories(@QueryMap(encoded = true) Map<String, String> variables);

    @GET
    Call<String> getUserStory(@Header("User-Agent") String userAgent, @Url String url);
}
