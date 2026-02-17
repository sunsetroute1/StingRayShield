package com.stingrayshield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stingrayshield.ui.screens.dashboard.DashboardScreen
import com.stingrayshield.ui.screens.scanner.ScannerScreen
import com.stingrayshield.ui.screens.map.MapScreen
import com.stingrayshield.ui.screens.settings.SettingsScreen
import com.stingrayshield.ui.screens.details.EventDetailsScreen
import com.stingrayshield.ui.screens.details.CellTowerDetailsScreen
import com.stingrayshield.ui.screens.devices.StingrayDevicesScreen
import com.stingrayshield.ui.screens.statistics.StatisticsScreen
import com.stingrayshield.ui.screens.threat.ThreatResponseScreen
import com.stingrayshield.ui.screens.threat.ThreatResponseViewModel

/**
 * Main navigation host for the StingrayShield app
 */
@Composable
fun StingrayNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // Dashboard screen - the main screen showing the current status
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navigateToEventDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                },
                navigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                viewModel = hiltViewModel()
            )
        }
        
        // Statistics screen - detailed detection and tower statistics
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEvent = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                },
                onNavigateToCellTower = { cellId ->
                    navController.navigate(Screen.CellTowerDetails.createRoute(cellId))
                },
                viewModel = hiltViewModel()
            )
        }
        
        // Scanner screen - real-time scanning and detection
        composable(Screen.Scanner.route) {
            ScannerScreen(
                navigateToCellTowerDetails = { cellId ->
                    navController.navigate(Screen.CellTowerDetails.createRoute(cellId))
                },
                viewModel = hiltViewModel()
            )
        }
        
        // Map screen - shows cell towers and detection events on a map
        composable(Screen.Map.route) {
            MapScreen(
                navigateToEventDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                },
                navigateToCellTowerDetails = { cellId ->
                    navController.navigate(Screen.CellTowerDetails.createRoute(cellId))
                },
                viewModel = hiltViewModel()
            )
        }
        
        // Settings screen - configure app settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onNavigateToStingrayDevices = {
                    navController.navigate(Screen.StingrayDevices.route)
                }
            )
        }
        
        // Stingray Devices screen - view identified stingray devices
        composable(Screen.StingrayDevices.route) {
            StingrayDevicesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        // Event details screen - shows details for a specific detection event
        composable(
            route = Screen.EventDetails.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
            EventDetailsScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        // Cell tower details screen - shows details for a specific cell tower
        composable(
            route = Screen.CellTowerDetails.route,
            arguments = listOf(
                navArgument("cellId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val cellId = backStackEntry.arguments?.getInt("cellId") ?: 0
            CellTowerDetailsScreen(
                cellId = cellId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        // Threat response screen - shows full threat details and response options
        composable(
            route = Screen.ThreatResponse.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
            val viewModel: ThreatResponseViewModel = hiltViewModel()
            
            // Load the event
            androidx.compose.runtime.LaunchedEffect(eventId) {
                viewModel.loadEvent(eventId)
            }
            
            val currentEvent by viewModel.currentEvent.collectAsState()
            
            ThreatResponseScreen(
                event = currentEvent,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
