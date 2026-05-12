package com.hbde.courseschedule.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TimeSlotConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromTimeSlotList(timeSlots: List<Pair<String, String>>?): String? {
        if (timeSlots == null) return null
        return gson.toJson(timeSlots)
    }

    @TypeConverter
    fun toTimeSlotList(json: String?): List<Pair<String, String>>? {
        if (json == null) return null
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return gson.fromJson(json, type)
    }
}
