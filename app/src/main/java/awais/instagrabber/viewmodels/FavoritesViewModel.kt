package awais.instagrabber.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.db.repositories.RepositoryCallback

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val _list = MutableLiveData<List<Favorite>>()
    val list: LiveData<List<Favorite>> = _list

    private val favoriteRepository: FavoriteRepository = FavoriteRepository.getInstance(FavoriteDataSource.getInstance(application))

    init {
        fetch()
    }

    fun fetch() {
        favoriteRepository.getAllFavorites(object : RepositoryCallback<List<Favorite>> {
            override fun onSuccess(favorites: List<Favorite>?) {
                _list.postValue(favorites ?: emptyList())
            }

            override fun onDataNotAvailable() {}
        })
    }

    fun delete(favorite: Favorite, onSuccess: () -> Unit) {
        favoriteRepository.deleteFavorite(favorite.query, favorite.type, object : RepositoryCallback<Void> {
            override fun onSuccess(result: Void?) {
                onSuccess()
                favoriteRepository.getAllFavorites(object : RepositoryCallback<List<Favorite>> {
                    override fun onSuccess(result: List<Favorite>?) {
                        _list.postValue(result ?: emptyList())
                    }

                    override fun onDataNotAvailable() {}
                })
            }

            override fun onDataNotAvailable() {}
        })
    }
}