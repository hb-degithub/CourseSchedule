package com.hbde.courseschedule.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hbde.courseschedule.ui.campus.CampusScreen
import com.hbde.courseschedule.ui.campus.classroom.ClassroomScreen
import com.hbde.courseschedule.ui.campus.grade.GradeScreen
import com.hbde.courseschedule.ui.editor.CourseEditorScreen
import com.hbde.courseschedule.ui.importer.ImportMethodScreen
import com.hbde.courseschedule.ui.importer.ImportPreviewScreen
import com.hbde.courseschedule.ui.event.EventEditorScreen
import com.hbde.courseschedule.ui.onboarding.OnboardingScreen
import com.hbde.courseschedule.ui.event.EventListScreen
import com.hbde.courseschedule.ui.schedule.ScheduleScreen
import com.hbde.courseschedule.ui.schedule.TwoDayScheduleScreen
import com.hbde.courseschedule.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Schedule : Screen("schedule_screen", "课表", Icons.Filled.CalendarMonth)
    data object Event : Screen("event_screen", "日程", Icons.Filled.Event)
    data object Campus : Screen("campus_screen", "校园", Icons.Filled.School)
    data object Settings : Screen("settings_screen", "设置", Icons.Filled.Settings)
}

val bottomNavItems = listOf(Screen.Schedule, Screen.Event, Screen.Campus, Screen.Settings)

object Routes {
    const val SCHEDULE = "schedule_screen"
    const val TWO_DAY_SCHEDULE = "two_day_schedule_screen"
    const val EVENT = "event_screen"
    const val CAMPUS = "campus_screen"
    const val GRADE = "grade_screen"
    const val CLASSROOM = "classroom_screen"
    const val SETTINGS = "settings_screen"
    const val COURSE_EDITOR = "course_editor_screen"
    const val EVENT_EDITOR = "event_editor_screen"
    const val IMPORT_PREVIEW = "import_preview_screen"
    const val IMPORT_METHOD = "import_method_screen"
    const val ONBOARDING = "onboarding_screen"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SCHEDULE,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    onNavigateToEditor = { courseId, dayOfWeek, startNode ->
                        val route = buildString {
                            append(Routes.COURSE_EDITOR)
                            val params = mutableListOf<String>()
                            courseId?.let { params.add("courseId=$it") }
                            dayOfWeek?.let { params.add("dayOfWeek=$it") }
                            startNode?.let { params.add("startNode=$it") }
                            if (params.isNotEmpty()) {
                                append("?")
                                append(params.joinToString("&"))
                            }
                        }
                        navController.navigate(route)
                    },
                    onNavigateToImportMethod = { navController.navigate(Routes.IMPORT_METHOD) }
                )
            }
            composable(Routes.TWO_DAY_SCHEDULE) {
                TwoDayScheduleScreen(
                    onNavigateToEditor = { courseId ->
                        val route = if (courseId != null) {
                            "${Routes.COURSE_EDITOR}?courseId=$courseId"
                        } else {
                            Routes.COURSE_EDITOR
                        }
                        navController.navigate(route)
                    }
                )
            }
            composable(Routes.EVENT) {
                EventListScreen(
                    onNavigateToEditor = { navController.navigate(Routes.EVENT_EDITOR) }
                )
            }
            composable(Routes.CAMPUS) {
                CampusScreen(
                    onNavigateToGrade = { navController.navigate(Routes.GRADE) },
                    onNavigateToClassroom = { navController.navigate(Routes.CLASSROOM) }
                )
            }
            composable(Routes.GRADE) {
                GradeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CLASSROOM) {
                ClassroomScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.COURSE_EDITOR + "?courseId={courseId}&dayOfWeek={dayOfWeek}&startNode={startNode}",
                arguments = listOf(
                    navArgument("courseId") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("dayOfWeek") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("startNode") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getInt("courseId")?.takeIf { it != -1 }
                val dayOfWeek = backStackEntry.arguments?.getInt("dayOfWeek")?.takeIf { it != -1 }
                val startNode = backStackEntry.arguments?.getInt("startNode")?.takeIf { it != -1 }
                CourseEditorScreen(
                    courseId = courseId,
                    initialDayOfWeek = dayOfWeek,
                    initialStartNode = startNode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.EVENT_EDITOR) {
                EventEditorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.IMPORT_PREVIEW + "?courses_json={courses_json}",
                arguments = listOf(
                    navArgument("courses_json") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                ImportPreviewScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.IMPORT_METHOD) {
                ImportMethodScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWebViewImporter = {
                        // TODO: Navigate to WebView importer
                        navController.popBackStack()
                    },
                    onNavigateToFilePicker = {
                        // TODO: Open file picker for Excel/CSV/ICS
                        navController.popBackStack()
                    },
                    onNavigateToImagePicker = {
                        // TODO: Open camera/gallery picker
                        navController.popBackStack()
                    },
                    onNavigateToShareCode = {
                        // TODO: Navigate to share code input
                        navController.popBackStack()
                    },
                    onNavigateToManualAdd = {
                        navController.navigate(Routes.COURSE_EDITOR)
                    }
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = { navController.navigate(Routes.SCHEDULE) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
                    onSkip = { navController.navigate(Routes.SCHEDULE) { popUpTo(Routes.ONBOARDING) { inclusive = true } } }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    if (showBottomBar) {
        NavigationBar {
            bottomNavItems.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                    label = { Text(screen.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
