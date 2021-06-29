package awais.instagrabber.common

import awais.instagrabber.db.dao.AccountDao
import awais.instagrabber.db.dao.FavoriteDao
import awais.instagrabber.db.entities.Account
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.*
import awais.instagrabber.repositories.responses.*
import awais.instagrabber.repositories.responses.stories.StoryStickerResponse

open class UserServiceAdapter : UserService {
    override suspend fun getUserInfo(uid: Long): WrappedUser {
        TODO("Not yet implemented")
    }

    override suspend fun getUsernameInfo(username: String): WrappedUser {
        TODO("Not yet implemented")
    }

    override suspend fun getUserFriendship(uid: Long): FriendshipStatus = FriendshipStatus()

    override suspend fun search(timezoneOffset: Float, query: String): UserSearchResponse {
        TODO("Not yet implemented")
    }
}

open class FriendshipServiceAdapter : FriendshipService {
    override suspend fun change(action: String, id: Long, form: Map<String, String>): FriendshipChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun toggleRestrict(action: String, form: Map<String, String>): FriendshipRestrictResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getList(userId: Long, type: String, queryParams: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun changeMute(action: String, form: Map<String, String>): FriendshipChangeResponse {
        TODO("Not yet implemented")
    }
}

open class StoriesServiceAdapter : StoriesService {
    override suspend fun fetch(mediaId: Long): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFeedStories(): String {
        TODO("Not yet implemented")
    }

    override suspend fun fetchHighlights(uid: Long): String {
        TODO("Not yet implemented")
    }

    override suspend fun fetchArchive(queryParams: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun getUserStory(url: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun respondToSticker(storyId: String, stickerId: String, action: String, form: Map<String, String>): StoryStickerResponse {
        TODO("Not yet implemented")
    }

    override suspend fun seen(queryParams: Map<String, String>, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }
}

open class MediaServiceAdapter : MediaService {
    override suspend fun fetch(mediaId: Long): MediaInfoResponse {
        TODO("Not yet implemented")
    }

    override suspend fun fetchLikes(mediaId: String, action: String): LikersResponse {
        TODO("Not yet implemented")
    }

    override suspend fun action(action: String, mediaId: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun editCaption(mediaId: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun translate(form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun uploadFinish(retryContext: String, queryParams: Map<String, String>, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun delete(mediaId: String, mediaType: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun archive(mediaId: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }
}

open class GraphQLServiceAdapter : GraphQLService {
    override suspend fun fetch(queryParams: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun getUser(username: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getPost(shortcode: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getTag(tag: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getLocation(locationId: Long): String {
        TODO("Not yet implemented")
    }
}

open class AccountDaoAdapter : AccountDao {
    override suspend fun getAllAccounts(): List<Account> {
        TODO("Not yet implemented")
    }

    override suspend fun findAccountByUid(uid: String): Account? {
        TODO("Not yet implemented")
    }

    override suspend fun insertAccounts(vararg accounts: Account) {
        TODO("Not yet implemented")
    }

    override suspend fun updateAccounts(vararg accounts: Account) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccounts(vararg accounts: Account) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllAccounts() {
        TODO("Not yet implemented")
    }
}

open class FavoriteDaoAdapter : FavoriteDao {
    override suspend fun getAllFavorites(): List<Favorite> = emptyList()

    override suspend fun findFavoriteByQueryAndType(query: String, type: FavoriteType): Favorite? = null

    override suspend fun insertFavorites(vararg favorites: Favorite) {}

    override suspend fun updateFavorites(vararg favorites: Favorite) {}

    override suspend fun deleteFavorites(vararg favorites: Favorite) {}

    override suspend fun deleteAllFavorites() {}
}