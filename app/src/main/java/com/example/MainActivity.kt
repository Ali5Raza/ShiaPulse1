package com.example

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.*
import com.example.ui.theme.ShiaPulseTheme
import com.example.receiver.MasoomEventScheduler
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var orientationSensor: Sensor? = null

    // Azimuth state holding current direction (0=North, 90=East...)
    private val azimuthState = MutableStateFlow(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashScreen = true
        lifecycleScope.launch {
            delay(50L) // Hide standard splash quickly
            keepSplashScreen = false
        }
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Sensors
        try {
            val systemService = getSystemService(Context.SENSOR_SERVICE)
            if (systemService != null) {
                sensorManager = systemService as SensorManager
                rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                if (rotationVectorSensor == null) {
                    // Fallback to old orientation sensor if gyro/rotation is not present
                    orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Failed to setup sensorManager on this hardware.", t)
        }

        // Schedule daily check for 14 Masoomeen lunar calendar events
        MasoomEventScheduler.scheduleDailyCheck(this)
        com.example.receiver.AamalEventScheduler.scheduleNext(this)
        com.example.utils.HijriCalendarHelper.syncOffset(this)
        
        lifecycleScope.launch {
            try {
                val countryService = com.example.utils.DefaultCountryDetectionService(this@MainActivity)
                val remoteConfigService = com.example.utils.DefaultRemoteConfigurationService(this@MainActivity)
                val hijriConfigService = com.example.utils.DefaultHijriConfigurationService(countryService, remoteConfigService)
                
                // Initial resolution
                hijriConfigService.resolveAndApplyOffset(this@MainActivity)
                
                // Start real-time remote configuration listener for immediate updates across all users
                remoteConfigService.startRealtimeSync { updatedConfig ->
                    hijriConfigService.applyRemoteConfigDirectly(this@MainActivity, updatedConfig)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error setting up remote config sync", e)
            }
            
            com.example.utils.HijriCalendarHelper.fetchAndSyncOffsetFromApi(this@MainActivity)
        }

        // Trigger widget and notification bar calendar update on launch
        try {
            val widgetIntent = Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                component = android.content.ComponentName(this@MainActivity, com.example.receiver.HijriWidgetProvider::class.java)
            }
            sendBroadcast(widgetIntent)
            com.example.receiver.CalendarNotificationManager.showOrUpdateCalendarNotification(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val viewModel: PrayerViewModel = viewModel()
            
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshHomeClock()
                        viewModel.refreshDailyContentIfNeeded()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            val autoOccasionTheme by viewModel.autoOccasionTheme.collectAsState()
            val themeOverride by viewModel.themeOverride.collectAsState()
            val eventsRevision by com.example.data.IslamicEventManager.eventsRevision.collectAsState()
            val homeDayContext by viewModel.homeDayContext.collectAsState()

            val context = this

            // Calculate active occasion event type based on automatic detection or manual test overrides
            val activeOccasionType = remember(
                autoOccasionTheme,
                themeOverride,
                homeDayContext.hijriDate,
                eventsRevision
            ) {
                when (themeOverride) {
                    "wiladat" -> com.example.data.EventType.WILADAT
                    "shahadat" -> com.example.data.EventType.SHAHADAT
                    else -> {
                        if (autoOccasionTheme) {
                            val hijriDate = homeDayContext.hijriDate
                            val allEvents = com.example.data.IslamicEventManager.getFilteredEvents(context)
                            val todaysEvents = allEvents.filter {
                                it.occursOn(hijriDate.day, hijriDate.month, hijriDate.monthName)
                            }
                            when {
                                todaysEvents.any { it.eventType == com.example.data.EventType.SHAHADAT } -> com.example.data.EventType.SHAHADAT
                                todaysEvents.any { it.eventType == com.example.data.EventType.WILADAT } -> com.example.data.EventType.WILADAT
                                else -> null
                            }
                        } else {
                            null
                        }
                    }
                }
            }

            val appFontName by viewModel.appFont.collectAsState()
            val languageCode by viewModel.appLanguage.collectAsState()
            
            val activeFontFamily = remember(appFontName, languageCode) {
                com.example.ui.theme.getAppFontFamily(appFontName, languageCode)
            }

            val activeLayoutDirection = remember(languageCode) {
                if (languageCode == "ar" || languageCode == "ur") {
                    androidx.compose.ui.unit.LayoutDirection.Rtl
                } else {
                    androidx.compose.ui.unit.LayoutDirection.Ltr
                }
            }

            var showComposeSplash by remember { mutableStateOf(true) }

            // Request Notification Permission on Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            ShiaPulseTheme(
                darkTheme = isDark,
                eventType = activeOccasionType,
                appFontFamily = activeFontFamily,
                layoutDirection = activeLayoutDirection
            ) {
                val azimuth by azimuthState.collectAsState()

                if (showComposeSplash) {
                    AnimatedSplashScreen(onSplashFinish = { showComposeSplash = false })
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        DashboardScreen(
                            viewModel = viewModel,
                            azimuth = azimuth,
                            eventType = activeOccasionType,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!::sensorManager.isInitialized) return
        try {
            // Register orientation sensors
            rotationVectorSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            } ?: orientationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error registering sensors", t)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!::sensorManager.isInitialized) return
        try {
            // Unregister to save battery
            sensorManager.unregisterListener(this)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error unregistering sensors", t)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        try {
            val sensorType = event.sensor?.type ?: return
            if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                val azimuthRad = orientationValues[0]
                val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                azimuthState.value = (azimuthDeg + 360f) % 360f
            } else if (sensorType == Sensor.TYPE_ORIENTATION) {
                // Legacy/Virtual fallback
                azimuthState.value = event.values[0]
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error handling sensor changed", t)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for current compass direction calculations
    }
}
