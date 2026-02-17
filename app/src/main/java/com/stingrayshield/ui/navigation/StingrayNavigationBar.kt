package com.stingrayshield.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stingrayshield.R

/**
 * Bottom navigation bar for the StingrayShield app
 */
@Composable
fun StingrayNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem(
            title = R.string.nav_dashboard,
            icon = Icons.Filled.Dashboard,
            route = Screen.Dashboard.route
        ),
        NavigationItem(
            title = R.string.nav_scanner,
            icon = Icons.Filled.Search,
            route = Screen.Scanner.route
        ),
        NavigationItem(
            title = R.string.nav_map,
            icon = Icons.Filled.Map,
            route = Screen.Map.route
        ),
        NavigationItem(
            title = R.string.nav_settings,
            icon = Icons.Filled.Settings,
            route = Screen.Settings.route
        )
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Only show the navigation bar on main screens
    val shouldShowNavBar = items.any { it.route == currentRoute }
    
    if (shouldShowNavBar) {
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = stringResource(item.title)) },
                    label = { Text(text = stringResource(item.title)) },
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to avoid building up
                                // a large stack of destinations
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}
