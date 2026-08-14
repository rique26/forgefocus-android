package com.app.forgefocus.di


import androidx.room.Room
import androidx.room.RoomDatabase
import com.app.forgefocus.core.data.local.database.ForgeFocusDatabase
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual val platformModule = module {
    single<RoomDatabase.Builder<ForgeFocusDatabase>> {
        val dbFilePath = NSHomeDirectory() + "/forge_focus_database.db"
        Room.databaseBuilder<ForgeFocusDatabase>(
            name = dbFilePath
        )
    }
}