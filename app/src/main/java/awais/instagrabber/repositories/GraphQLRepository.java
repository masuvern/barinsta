package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface GraphQLRepository {
    @GET("/graphql/query/")
    Call<String> fetch(@QueryMap(encoded = true) Map<String, String> queryParams);
}
