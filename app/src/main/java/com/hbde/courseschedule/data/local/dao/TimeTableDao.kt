package com.hbde.courseschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hbde.courseschedule.data.local.entity.TimeTableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeTableDao {

    @Insert
    suspend fun insert(timeTable: TimeTableEntity): Long

    @Update
    suspend fun update(timeTable: TimeTableEntity)

    @Delete
    suspend fun delete(timeTable: TimeTableEntity)

    @Query("SELECT * FROM time_tables")
    fun getAllTimeTables(): Flow<List<TimeTableEntity>>

    @Query("SELECT * FROM time_tables WHERE id = :id")
    fun getTimeTableById(id: Int): Flow<TimeTableEntity?>

    @Query("SELECT * FROM time_tables ORDER BY id ASC LIMIT 1")
    suspend fun getDefaultTimeTable(): TimeTableEntity?

    @Query("SELECT * FROM time_tables WHERE id = :id")
    suspend fun getTimeTableByIdSync(id: Int): TimeTableEntity?
}
