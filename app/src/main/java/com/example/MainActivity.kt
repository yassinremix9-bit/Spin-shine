package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AppThemeMode
import com.example.ui.components.AddMachineBottomSheet
import com.example.ui.components.MachineDetailBottomSheet
import com.example.ui.components.SpinShineHeader
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SpinShineTheme
import com.example.ui.viewmodel.SpinShineViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_SETTINGS = "settings"

private data class NavTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SpinShineViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            val isDark = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            SpinShineTheme(darkTheme = isDark) {
                SpinShineApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SpinShineApp(viewModel: SpinShineViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMachineId by viewModel.selectedMachineId.collectAsStateWithLifecycle()

    var showAddMachineSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navTabs = listOf(
        NavTabItem(
            route = ROUTE_DASHBOARD,
            labelRes = R.string.nav_dashboard,
            selectedIcon = Icons.Rounded.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_tab_dashboard"
        ),
        NavTabItem(
            route = ROUTE_NOTIFICATIONS,
            labelRes = R.string.nav_notifications,
            selectedIcon = Icons.Rounded.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            testTag = "nav_tab_notifications"
        ),
        NavTabItem(
            route = ROUTE_SETTINGS,
            labelRes = R.string.nav_settings,
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_tab_settings"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                SpinShineHeader(
                    isOnline = settings.isOnlineMode,
                    unreadCount = unreadCount,
                    isRefreshing = dashboardState.isRefreshing,
                    onNotificationsClick = { navigateToTab(ROUTE_NOTIFICATIONS) },
                    onToggleNetworkClick = { viewModel.toggleNetworkState() },
                    onRefreshClick = { viewModel.refreshData() }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            navTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        TabIconWithBadge(
                                            tab = tab,
                                            selected = selected,
                                            unreadCount = unreadCount
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            uiState = dashboardState,
                            isOnline = settings.isOnlineMode,
                            onReconnectClick = { viewModel.toggleNetworkState() },
                            onAddMachineClick = { showAddMachineSheet = true },
                            onSelectFilter = { viewModel.selectFilter(it) },
                            onMachineCardClick = { viewModel.openMachineSheet(it.id) },
                            onQuickActionClick = { viewModel.toggleMaintenance(it) },
                            onToggleReminder = { viewModel.toggleReminder(it) }
                        )
                    }

                    composable(ROUTE_NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notifications,
                            onMarkAsRead = { viewModel.markNotificationRead(it) },
                            onMarkAllRead = { viewModel.markAllNotificationsRead() },
                            onClearAll = { viewModel.clearNotifications() }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onToggleOnlineMode = { viewModel.toggleNetworkState() },
                            onTogglePushNotifications = { viewModel.setPushNotificationsEnabled(it) },
                            onToggleCycleCompleteAlert = { viewModel.setCycleCompleteAlert(it) },
                            onToggleEndingSoonAlert = { viewModel.setEndingSoonAlert(it) },
                            onToggleSoundVibration = { viewModel.setSoundAndVibration(it) },
                            onSelectThemeMode = { viewModel.setThemeMode(it) },
                            onSendTestNotification = { viewModel.sendTestNotification() },
                            onResetDefaultMachines = { viewModel.resetDefaultMachines() },
                            buildReportText = { viewModel.buildStatusReportText() }
                        )
                    }
                }
            }
        }
    }

    if (showAddMachineSheet) {
        AddMachineBottomSheet(
            onDismiss = { showAddMachineSheet = false },
            onAddMachine = { name, model, location, capacity ->
                viewModel.addMachine(name, model, location, capacity)
            }
        )
    }

    val activeMachine = selectedMachineId?.let { id ->
        dashboardState.allMachines.firstOrNull { it.id == id }
    }
    if (activeMachine != null) {
        MachineDetailBottomSheet(
            machine = activeMachine,
            onDismiss = { viewModel.closeMachineSheet() },
            onStartCycle = { program, reminder ->
                viewModel.startCycle(activeMachine, program, reminder)
            },
            onTogglePauseResume = { viewModel.togglePauseResume(activeMachine) },
            onExtendFiveMinutes = { viewModel.extendCycleFiveMinutes(activeMachine) },
            onCompleteNow = { viewModel.completeCycleNow(activeMachine) },
            onToggleMaintenance = { viewModel.toggleMaintenance(activeMachine) },
            onToggleReminder = { viewModel.toggleReminder(activeMachine) },
            onDeleteMachine = { viewModel.deleteMachine(activeMachine) }
        )
    }
}

@Composable
private fun TabIconWithBadge(
    tab: NavTabItem,
    selected: Boolean,
    unreadCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab.route == ROUTE_NOTIFICATIONS && unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (unreadCount > 9) "9+" else unreadCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(tab.labelRes))
        }
    } else {
        Icon(imageVector = icon, contentDescription = stringResource(tab.labelRes))
    }
}
