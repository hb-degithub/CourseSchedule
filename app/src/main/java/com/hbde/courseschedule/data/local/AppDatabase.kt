package com.hbde.courseschedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hbde.courseschedule.data.local.converter.TimeSlotConverter
import com.hbde.courseschedule.data.local.dao.CourseDao
import com.hbde.courseschedule.data.local.dao.EventDao
import com.hbde.courseschedule.data.local.dao.TimeTableDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.data.local.entity.ScheduleEntity
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import com.hbde.courseschedule.data.local.entity.TimeTableEntity

@Database(
    entities = [
        CourseEntity::class,
        TimeTableEntity::class,
        ScheduleEntity::class,
        EventEntity::class,
        ThemeConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(TimeSlotConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun eventDao(): EventDao
    abstract fun timeTableDao(): TimeTableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "course_schedule.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                kotlinx.coroutines.CoroutineScope(
                                    kotlinx.coroutines.SupervisorJob() +
                                    kotlinx.coroutines.Dispatchers.IO
                                ).launch {
                                    com.hbde.courseschedule.data.local.seed.DatabaseSeed
                                        .sampleCourses.forEach { course ->
                                            database.courseDao().insert(course)
                                        }
                                }
                            }
                        }
                    })
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}
