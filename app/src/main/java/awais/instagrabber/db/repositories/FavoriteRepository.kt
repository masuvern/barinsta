package awais.instagrabber.db.repositories

import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType

class FavoriteRepository private constructor(private val favoriteDataSource: FavoriteDataSource) {

    suspend fun getFavorite(query: String, type: FavoriteType): Favorite? = favoriteDataSource.getFavorite(query, type)

    suspend fun getAllFavorites(): List<Favorite> = favoriteDataSource.getAllFavorites()

    suspend fun insertOrUpdateFavorite(favorite: Favorite) = favoriteDataSource.insertOrUpdateFavorite(favorite)

    suspend fun deleteFavorite(query: String?, type: FavoriteType?) = favoriteDataSource.deleteFavorite(query, type)

    companion object {
        private lateinit var instance: FavoriteRepository

        @JvmStatic
        fun getInstance(favoriteDataSource: FavoriteDataSource): FavoriteRepository {
            if (!this::instance.isInitialized) {
                instance = FavoriteRepository(favoriteDataSource)
            }
            return instance
        }
    }
}