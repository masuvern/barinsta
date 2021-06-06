package awais.instagrabber.webservices

import awais.instagrabber.repositories.UserRepository
import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.UserSearchResponse
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import java.util.*

object UserService : BaseService() {
    private val repository: UserRepository = retrofit.create(UserRepository::class.java)

    suspend fun getUserInfo(uid: Long): User {
        val response = repository.getUserInfo(uid)
        return response.user
    }

    suspend fun getUsernameInfo(username: String): User {
        val response = repository.getUsernameInfo(username)
        return response.user
    }

    suspend fun getUserFriendship(uid: Long): FriendshipStatus = repository.getUserFriendship(uid)

    suspend fun search(query: String): UserSearchResponse {
        val timezoneOffset = TimeZone.getDefault().rawOffset.toFloat() / 1000
        return repository.search(timezoneOffset, query)
    }
}