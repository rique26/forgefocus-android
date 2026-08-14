package com.app.forgefocus.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.app.forgefocus.core.data.local.dao.GoalDao
import com.app.forgefocus.core.data.local.dao.ProgressLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [GoalEntity::class, ProgressLogEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
@ConstructedBy(ForgeFocusDatabaseConstructor::class)
abstract class ForgeFocusDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun progressLogDao(): ProgressLogDao
}

@Suppress("KotlinNoActualForExpect")
expect object ForgeFocusDatabaseConstructor : RoomDatabaseConstructor<ForgeFocusDatabase> {
    override fun initialize(): ForgeFocusDatabase
}

// Função para instanciar o banco final no código comum
fun getRoomDatabase(
    builder: RoomDatabase.Builder<ForgeFocusDatabase>
): ForgeFocusDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}