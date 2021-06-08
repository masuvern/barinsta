package awais.instagrabber.webservices

import awais.instagrabber.repositories.UserService
import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.UserSearchResponse
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import java.util.*

object UserRepository : BaseService() {
    private val service: UserService = retrofit.create(UserService::class.java)

    suspend fun getUserInfo(uid: Long): User {
        val response = service.getUserInfo(uid)
        return response.user
    }

    suspend fun getUsernameInfo(username: String): User {
        val response = service.getUsernameInfo(username)
        return response.user
    }

    suspend fun getUserFriendship(uid: Long): FriendshipStatus = service.getUserFriendship(uid)

    suspend fun search(query: String): UserSearchResponse {
        val timezoneOffset = TimeZone.getDefault().rawOffset.toFloat() / 1000
        return service.search(timezoneOffset, query)
    }
}