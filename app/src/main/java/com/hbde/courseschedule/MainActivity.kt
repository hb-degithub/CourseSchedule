package com.hbde.courseschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hbde.courseschedule.ui.AppNavigation
import com.hbde.courseschedule.ui.theme.CourseScheduleTheme
import com.hbde.courseschedule.ui.widget.ScheduleWidgetProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle widget click extras (e.g., open specific course)
        val courseId = intent.getIntExtra(ScheduleWidgetProvider.EXTRA_COURSE_ID, -1)
        // TODO: Navigate to course detail/editor when deep link is set up

        setContent {
            CourseScheduleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
