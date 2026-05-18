package com.hbde.courseschedule.ui.widget

import android.content.Context
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.utils.TermWeekCalculator
import kotlinx.coroutines.flow.first

internal suspend fun resolveCurrentWeek(context: Context): Int {
    val termStartDate = SettingsDataStore(context.applicationContext).termStartDate.first()
    return TermWeekCalculator.calculateCurrentWeek(termStartDate)
}
