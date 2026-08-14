package com.app.forgefocus.core.data.local.database

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

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

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilliseconds()
    }
}