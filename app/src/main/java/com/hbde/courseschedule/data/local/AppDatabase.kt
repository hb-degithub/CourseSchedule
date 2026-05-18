package com.hbde.courseschedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hbde.courseschedule.data.local.converter.TimeSlotConverter
import com.hbde.courseschedule.data.local.dao.CourseDao
import com.hbde.courseschedule.data.local.dao.EventDao
import com.hbde.courseschedule.data.local.dao.GradeDao
import com.hbde.courseschedule.data.local.dao.ThemeConfigDao
import com.hbde.courseschedule.data.local.dao.TimeTableDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.data.local.entity.GradeEntity
import com.hbde.courseschedule.data.local.entity.ScheduleEntity
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import com.hbde.courseschedule.data.local.entity.TimeTableEntity
import kotlinx.coroutines.launch

@Database(
    entities = [
        CourseEntity::class,
        TimeTableEntity::class,
        ScheduleEntity::class,
        EventEntity::class,
        ThemeConfigEntity::class,
        GradeEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(TimeSlotConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun eventDao(): EventDao
    abstract fun timeTableDao(): TimeTableDao
    abstract fun themeConfigDao(): ThemeConfigDao
    abstract fun gradeDao(): GradeDao

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
                    .addMigrations(MIGRATION_1_2)
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN courseId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grades (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        courseName TEXT NOT NULL,
                        credit REAL NOT NULL,
                        score REAL NOT NULL,
                        semester TEXT NOT NULL,
                        type TEXT NOT NULL,
                        teacher TEXT,
                        courseCode TEXT,
                        gpa REAL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
