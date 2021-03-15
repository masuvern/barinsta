package awais.instagrabber.webservices;

import awais.instagrabber.repositories.GifRepository;
import awais.instagrabber.repositories.responses.giphy.GiphyGifResponse;
import retrofit2.Call;
import retrofit2.Retrofit;

public class GifService extends BaseService {

    private final GifRepository repository;

    private static GifService instance;

    private GifService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(GifRepository.class);
    }

    public static GifService getInstance() {
        if (instance == null) {
            instance = new GifService();
        }
        return instance;
    }

    public Call<GiphyGifResponse> searchGiphyGifs(final String query,
                                                  final boolean includeGifs) {
        final String mediaTypes = includeGifs ? "[\"giphy_gifs\",\"giphy\"]" : "[\"giphy\"]";
        return repository.searchGiphyGifs("direct", query, mediaTypes);
    }
}
