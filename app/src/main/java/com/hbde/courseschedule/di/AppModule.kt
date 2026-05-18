package com.hbde.courseschedule.di

import android.content.Context
import com.hbde.courseschedule.data.local.AppDatabase
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.data.local.dao.CourseDao
import com.hbde.courseschedule.data.local.dao.EventDao
import com.hbde.courseschedule.data.local.dao.GradeDao
import com.hbde.courseschedule.data.local.dao.ThemeConfigDao
import com.hbde.courseschedule.data.local.dao.TimeTableDao
import com.hbde.courseschedule.data.repository.CourseRepository
import com.hbde.courseschedule.data.repository.CourseRepositoryImpl
import com.hbde.courseschedule.data.repository.EventRepository
import com.hbde.courseschedule.data.repository.EventRepositoryImpl
import com.hbde.courseschedule.data.repository.GradeRepository
import com.hbde.courseschedule.data.repository.GradeRepositoryImpl
import com.hbde.courseschedule.data.repository.ThemeConfigRepository
import com.hbde.courseschedule.data.repository.ThemeConfigRepositoryImpl
import com.hbde.courseschedule.importer.parser.ParserRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context).also { database ->
            // 种子数据通过 RoomDatabase.Callback 在 AppDatabase 中处理
        }
    }

    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao = database.courseDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()

    @Provides
    fun provideTimeTableDao(database: AppDatabase): TimeTableDao = database.timeTableDao()

    @Provides
    fun provideThemeConfigDao(database: AppDatabase): ThemeConfigDao = database.themeConfigDao()

    @Provides
    fun provideGradeDao(database: AppDatabase): GradeDao = database.gradeDao()

    @Provides
    @Singleton
    fun provideCourseRepository(
        courseDao: CourseDao,
        @ApplicationContext context: Context
    ): CourseRepository {
        return CourseRepositoryImpl(courseDao, context)
    }

    @Provides
    @Singleton
    fun provideEventRepository(eventDao: EventDao): EventRepository {
        return EventRepositoryImpl(eventDao)
    }

    @Provides
    @Singleton
    fun provideThemeConfigRepository(themeConfigDao: ThemeConfigDao): ThemeConfigRepository {
        return ThemeConfigRepositoryImpl(themeConfigDao)
    }

    @Provides
    @Singleton
    fun provideGradeRepository(gradeDao: GradeDao): GradeRepository {
        return GradeRepositoryImpl(gradeDao)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideParserRegistry(): ParserRegistry {
        return ParserRegistry()
    }

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): android.app.AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): com.hbde.courseschedule.service.notification.NotificationHelper {
        return com.hbde.courseschedule.service.notification.NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideAudioModeManager(
        @ApplicationContext context: Context,
        settingsDataStore: SettingsDataStore
    ): com.hbde.courseschedule.service.audio.AudioModeManager {
        return com.hbde.courseschedule.service.audio.AudioModeManager(context, settingsDataStore)
    }

    @Provides
    @Singleton
    fun provideCourseCountdownManager(): com.hbde.courseschedule.service.countdown.CourseCountdownManager {
        return com.hbde.courseschedule.service.countdown.CourseCountdownManager()
    }
}
