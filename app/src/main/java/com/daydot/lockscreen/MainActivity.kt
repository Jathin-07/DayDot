package com.daydot.lockscreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.daydot.lockscreen.ui.screens.AppearanceSettingsScreen
import com.daydot.lockscreen.ui.screens.DashboardScreen
import com.daydot.lockscreen.ui.screens.LockScreenPreviewScreen
import com.daydot.lockscreen.ui.screens.ReminderManagerScreen
import com.daydot.lockscreen.ui.theme.DayDotTheme
import com.daydot.lockscreen.ui.viewmodel.DayDotViewModel

private enum class DayDotRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Today", Icons.Outlined.Home),
    REMINDERS("reminders", "Reminders", Icons.Outlined.CalendarMonth),
    APPEARANCE("appearance", "Style", Icons.Outlined.Palette),
    LOCK_SCREEN("lock_screen", "Lock Screen", Icons.Outlined.Smartphone)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayDotTheme {
                DayDotAppRoot()
            }
        }
    }
}

@Composable
private fun DayDotAppRoot(viewModel: DayDotViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingEditDate by viewModel.pendingEditDate.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh today's date whenever the user returns to the app (e.g., after midnight)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDateIfChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                DayDotRoute.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DayDotRoute.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(DayDotRoute.DASHBOARD.route) {
                DashboardScreen(
                    uiState = uiState,
                    onQuickEditDate = { date ->
                        viewModel.openEditorForDate(date)
                        navigateToTab(DayDotRoute.REMINDERS.route)
                    },
                    onOpenLockScreenPreview = {
                        navigateToTab(DayDotRoute.LOCK_SCREEN.route)
                    },
                    onApplyNow = { viewModel.applyToLockScreen() }
                )
            }

            composable(DayDotRoute.REMINDERS.route) {
                ReminderManagerScreen(
                    uiState = uiState,
                    initialEditDate = pendingEditDate,
                    onClearInitialEditDate = viewModel::consumePendingEditDate,
                    onSaveReminder = viewModel::saveReminder,
                    onDeleteReminder = viewModel::deleteReminder
                )
            }

            composable(DayDotRoute.APPEARANCE.route) {
                AppearanceSettingsScreen(
                    uiState = uiState,
                    onUpdateAppearance = viewModel::updateAppearance
                )
            }

            composable(DayDotRoute.LOCK_SCREEN.route) {
                LockScreenPreviewScreen(
                    uiState = uiState,
                    onApplyToLockScreen = { viewModel.applyToLockScreen() },
                    onManualFallback = { viewModel.exportAndLaunchFallbackPicker() }
                )
            }
        }
    }
}
