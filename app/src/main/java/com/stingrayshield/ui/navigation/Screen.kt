package com.stingrayshield.ui.navigation

/**
 * Contains all navigation routes for the StingrayShield app
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Scanner : Screen("scanner")
    object Map : Screen("map")
    object Settings : Screen("settings")
    object StingrayDevices : Screen("stingray_devices")
    object Statistics : Screen("statistics")
    
    object EventDetails : Screen("event_details/{eventId}") {
        fun createRoute(eventId: Long) = "event_details/$eventId"
    }
    
    object CellTowerDetails : Screen("cell_tower_details/{cellId}") {
        fun createRoute(cellId: Int) = "cell_tower_details/$cellId"
    }
    
    object ThreatResponse : Screen("threat_response/{eventId}") {
        fun createRoute(eventId: Long) = "threat_response/$eventId"
    }
}
