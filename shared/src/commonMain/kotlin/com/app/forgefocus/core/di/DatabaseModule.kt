package com.app.forgefocus.core.di

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.app.forgefocus.core.data.local.dao.GoalDao
import com.app.forgefocus.core.data.local.dao.ProgressLogDao
import com.app.forgefocus.core.data.local.database.ForgeFocusDatabase
import com.app.forgefocus.core.data.local.database.getRoomDatabase
import org.koin.dsl.module

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE goals ADD COLUMN duration_unit TEXT NOT NULL DEFAULT 'MONTHS'")
    }
}

val databaseModule = module {
    // Injeta a instância do banco construída pela plataforma (Android ou iOS)
    single<ForgeFocusDatabase> {
        val builder = get<RoomDatabase.Builder<ForgeFocusDatabase>>()
        getRoomDatabase(builder.addMigrations(MIGRATION_1_2))
    }

    // Provedores dos DAOs
    single<GoalDao> { get<ForgeFocusDatabase>().goalDao() }
    single<ProgressLogDao> { get<ForgeFocusDatabase>().progressLogDao() }
}
