package com.app.forgefocus.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.app.forgefocus.core.data.local.database.ForgeFocusDatabase
import org.koin.dsl.module

actual val platformModule = module {
    single<RoomDatabase.Builder<ForgeFocusDatabase>> {
        val context = get<Context>()
        val dbFile = context.getDatabasePath("forge_focus_database.db")
        Room.databaseBuilder<ForgeFocusDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
    }
}