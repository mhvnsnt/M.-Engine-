package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONObject

@Entity(tableName = "capability_knowledge")
@TypeConverters(CapabilityConverters::class)
data class CapabilityKnowledgeEntity(
    @PrimaryKey val capabilityName: String,
    val knownImplementations: List<String>,
    val currentWinner: String?,
    val reasonForWinning: String?,
    val lastEvaluatedAt: Long,
    val reevaluateCondition: String?,
    val evaluations: Map<String, String> // Simplified for SQLite
)

class CapabilityConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromEvaluationMap(value: Map<String, String>): String {
        val json = JSONObject()
        value.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    @TypeConverter
    fun toEvaluationMap(value: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (value.isEmpty()) return map
        try {
            val json = JSONObject(value)
            json.keys().forEach { key ->
                map[key] = json.getString(key)
            }
        } catch (e: Exception) {}
        return map
    }
}
