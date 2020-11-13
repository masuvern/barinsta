package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface LocationRepository {

    @GET("/api/v1/feed/location/{location}/")
    Call<String> fetchPosts(@Path("location") final String locationId,
                            @QueryMap Map<String, String> queryParams);

    @GET("/graphql/query/")
    Call<String> fetchGraphQLPosts(@QueryMap(encoded = true) Map<String, String> queryParams);
}
