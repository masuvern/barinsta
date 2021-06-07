package awais.instagrabber.db.datasources

import android.content.Context
import awais.instagrabber.db.AppDatabase
import awais.instagrabber.db.dao.FavoriteDao
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType

class FavoriteDataSource private constructor(private val favoriteDao: FavoriteDao) {
    suspend fun getFavorite(query: String, type: FavoriteType): Favorite? = favoriteDao.findFavoriteByQueryAndType(query, type)

    suspend fun getAllFavorites(): List<Favorite> = favoriteDao.getAllFavorites()

    suspend fun insertOrUpdateFavorite(favorite: Favorite) {
        if (favorite.id != 0) {
            favoriteDao.updateFavorites(favorite)
            return
        }
        favoriteDao.insertFavorites(favorite)
    }

    suspend fun deleteFavorite(query: String, type: FavoriteType) {
        val favorite = getFavorite(query, type) ?: return
        favoriteDao.deleteFavorites(favorite)
    }

    companion object {
        private lateinit var INSTANCE: FavoriteDataSource

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): FavoriteDataSource {
            if (!this::INSTANCE.isInitialized) {
                synchronized(FavoriteDataSource::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        val database = AppDatabase.getDatabase(context)
                        INSTANCE = FavoriteDataSource(database.favoriteDao())
                    }
                }
            }
            return INSTANCE
        }
    }
}