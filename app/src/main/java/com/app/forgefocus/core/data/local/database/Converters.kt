package com.app.forgefocus.core.data.local.database

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromBrokenBlocks(value: String): Set<Int> {
        return try {
            Json.decodeFromString<Set<Int>>(value)
        } catch (e: Exception) {
            emptySet()
        }
    }

    @TypeConverter
    fun toBrokenBlocks(set: Set<Int>): String {
        return Json.encodeToString(set)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }
}