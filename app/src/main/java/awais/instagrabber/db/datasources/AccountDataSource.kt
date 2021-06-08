package awais.instagrabber.db.datasources

import android.content.Context
import awais.instagrabber.db.AppDatabase
import awais.instagrabber.db.dao.AccountDao
import awais.instagrabber.db.entities.Account

class AccountDataSource private constructor(private val accountDao: AccountDao) {
    suspend fun getAccount(uid: String): Account? = accountDao.findAccountByUid(uid)

    suspend fun getAllAccounts(): List<Account> = accountDao.getAllAccounts()

    suspend fun insertOrUpdateAccount(
        uid: String?,
        username: String?,
        cookie: String?,
        fullName: String?,
        profilePicUrl: String?,
    ) {
        val account = uid?.let { getAccount(it) }
        val toUpdate = Account(account?.id ?: 0, uid, username, cookie, fullName, profilePicUrl)
        if (account != null) {
            accountDao.updateAccounts(toUpdate)
            return
        }
        accountDao.insertAccounts(toUpdate)
    }

    suspend fun deleteAccount(account: Account) = accountDao.deleteAccounts(account)

    suspend fun deleteAllAccounts() = accountDao.deleteAllAccounts()

    companion object {
        private lateinit var INSTANCE: AccountDataSource

        @JvmStatic
        fun getInstance(context: Context): AccountDataSource {
            if (!this::INSTANCE.isInitialized) {
                synchronized(AccountDataSource::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        val database = AppDatabase.getDatabase(context)
                        INSTANCE = AccountDataSource(database.accountDao())
                    }
                }
            }
            return INSTANCE
        }
    }
}