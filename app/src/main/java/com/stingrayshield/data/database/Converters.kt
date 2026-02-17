package com.stingrayshield.data.database

import androidx.room.TypeConverter
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.ThreatLevel

/**
 * Type converters for Room database to handle custom types and enums
 */
class Converters {

    @TypeConverter
    fun fromAnomalyType(value: AnomalyType): String {
        return value.name
    }

    @TypeConverter
    fun toAnomalyType(value: String): AnomalyType {
        return try {
            AnomalyType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AnomalyType.UNKNOWN
        }
    }

    @TypeConverter
    fun fromThreatLevel(value: ThreatLevel): String {
        return value.name
    }

    @TypeConverter
    fun toThreatLevel(value: String): ThreatLevel {
        return try {
            ThreatLevel.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ThreatLevel.NONE
        }
    }
}
