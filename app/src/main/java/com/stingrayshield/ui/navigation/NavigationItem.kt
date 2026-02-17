package com.stingrayshield.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class for navigation items in the bottom navigation bar
 */
data class NavigationItem(
    val title: Int, // String resource ID
    val icon: ImageVector,
    val route: String
)
