package com.example.data

import androidx.room.TypeConverter

class VectorConverters {
    @TypeConverter
    fun fromString(value: String?): List<Float>? {
        if (value.isNullOrEmpty()) return null
        return value.split(",").mapNotNull { it.toFloatOrNull() }
    }

    @TypeConverter
    fun fromList(list: List<Float>?): String? {
        if (list == null) return null
        return list.joinToString(",")
    }
}
