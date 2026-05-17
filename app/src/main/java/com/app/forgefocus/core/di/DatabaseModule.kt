package com.app.forgefocus.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.forgefocus.core.data.local.dao.GoalDao
import com.app.forgefocus.core.data.local.dao.ProgressLogDao
import com.app.forgefocus.core.data.local.database.ForgeFocusDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN duration_unit TEXT NOT NULL DEFAULT 'MONTHS'")
        }
    }
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ForgeFocusDatabase {
        return Room.databaseBuilder(
            context,
            ForgeFocusDatabase::class.java,
            "forge_focus_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideGoalDao(db: ForgeFocusDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideProgressLogDao(db: ForgeFocusDatabase): ProgressLogDao = db.progressLogDao()
}