package com.hbde.courseschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeConfigDao {

    @Query("SELECT * FROM theme_configs LIMIT 1")
    fun getThemeConfig(): Flow<ThemeConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ThemeConfigEntity): Long

    @Update
    suspend fun update(config: ThemeConfigEntity)

    @Query("SELECT * FROM theme_configs LIMIT 1")
    suspend fun getThemeConfigSync(): ThemeConfigEntity?
}
