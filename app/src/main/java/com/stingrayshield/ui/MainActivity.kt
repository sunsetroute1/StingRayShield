package com.stingrayshield.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.stingrayshield.service.DetectorService
import com.stingrayshield.ui.components.PermissionScreen
import com.stingrayshield.ui.components.areAllRequiredPermissionsGranted
import com.stingrayshield.ui.navigation.Screen
import com.stingrayshield.ui.navigation.StingrayNavHost
import com.stingrayshield.ui.navigation.StingrayNavigationBar
import com.stingrayshield.ui.screens.settings.SettingsViewModel
import com.stingrayshield.ui.theme.StingrayShieldTheme
import com.stingrayshield.ui.viewmodel.MainViewModel
import com.stingrayshield.util.ThreatNotificationManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // Track the nav controller for intent handling
    private var navController: NavHostController? = null
    
    // Track pending navigation from intent
    private var pendingThreatEventId: Long? = null
    private var pendingAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if launched from notification
        handleIntent(intent)
        
        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()

            StingrayShieldTheme(
                darkTheme = when {
                    settingsState.useSystemTheme -> isSystemInDarkTheme()
                    else -> settingsState.darkTheme
                },
                useCustomColors = true
            ) {
                StingrayAppWithPermissions(
                    onStartService = { startDetectorService() },
                    onStopService = { stopDetectorService() },
                    onNavControllerReady = { controller ->
                        navController = controller
                        // Handle pending navigation if any
                        handlePendingNavigation(controller)
                    }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        
        // If nav controller is ready, navigate immediately
        navController?.let { handlePendingNavigation(it) }
    }
    
    /**
     * Handle incoming intent from notification
     */
    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ThreatNotificationManager.ACTION_OPEN_THREAT_DETAILS -> {
                pendingThreatEventId = intent.getLongExtra(ThreatNotificationManager.EXTRA_THREAT_EVENT_ID, 0L)
                pendingAction = ThreatNotificationManager.ACTION_OPEN_THREAT_DETAILS
            }
            ThreatNotificationManager.ACTION_ENABLE_AIRPLANE_MODE -> {
                pendingAction = ThreatNotificationManager.ACTION_ENABLE_AIRPLANE_MODE
                // Open airplane mode settings immediately
                openAirplaneModeSettings()
            }
        }
    }
    
    /**
     * Navigate to threat response screen if pending
     */
    private fun handlePendingNavigation(navController: NavHostController) {
        when (pendingAction) {
            ThreatNotificationManager.ACTION_OPEN_THREAT_DETAILS -> {
                pendingThreatEventId?.let { eventId ->
                    if (eventId > 0) {
                        navController.navigate(Screen.ThreatResponse.createRoute(eventId)) {
                            // Pop up to dashboard to avoid back stack issues
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                }
                // Clear pending action
                pendingThreatEventId = null
                pendingAction = null
            }
        }
    }
    
    /**
     * Open airplane mode settings
     */
    private fun openAirplaneModeSettings() {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Please enable Airplane Mode in Settings", Toast.LENGTH_LONG).show()
            }
        }
        // Clear pending action
        pendingAction = null
    }
    
    override fun onResume() {
        super.onResume()
        // Update permission state when returning from settings
        viewModel.checkPermissions(this)
    }
    
    /**
     * Starts the detector service
     */
    private fun startDetectorService() {
        val serviceIntent = Intent(this, DetectorService::class.java).apply {
            action = DetectorService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        viewModel.updateServiceRunningState(true)
    }
    
    /**
     * Stops the detector service
     */
    private fun stopDetectorService() {
        val serviceIntent = Intent(this, DetectorService::class.java).apply {
            action = DetectorService.ACTION_STOP_SERVICE
        }
        stopService(serviceIntent)
        viewModel.updateServiceRunningState(false)
    }
}

@Composable
fun StingrayAppWithPermissions(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavControllerReady: (NavHostController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track if permissions are granted - recheck when app resumes
    var permissionsGranted by remember {
        mutableStateOf(areAllRequiredPermissionsGranted(context))
    }

    // Re-check permissions when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newPermissionState = areAllRequiredPermissionsGranted(context)
                if (newPermissionState != permissionsGranted) {
                    permissionsGranted = newPermissionState
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show permission screen if permissions not granted
    if (!permissionsGranted) {
        PermissionScreen(
            onAllPermissionsGranted = {
                permissionsGranted = true
                // Start the detector service once permissions are granted
                onStartService()
            }
        )
    } else {
        // Start service if not already running (for when app is reopened)
        LaunchedEffect(permissionsGranted) {
            if (permissionsGranted) {
                onStartService()
            }
        }
        StingrayApp(onNavControllerReady = onNavControllerReady)
    }
}

@Composable
fun StingrayApp(
    onNavControllerReady: (NavHostController) -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Notify parent when nav controller is ready
    LaunchedEffect(navController) {
        onNavControllerReady(navController)
    }

    Scaffold(
        bottomBar = {
            StingrayNavigationBar(navController = navController)
        },
        topBar = {
            // Show permission status banner if permissions are missing
            if (!areAllRequiredPermissionsGranted(context)) {
                com.stingrayshield.ui.components.PermissionStatusBanner(
                    onRequestPermissions = {
                        // Navigate to a permission settings screen or show dialog
                        // For now, just show a toast
                        Toast.makeText(context, "Please go to Settings > Permissions to grant required permissions", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            StingrayNavHost(navController = navController)
        }
    }
}
