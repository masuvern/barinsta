package awais.instagrabber.db.repositories

import awais.instagrabber.db.datasources.AccountDataSource
import awais.instagrabber.db.entities.Account

class AccountRepository private constructor(private val accountDataSource: AccountDataSource) {
    suspend fun getAccount(uid: Long): Account? = accountDataSource.getAccount(uid.toString())

    suspend fun getAllAccounts(): List<Account> = accountDataSource.getAllAccounts()

    suspend fun insertOrUpdateAccounts(accounts: List<Account>) {
        for (account in accounts) {
            accountDataSource.insertOrUpdateAccount(
                account.uid,
                account.username,
                account.cookie,
                account.fullName,
                account.profilePic
            )
        }
    }

    suspend fun insertOrUpdateAccount(
        uid: Long,
        username: String,
        cookie: String,
        fullName: String,
        profilePicUrl: String?,
    ): Account? {
        accountDataSource.insertOrUpdateAccount(uid.toString(), username, cookie, fullName, profilePicUrl)
        return accountDataSource.getAccount(uid.toString())
    }

    suspend fun deleteAccount(account: Account) = accountDataSource.deleteAccount(account)

    suspend fun deleteAllAccounts() = accountDataSource.deleteAllAccounts()

    companion object {
        private lateinit var instance: AccountRepository

        @JvmStatic
        fun getInstance(accountDataSource: AccountDataSource): AccountRepository {
            if (!this::instance.isInitialized) {
                instance = AccountRepository(accountDataSource)
            }
            return instance
        }
    }
}