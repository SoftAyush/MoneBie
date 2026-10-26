package com.aurex.app.data.repository

import com.aurex.app.data.database.DatabaseHelper
import com.aurex.app.data.mapper.toDomain
import com.aurex.app.domain.model.ThemeMode
import com.aurex.app.domain.model.User
import com.aurex.app.domain.model.themeModeOf
import com.aurex.app.domain.repository.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val dbHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : UserRepository {

//    override suspend fun insertUser(name: String, date: String, currencyCode: String) {
//        withContext(ioDispatcher) {
//            dbHelper.userQueries.insertUser(name, date, currencyCode)
//        }
//    }

    override suspend fun insertUser(
        name: String,
        date: String,
        currencyCode: String
    ) {
        withContext(ioDispatcher) {
            val user = dbHelper.userQueries.getUser().executeAsOneOrNull()

            if (user != null) {
                dbHelper.userQueries.updateUser(
                    name = name,
                    date = date,
                    currency_code = currencyCode
                )
            } else {
                dbHelper.userQueries.insertUser(
                    name = name,
                    date = date,
                    currency_code = currencyCode
                )
            }
        }
    }

    override fun getAllUsers(): Flow<List<User>> {
        return dbHelper.userQueries.selectAllUsers()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getSelectedCurrencySymbol(): Flow<String?> {
        return dbHelper.userQueries.getSelectedCurrencySymbol()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
    }

    override fun observeThemeMode(): Flow<ThemeMode> {
        return dbHelper.userQueries.getThemeMode()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            // No profile row yet - during first run - is not an error: the app
            // follows the phone until there is somewhere to record a choice.
            .map { themeModeOf(it) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        withContext(ioDispatcher) {
            dbHelper.userQueries.setThemeMode(mode.name)
        }
    }
}
