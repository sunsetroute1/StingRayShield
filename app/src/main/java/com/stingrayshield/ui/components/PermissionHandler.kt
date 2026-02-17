package com.stingrayshield.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Data class representing a permission with its details
 */
data class PermissionInfo(
    val permission: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean = true,
    val minSdk: Int = 1
)

/**
 * All permissions required by StingrayShield
 */
object AppPermissions {
    
    val locationPermissions = listOf(
        PermissionInfo(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            name = "Precise Location",
            description = "Required to detect cell tower locations and display them on the map. Essential for stingray detection accuracy.",
            icon = Icons.Default.MyLocation,
            isRequired = true
        ),
        PermissionInfo(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            name = "Approximate Location",
            description = "Fallback location access for basic cell tower detection.",
            icon = Icons.Default.LocationOn,
            isRequired = true
        )
    )
    
    val backgroundLocationPermission = PermissionInfo(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        name = "Background Location",
        description = "Allows continuous stingray monitoring even when the app is in the background. Highly recommended for full protection.",
        icon = Icons.Default.LocationSearching,
        isRequired = false,
        minSdk = Build.VERSION_CODES.Q
    )
    
    val phonePermissions = listOf(
        PermissionInfo(
            permission = Manifest.permission.READ_PHONE_STATE,
            name = "Phone State",
            description = "Required to read cellular network information, cell tower IDs, signal strength, and detect network changes. Core functionality.",
            icon = Icons.Default.PhoneAndroid,
            isRequired = true
        )
    )
    
    val notificationPermission = PermissionInfo(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        name = "Notifications",
        description = "Required to alert you when a potential stingray device is detected. Critical for real-time protection.",
        icon = Icons.Default.Notifications,
        isRequired = true,
        minSdk = Build.VERSION_CODES.TIRAMISU
    )
    
    /**
     * Get all permissions that need to be requested
     */
    fun getAllPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        
        // Location permissions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        // Phone state
        permissions.add(Manifest.permission.READ_PHONE_STATE)
        
        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        return permissions
    }
    
    /**
     * Get background location permission separately (must be requested after foreground location)
     */
    fun getBackgroundLocationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else null
    }
}

/**
 * Check if a permission is granted
 */
fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check if all required permissions are granted
 */
fun areAllRequiredPermissionsGranted(context: Context): Boolean {
    return AppPermissions.getAllPermissions().all { isPermissionGranted(context, it) }
}

/**
 * Check if background location permission is granted
 */
fun isBackgroundLocationGranted(context: Context): Boolean {
    return AppPermissions.getBackgroundLocationPermission()?.let { permission ->
        isPermissionGranted(context, permission)
    } ?: true
}

/**
 * Check if we should show permission rationale
 */
fun shouldShowPermissionRationale(context: Context, permission: String): Boolean {
    val activity = context as? androidx.activity.ComponentActivity ?: return false
    return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

/**
 * Composable that handles permission requests with a nice UI
 */
@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission states
    var fineLocationGranted by remember { mutableStateOf(isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var coarseLocationGranted by remember { mutableStateOf(isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    var phoneStateGranted by remember { mutableStateOf(isPermissionGranted(context, Manifest.permission.READ_PHONE_STATE)) }
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
            } else true
        )
    }
    var backgroundLocationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isPermissionGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else true
        )
    }

    // Rationale states
    var showLocationRationale by remember { mutableStateOf(false) }
    var showPhoneRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    // Check if all required permissions are granted
    val allRequiredGranted = fineLocationGranted && phoneStateGranted && notificationGranted
    
    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        phoneStateGranted = granted
    }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }
    
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundLocationGranted = granted
    }
    
    // Check if we should proceed
    LaunchedEffect(allRequiredGranted) {
        if (allRequiredGranted) {
            onAllPermissionsGranted()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "StingrayShield needs the following permissions to protect you from surveillance devices.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Location Permission Card
        PermissionCard(
            title = "Location Access",
            description = "Required to detect cell tower locations and display them on the map. Essential for stingray detection.",
            icon = Icons.Default.LocationOn,
            isGranted = fineLocationGranted || coarseLocationGranted,
            isRequired = true,
            onRequestPermission = {
                if (shouldShowPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showLocationRationale = true
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Phone State Permission Card
        PermissionCard(
            title = "Phone State",
            description = "Required to read cellular network information, cell tower IDs, and signal strength. Core functionality.",
            icon = Icons.Default.PhoneAndroid,
            isGranted = phoneStateGranted,
            isRequired = true,
            onRequestPermission = {
                if (shouldShowPermissionRationale(context, Manifest.permission.READ_PHONE_STATE)) {
                    showPhoneRationale = true
                } else {
                    phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Notification Permission Card (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                title = "Notifications",
                description = "Required to alert you when a potential stingray device is detected. Critical for real-time protection.",
                icon = Icons.Default.Notifications,
                isGranted = notificationGranted,
                isRequired = true,
                onRequestPermission = {
                    if (shouldShowPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationRationale = true
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Background Location Permission Card (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (fineLocationGranted || coarseLocationGranted)) {
            PermissionCard(
                title = "Background Location",
                description = "Allows continuous monitoring even when the app is closed. Recommended for full protection.",
                icon = Icons.Default.LocationSearching,
                isGranted = backgroundLocationGranted,
                isRequired = false,
                onRequestPermission = {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Continue button
        if (allRequiredGranted) {
            Button(
                onClick = onAllPermissionsGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Continue to App", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // Show settings button if permissions were denied
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Open App Settings", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info text
        Text(
            text = "Your privacy is important. These permissions are used only for stingray detection and are never shared.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    // Rationale Dialogs
    if (showLocationRationale) {
        PermissionRationaleDialog(
            title = "Location Permission Needed",
            message = "StingrayShield needs location access to:\n\n• Detect cell tower locations\n• Display towers on the map\n• Calculate distances for threat analysis\n\nWithout this permission, the app cannot function properly.",
            onConfirm = {
                showLocationRationale = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { showLocationRationale = false }
        )
    }

    if (showPhoneRationale) {
        PermissionRationaleDialog(
            title = "Phone State Permission Needed",
            message = "StingrayShield needs phone state access to:\n\n• Read cellular network information\n• Detect cell tower changes\n• Monitor signal strength\n• Identify potential stingray devices\n\nThis is core functionality for the app.",
            onConfirm = {
                showPhoneRationale = false
                phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            },
            onDismiss = { showPhoneRationale = false }
        )
    }

    if (showNotificationRationale) {
        PermissionRationaleDialog(
            title = "Notification Permission Needed",
            message = "StingrayShield needs notification access to:\n\n• Alert you of detected threats\n• Show real-time warnings\n• Notify about stingray devices\n\nNotifications are critical for your security.",
            onConfirm = {
                showNotificationRationale = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { showNotificationRationale = false }
        )
    }
}

/**
 * Card showing a single permission with its status
 */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    isRequired: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Status/Action
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

/**
 * Compact permission status indicator for use in other screens
 */
@Composable
fun PermissionStatusBanner(
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val allGranted = areAllRequiredPermissionsGranted(context)
    
    if (!allGranted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Some permissions are missing. Tap to fix.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                
                TextButton(onClick = onRequestPermissions) {
                    Text("Fix")
                }
            }
        }
    }
}

/**
 * Dialog explaining why a permission is needed
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

