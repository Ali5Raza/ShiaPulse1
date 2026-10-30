package com.example.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.ZiyaratLocation
import com.example.ui.ziyaratLocations
import com.example.data.ZiyaratVisitRecord
import com.example.data.ZiyaratJournalRecord
import com.example.data.ExpandedMasoomData
import com.example.data.MasoomDetails
import com.example.data.MasoomeenAssetLoader
import com.example.data.MasoomeenData
import com.example.utils.LocalizationUtility
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

// Geolocation bridge interface
class WebAppInterface(
    private val onLocationDetected: (Double, Double) -> Unit,
    private val onToggleVisit: (String, String) -> Unit,
    private val onSelectMarker: (String) -> Unit
) {
    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val json = JSONObject(message)
            if (json.has("action")) {
                val action = json.getString("action")
                if (action == "toggleVisit") {
                    val siteId = json.getString("siteId")
                    val type = json.getString("type")
                    onToggleVisit(siteId, type)
                } else if (action == "selectMarker") {
                    val siteId = json.getString("siteId")
                    onSelectMarker(siteId)
                }
            } else if (json.has("lat") && json.has("lon")) {
                val lat = json.getDouble("lat")
                val lon = json.getDouble("lon")
                onLocationDetected(lat, lon)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

val mapLeafletHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        html, body, #map {
            height: 100%;
            margin: 0;
            padding: 0;
            background-color: #0F172A;
        }
        .leaflet-popup-content-wrapper {
            background-color: #1E293B !important;
            color: #FFFFFF !important;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.5);
            font-family: system-ui, -apple-system, sans-serif;
            border: 1px solid rgba(255,255,255,0.1);
        }
        .leaflet-popup-tip {
            background-color: #1E293B !important;
        }
        .popup-title {
            font-weight: bold;
            font-size: 15px;
            color: #38BDF8;
            margin-bottom: 4px;
        }
        .popup-city {
            font-size: 11px;
            font-weight: 600;
            color: #34D399;
            text-transform: uppercase;
            margin-bottom: 4px;
        }
        .popup-distance {
            font-size: 12px;
            font-weight: bold;
            color: #FBBF24;
            margin-bottom: 8px;
        }
        .popup-desc {
            font-size: 11px;
            line-height: 1.5;
            color: #E2E8F0;
        }
        .pulse-circle {
            background-color: #3B82F6;
            width: 14px;
            height: 14px;
            border-radius: 50%;
            border: 2px solid #FFFFFF;
            animation: pulse 1.6s infinite ease-in-out;
            box-shadow: 0 0 12px rgba(59, 130, 246, 0.8);
        }
        @keyframes pulse {
            0% { transform: scale(0.9); opacity: 1; box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7); }
            70% { transform: scale(1.1); opacity: 0.8; box-shadow: 0 0 0 8px rgba(59, 130, 246, 0); }
            100% { transform: scale(0.9); opacity: 1; box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map', {
            zoomControl: false
        }).setView([32.5, 45.0], 4);
        
        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; OpenStreetMap &copy; CARTO',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        L.control.zoom({
            position: 'topright'
        }).addTo(map);

        var markers = {};
        var locationsData = [];
        var userLat = %DEFAULT_USER_LAT%;
        var userLon = %DEFAULT_USER_LON%;
        var userLocationSource = "%DEFAULT_USER_SOURCE%";
        var userMarker = null;

        function getHaversineDistance(lat1, lon1, lat2, lon2) {
            var R = 6371; // Radius of the earth in km
            var dLat = (lat2 - lat1) * Math.PI / 180;
            var dLon = (lon2 - lon1) * Math.PI / 180;
            var a = 
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
                Math.sin(dLon/2) * Math.sin(dLon/2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            var d = R * c; // Distance in km
            return d;
        }

        function formatDistance(distKm) {
            var miles = distKm * 0.621371;
            var isUr = "%LANG%" === "ur";
            var isAr = "%LANG%" === "ar";
            if (isUr) {
                return "فاصلہ: " + Math.round(distKm).toLocaleString() + " کلومیٹر (" + Math.round(miles).toLocaleString() + " میل) دور";
            } else if (isAr) {
                return "المسافة: " + Math.round(distKm).toLocaleString() + " كم (" + Math.round(miles).toLocaleString() + " ميل) تقريباً";
            } else {
                return "Distance: " + Math.round(distKm).toLocaleString() + " km (" + Math.round(miles).toLocaleString() + " miles) away";
            }
        }

        function updatePopups() {
            // Decoupled from map popups as all detail is gracefully rendered below the map in Jetpack Compose
        }

        function addLocation(id, lat, lon, title, city, desc, tips, virtuallyVisited, physicallyVisited) {
            locationsData.push({
                id: id,
                lat: lat,
                lon: lon,
                title: title,
                city: city,
                desc: desc,
                tips: tips,
                virtuallyVisited: virtuallyVisited || false,
                physicallyVisited: physicallyVisited || false
            });

            var customIcon = L.divIcon({
                className: 'custom-ziyarat-marker',
                html: '<div style="background: linear-gradient(135deg, #FFD700, #D4AF37); width: 34px; height: 34px; border-radius: 50% 50% 50% 0; transform: rotate(-45deg); border: 2px solid #1E1E1E; box-shadow: 0 4px 10px rgba(212,175,55,0.6); display: flex; align-items: center; justify-content: center;"><div style="transform: rotate(45deg); display: flex; align-items: center; justify-content: center; width: 100%; height: 100%;"><svg viewBox="0 0 24 24" width="16" height="16" fill="#1E1E1E" style="margin-top: 1px;"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg></div></div>',
                iconSize: [34, 34],
                iconAnchor: [17, 34]
            });

            var marker = L.marker([lat, lon], {icon: customIcon}).addTo(map);
            marker.on('click', function() {
                if (window.AndroidBridge) {
                    window.AndroidBridge.postMessage(JSON.stringify({
                        action: "selectMarker",
                        siteId: id
                    }));
                }
            });
            markers[id] = marker;
        }

        function toggleVisitJS(siteId, type) {
            var loc = locationsData.find(function(l) { return l.id === siteId; });
            if (loc) {
                if (type === 'virtual') {
                    loc.virtuallyVisited = !loc.virtuallyVisited;
                } else if (type === 'physical') {
                    loc.physicallyVisited = !loc.physicallyVisited;
                }
                
                if (window.AndroidBridge) {
                    window.AndroidBridge.postMessage(JSON.stringify({
                        action: "toggleVisit",
                        siteId: siteId,
                        type: type
                    }));
                }
                updatePopups();
            }
        }

        function updateVisitStatus(siteId, virtuallyVisited, physicallyVisited) {
            var loc = locationsData.find(function(l) { return l.id === siteId; });
            if (loc) {
                loc.virtuallyVisited = virtuallyVisited;
                loc.physicallyVisited = physicallyVisited;
                updatePopups();
            }
        }

        function drawUserLocation() {
            if (userLat === null || userLon === null) return;
            
            if (userMarker) {
                map.removeLayer(userMarker);
            }
            
            var sourceText = userLocationSource === "GPS Device" ? 
                ("%LANG%" === "ur" ? "آپ کا موجودہ موبائل مقام" : "%LANG%" === "ar" ? "موقعك الحالي الفعلي" : "Your Real GPS Location") :
                ("%LANG%" === "ur" ? "آپ کا منتخب کردہ شہر" : "%LANG%" === "ar" ? "مدينتك المختارة" : "Your Selected City Preset");

            var userIcon = L.divIcon({
                className: 'user-location-marker',
                html: '<div class="pulse-circle"></div>',
                iconSize: [20, 20],
                iconAnchor: [10, 10]
            });
            
            userMarker = L.marker([userLat, userLon], {icon: userIcon, zIndexOffset: 1000}).addTo(map)
                .bindPopup('<b style="color:#60A5FA; font-family: sans-serif; display: flex; align-items: center; gap: 4px;"><svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg> ' + sourceText + '</b>');
        }

        function focusLocation(id, lat, lon) {
            map.flyTo([lat, lon], 12, {
                animate: true,
                duration: 1.5
            });
        }

        // Injected locations:
        %MARKERS%

        // Draw default user position (Preset Home)
        drawUserLocation();
        updatePopups();

        // Query actual HTML5 device GPS Geolocation
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(position) {
                userLat = position.coords.latitude;
                userLon = position.coords.longitude;
                userLocationSource = "GPS Device";
                drawUserLocation();
                updatePopups();
                
                // Expose coords back to Android via JS bridge
                if (window.AndroidBridge) {
                    window.AndroidBridge.postMessage(JSON.stringify({
                        lat: userLat,
                        lon: userLon
                    }));
                }
            }, function(err) {
                console.log("HTML5 location query failed/denied.");
            }, {
                enableHighAccuracy: true,
                timeout: 10000
            });
        }
    </script>
</body>
</html>
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZiyaratsMapScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    languageCode: String = "en",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLocationPreset by viewModel.selectedLocation.collectAsState()
    val visitRecords by viewModel.allZiyaratVisits.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val localizedMasoomeen = remember(context, languageCode) {
        MasoomeenAssetLoader.loadMasoomeenData(context, languageCode).associateBy { it.id }
    }
    val infallibleCoverage = remember {
        MasoomeenData.list.mapNotNull { masoom ->
            ziyaratLocations.firstOrNull { location -> masoom.index in location.masoomeenIndices }
                ?.let { location -> masoom to location }
        }
    }
    val totalMasoomCount = MasoomeenData.list.size

    val totalCount = ziyaratLocations.size
    val (virtCount, physCount) = remember(visitRecords) {
        val virtSet = visitRecords.filter { it.virtuallyVisited }.map { it.siteId }.toSet()
        val physSet = visitRecords.filter { it.physicallyVisited }.map { it.siteId }.toSet()
        Pair(
            ziyaratLocations.count { it.id in virtSet },
            ziyaratLocations.count { it.id in physSet }
        )
    }

    var selectedLocId by remember { mutableStateOf(ziyaratLocations.first().id) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Coordinates tracker in Compose
    var userCoordinates by remember { 
        mutableStateOf<Pair<Double, Double>>(Pair(selectedLocationPreset.lat, selectedLocationPreset.lon)) 
    }
    var trackingSource by remember { mutableStateOf("Preset City") } // "Preset City", "GPS Active"

    // Sync Compose database changes back into WebView JavaScript dynamically
    LaunchedEffect(visitRecords) {
        visitRecords.forEach { record ->
            webViewRef?.evaluateJavascript(
                "javascript:updateVisitStatus('${record.siteId}', ${record.virtuallyVisited}, ${record.physicallyVisited})",
                null
            )
        }
    }

    // Launcher for location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            // Trigger webview reload to fetch GPS
            webViewRef?.reload()
        }
    }

    // Request permissions on startup
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (languageCode == "ur") "جغرافیہ و زبانی نقشہ" else if (languageCode == "ar") "الخريطة التفاعلية للزيارات" else "Interactive Ziyarats Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (languageCode == "ur") "مقدس مزارات اور تاریخی مقامات کا دوری کیلکولیٹر" else if (languageCode == "ar") "تصفح العتبات المقدسة وحساب المسافات" else "Distance calculator to holy shrines and heritage sites",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ziyarats_map_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = if (trackingSource == "GPS Active") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (trackingSource == "GPS Active") Icons.Default.MyLocation else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (trackingSource == "GPS Active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (trackingSource == "GPS Active") {
                                if (languageCode == "ur") "GPS فعال" else if (languageCode == "ar") "GPS نشط" else "GPS Active"
                            } else {
                                if (languageCode == "ur") "قریبی شہر" else if (languageCode == "ar") "الافتراضي" else "Home City"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (trackingSource == "GPS Active") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("ziyarats_map_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ziyarat Progress Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("ziyarat_progress_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (languageCode == "ur") "🎯 زیارات اہم پیشرفت" else if (languageCode == "ar") "🎯 حصيلة زياراتك للمراقد" else "🎯 Your Ziyarat Progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (languageCode == "ur") "مقامات کی حاضری کا مبارک اور الیکٹرانک ریکارڈ" else if (languageCode == "ar") "سجل محفوظ لجميع زياراتك الميدانية والافتراضية" else "Blessed digital record of your sacred check-ins",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = when (languageCode) {
                                "ur" -> "معصومینؑ: ${infallibleCoverage.size} / $totalMasoomCount"
                                "ar" -> "المعصومون: ${infallibleCoverage.size} / $totalMasoomCount"
                                else -> "Infallibles: ${infallibleCoverage.size} / $totalMasoomCount"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = if (languageCode == "ur") "تصوراتی: $virtCount / $totalCount" else if (languageCode == "ar") "افتراضية: $virtCount / $totalCount" else "Virtual: $virtCount / $totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (languageCode == "ur") "عملی: $physCount / $totalCount" else if (languageCode == "ar") "ميدانية: $physCount / $totalCount" else "Physical: $physCount / $totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Interactive Map View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setGeolocationEnabled(true)
                            
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?,
                                    callback: android.webkit.GeolocationPermissions.Callback?
                                ) {
                                    callback?.invoke(origin, true, false)
                                }
                            }
                            
                            addJavascriptInterface(WebAppInterface(
                                onLocationDetected = { lat, lon ->
                                    coroutineScope.launch {
                                        userCoordinates = Pair(lat, lon)
                                        trackingSource = "GPS Active"
                                    }
                                },
                                onToggleVisit = { siteId, type ->
                                    viewModel.toggleZiyaratVisit(siteId, type)
                                },
                                onSelectMarker = { siteId ->
                                    coroutineScope.launch {
                                        selectedLocId = siteId
                                    }
                                }
                            ), "AndroidBridge")
                            
                            val markersJs = buildString {
                                ziyaratLocations.forEach { loc ->
                                    val title = when (languageCode) {
                                        "ur" -> loc.nameUr
                                        "ar" -> loc.nameAr
                                        else -> loc.nameEn
                                    }
                                    val city = when (languageCode) {
                                        "ur" -> loc.cityUr
                                        "ar" -> loc.cityAr
                                        else -> loc.cityEn
                                    }
                                    val desc = when (languageCode) {
                                        "ur" -> loc.descUr
                                        "ar" -> loc.descAr
                                        else -> loc.descEn
                                    }.replace("'", "\\'")
                                    
                                    val tips = when (languageCode) {
                                        "ur" -> loc.tipsUr
                                        "ar" -> loc.tipsAr
                                        else -> loc.tipsEn
                                    }.replace("'", "\\'")
                                    
                                    val visitRecord = visitRecords.find { it.siteId == loc.id }
                                    val isVirt = visitRecord?.virtuallyVisited ?: false
                                    val isPhys = visitRecord?.physicallyVisited ?: false
                                    
                                    appendLine("addLocation('${loc.id}', ${loc.lat}, ${loc.lon}, '${title.replace("'", "\\'")}', '${city.replace("'", "\\'")}', '$desc', '$tips', $isVirt, $isPhys);")
                                }
                            }
                            
                            var finalHtml = mapLeafletHtml
                                .replace("%MARKERS%", markersJs)
                                .replace("%DEFAULT_USER_LAT%", selectedLocationPreset.lat.toString())
                                .replace("%DEFAULT_USER_LON%", selectedLocationPreset.lon.toString())
                                .replace("%DEFAULT_USER_SOURCE%", "Preset Home")
                                .replace("%LANG%", languageCode)
                            
                            loadDataWithBaseURL("https://openstreetmap.org", finalHtml, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Complete Fourteen Infallibles coverage, using the existing localized biographies.
            Text(
                text = when (languageCode) {
                    "ur" -> "چودہ معصومینؑ — نقشے میں ${infallibleCoverage.size} / $totalMasoomCount"
                    "ar" -> "المعصومون الأربعة عشر — ${infallibleCoverage.size} / $totalMasoomCount على الخريطة"
                    else -> "The Fourteen Infallibles — ${infallibleCoverage.size} / $totalMasoomCount covered"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = when (languageCode) {
                    "ur" -> "کسی نام کو منتخب کر کے متعلقہ مقام، تاریخیں، مدفن اور مختصر سوانح دیکھیں۔"
                    "ar" -> "اختر اسماً لعرض موقع الزيارة والتفاصيل والسيرة المختصرة."
                    else -> "Select a name to open its ziyarat location and localized details."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth().testTag("infallible_coverage_row")
            ) {
                items(infallibleCoverage, key = { it.first.index }) { coverage ->
                    val (masoom, location) = coverage
                    val expanded = localizedMasoomeen[masoom.index]
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (location.id == selectedLocId) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (location.id == selectedLocId) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                Color.Transparent
                            }
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .testTag("infallible_coverage_${masoom.index}")
                            .clickable {
                                selectedLocId = location.id
                                webViewRef?.evaluateJavascript(
                                    "javascript:focusLocation('${location.id}', ${location.lat}, ${location.lon})",
                                    null
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(11.dp)) {
                            Text(
                                text = expanded?.name
                                    ?: LocalizationUtility.translateMasoomName(masoom.name, languageCode),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = expanded?.title ?: masoom.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = when (languageCode) {
                                    "ur" -> location.nameUr
                                    "ar" -> location.nameAr
                                    else -> location.nameEn
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Selection Title
            Text(
                text = if (languageCode == "ur") "مقدس مقامات منتخب کریں:" else if (languageCode == "ar") "اختر موقعاً كمرقداً مقدس:" else "Select Sacred Shrine Location:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Horizontal scrolling row of Ziyarat positions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ziyaratLocations) { loc ->
                    val isSelected = loc.id == selectedLocId
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .width(160.dp)
                            .clickable {
                                selectedLocId = loc.id
                                webViewRef?.evaluateJavascript("javascript:focusLocation('${loc.id}', ${loc.lat}, ${loc.lon})", null)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = when (languageCode) {
                                    "ur" -> loc.cityUr.split("،").firstOrNull() ?: loc.cityUr
                                    "ar" -> loc.cityAr.split("،").firstOrNull() ?: loc.cityAr
                                    else -> loc.cityEn.split(",").firstOrNull() ?: loc.cityEn
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (languageCode) {
                                    "ur" -> loc.nameUr
                                    "ar" -> loc.nameAr
                                    else -> loc.nameEn
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Current selected detail description card
            val currentSelectedLoc = remember(selectedLocId) {
                ziyaratLocations.find { it.id == selectedLocId } ?: ziyaratLocations.first()
            }

            // Haversine calculation in Kotlin to display dynamic text
            val distanceKm = remember(userCoordinates, currentSelectedLoc) {
                calculateDistanceKm(
                    userCoordinates.first, userCoordinates.second,
                    currentSelectedLoc.lat, currentSelectedLoc.lon
                )
            }
            val distanceMiles = distanceKm * 0.621371

            val bearing = remember(userCoordinates, currentSelectedLoc) {
                calculateBearing(
                    userCoordinates.first, userCoordinates.second,
                    currentSelectedLoc.lat, currentSelectedLoc.lon
                )
            }
            val cardinalDir = remember(bearing) {
                getCardinalDirection(bearing, languageCode)
            }

            val journalRecords by viewModel.allZiyaratJournals.collectAsState(initial = emptyList())
            
            var visitDateText by remember { mutableStateOf("") }
            var journalNotesText by remember { mutableStateOf("") }
            var prayRequestsText by remember { mutableStateOf("") }
            var isEditingJournal by remember { mutableStateOf(false) }

            LaunchedEffect(selectedLocId, journalRecords) {
                val entry = journalRecords.find { it.siteId == selectedLocId }
                visitDateText = entry?.visitDate ?: ""
                journalNotesText = entry?.notes ?: ""
                prayRequestsText = entry?.duaRequests ?: ""
                isEditingJournal = entry == null
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (languageCode) {
                                    "ur" -> currentSelectedLoc.nameUr
                                    "ar" -> currentSelectedLoc.nameAr
                                    else -> currentSelectedLoc.nameEn
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "📍 " + when (languageCode) {
                                    "ur" -> currentSelectedLoc.cityUr
                                    "ar" -> currentSelectedLoc.cityAr
                                    else -> currentSelectedLoc.cityEn
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                webViewRef?.evaluateJavascript("javascript:focusLocation('${currentSelectedLoc.id}', ${currentSelectedLoc.lat}, ${currentSelectedLoc.lon})", null)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // TABS SELECTOR FOR CLEAN LOOK
                    var activeDetailTab by remember(selectedLocId) { mutableStateOf(0) }

                    TabRow(
                        selectedTabIndex = activeDetailTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        val tabTitles = if (languageCode == "ur") {
                            listOf("📜 تفصیل", "🧭 سمت مزار", "📝 سفرنامہ")
                        } else if (languageCode == "ar") {
                            listOf("📜 التفاصيل", "🧭 الاتجاه", "📝 مذكرات")
                        } else {
                            listOf("📜 Info", "🧭 Compass", "📝 Journal")
                        }

                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = activeDetailTab == index,
                                onClick = { activeDetailTab = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        fontWeight = if (activeDetailTab == index) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    ) 
                                },
                                modifier = Modifier.testTag("detail_tab_$index")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    when (activeDetailTab) {
                        0 -> {
                            // TAB 1: DESCRIPTION & LOGISTICS/TIPS
                            Text(
                                text = when (languageCode) {
                                    "ur" -> "تاریخی اور روحانی اہمیت:"
                                    "ar" -> "الخلفيّة التّاريخيّة والفضل:"
                                    else -> "Historical & Spiritual Significance:"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            Text(
                                text = when (languageCode) {
                                    "ur" -> currentSelectedLoc.descUr
                                    "ar" -> currentSelectedLoc.descAr
                                    else -> currentSelectedLoc.descEn
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                            
                            val masoomeenForLoc = currentSelectedLoc.masoomeenIndices.mapNotNull { index ->
                                MasoomeenData.list.find { it.index == index }
                            }

                            if (masoomeenForLoc.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = when (languageCode) {
                                        "ur" -> "منسلک معصومینؑ کا تفصیلی تعارف:"
                                        "ar" -> "تفاصيل المعصومين المرتبطين بهذا الموقع:"
                                        else -> "Linked Infallibles — dates, burial & biography:"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    masoomeenForLoc.forEach { masoom ->
                                        MasoomZiyaratCard(
                                            masoom = masoom,
                                            expanded = localizedMasoomeen[masoom.index],
                                            languageCode = languageCode
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (languageCode) {
                                            "ur" -> "سفری رہنمائی اور آداب:"
                                            "ar" -> "إرشادات السفر والآداب الشرعية:"
                                            else -> "Travel Tips & Pilgrimage Etiquette:"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = when (languageCode) {
                                        "ur" -> currentSelectedLoc.tipsUr
                                        "ar" -> currentSelectedLoc.tipsAr
                                        else -> currentSelectedLoc.tipsEn
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Lat: ${currentSelectedLoc.lat}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lon: ${currentSelectedLoc.lon}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = when (languageCode) {
                                    "ur" -> "نقشے کے نشانات رہنمائی کے لیے ہیں؛ سفر سے قبل موجودہ داخلے اور حفاظتی ہدایات مقامی ذرائع سے چیک کریں۔"
                                    "ar" -> "مؤشرات الخريطة للإرشاد؛ تحقق محلياً من تعليمات الدخول والسلامة الحالية قبل السفر."
                                    else -> "Map pins are for guidance; confirm current access and safety instructions locally before travel."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        1 -> {
                            // TAB 2: INTERACTIVE COMPASS DIRECTION
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shrine_compass_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        androidx.compose.runtime.CompositionLocalProvider(
                                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val primaryColor = MaterialTheme.colorScheme.primary
                                                val errorColor = MaterialTheme.colorScheme.error
                                                
                                                Canvas(modifier = Modifier.size(70.dp)) {
                                                    val center = this.center
                                                    val radius = size.minDimension / 2f
                                                    
                                                    rotate(degrees = bearing.toFloat(), pivot = center) {
                                                        val needleLength = radius * 0.85f
                                                        val needleWidth = 5.dp.toPx()
                                                        
                                                        val pathNorth = Path().apply {
                                                            moveTo(center.x, center.y - needleLength)
                                                            lineTo(center.x - needleWidth, center.y)
                                                            lineTo(center.x + needleWidth, center.y)
                                                            close()
                                                        }
                                                        drawPath(path = pathNorth, color = errorColor)
                                                        
                                                        val pathSouth = Path().apply {
                                                            moveTo(center.x, center.y + needleLength)
                                                            lineTo(center.x - needleWidth, center.y)
                                                            lineTo(center.x + needleWidth, center.y)
                                                            close()
                                                        }
                                                        drawPath(path = pathSouth, color = primaryColor.copy(alpha = 0.3f))
                                                        
                                                        drawCircle(color = Color.White, radius = 3.5.dp.toPx())
                                                        drawCircle(color = errorColor, radius = 2.dp.toPx())
                                                    }
                                                }

                                                Text(
                                                    text = "N",
                                                    color = errorColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 1.dp)
                                                )
                                                Text(
                                                    text = "S",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp)
                                                )
                                                Text(
                                                    text = "W",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)
                                                )
                                                Text(
                                                    text = "E",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (languageCode == "ur") "🧭 مزار کا قبلہ رخ & سمت" else if (languageCode == "ar") "🧭 اتّجاه المرقد الشّريف" else "🧭 Shrine Compass & Angle",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (languageCode == "ur") {
                                                    "سرخ سوئی کا رخ مزارِ اقدس کی طرف ہے"
                                                } else if (languageCode == "ar") {
                                                    "تشير الإبْرة الحمراء مباشرة إلى موضع الروضة الشريفة للتسليم"
                                                } else {
                                                    "The Red pointer indicates direct line of sight to the Shrine"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = if (languageCode == "ur") {
                                                    "اس سمت رخ کر کے سلام و تصوراتی حاضری دیں"
                                                } else if (languageCode == "ar") {
                                                    "تحديد اتّجاه المشهد للتّسليم والزّيارة القلبية الطّاهرة"
                                                } else {
                                                    "Face this bearing to offer virtual Salam to the shrine"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (languageCode == "ur") "سچی سمت (Bearing)" else if (languageCode == "ar") "زاوية الاتّجاه" else "Exact Bearing",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", bearing)}°",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (languageCode == "ur") "رخ (Cardinal)" else if (languageCode == "ar") "جهة الرّيح" else "Cardinal Direction",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = cardinalDir,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (languageCode == "ur") "حسابی فاصلہ" else if (languageCode == "ar") "المسافة" else "Distance",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (languageCode == "ur") {
                                                    "${String.format("%,d", distanceKm.toInt())} کلومیٹر"
                                                } else if (languageCode == "ar") {
                                                    "${String.format("%,d", distanceKm.toInt())} كم"
                                                } else {
                                                    "${String.format("%,d", distanceKm.toInt())} km"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (trackingSource == "GPS Active") {
                                                if (languageCode == "ur") "مقام بذریعہ: موبائل GPS" else if (languageCode == "ar") "المصدر: نظام GPS" else "Source: Device GPS"
                                            } else {
                                                if (languageCode == "ur") "مقام بذریعہ: منتخب شہر" else if (languageCode == "ar") "المصدر: مدينة الأذان" else "Source: Selected Home City"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        2 -> {
                            // TAB 3: SAFAR NAMA / JOURNAL
                            val currentVisitRecord = visitRecords.find { it.siteId == currentSelectedLoc.id }
                            val currentVirtuallyVisited = currentVisitRecord?.virtuallyVisited ?: false
                            val currentPhysicallyVisited = currentVisitRecord?.physicallyVisited ?: false

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ziyarat_checklist_card_${currentSelectedLoc.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (languageCode == "ur") "🎯 حاضری ریکارڈ" else if (languageCode == "ar") "🎯 مفكرة زيارة المرقد" else "🎯 Ziyarat Tracker",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = if (languageCode == "ur") "کیا آپ نے اس مقام کی حاضری دی ہے؟" else if (languageCode == "ar") "علّم حالتك لزيارة هذا المرقد" else "Tap below to update your visit status",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Virtual checklist chip
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (currentVirtuallyVisited) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (currentVirtuallyVisited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.toggleZiyaratVisit(currentSelectedLoc.id, "virtual")
                                                }
                                                .testTag("btn_virt_${currentSelectedLoc.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = currentVirtuallyVisited,
                                                    onCheckedChange = {
                                                        viewModel.toggleZiyaratVisit(currentSelectedLoc.id, "virtual")
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.primary,
                                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    modifier = Modifier.size(24.dp).testTag("box_virt_${currentSelectedLoc.id}")
                                                )
                                                Text(
                                                    text = if (languageCode == "ur") "تصوراتی" else if (languageCode == "ar") "افتراضية" else "Virtual",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentVirtuallyVisited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Physical checklist chip
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (currentPhysicallyVisited) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (currentPhysicallyVisited) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.toggleZiyaratVisit(currentSelectedLoc.id, "physical")
                                                }
                                                .testTag("btn_phys_${currentSelectedLoc.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = currentPhysicallyVisited,
                                                    onCheckedChange = {
                                                        viewModel.toggleZiyaratVisit(currentSelectedLoc.id, "physical")
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.secondary,
                                                        checkmarkColor = MaterialTheme.colorScheme.onSecondary
                                                    ),
                                                    modifier = Modifier.size(24.dp).testTag("box_phys_${currentSelectedLoc.id}")
                                                )
                                                Text(
                                                    text = if (languageCode == "ur") "عملی" else if (languageCode == "ar") "ميدانية" else "Physical",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentPhysicallyVisited) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // SAFARNAMA & NIJI YADDASHTAIN (PILGRIM DIARY & JOURNAL NOTES)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ziyarat_journal_card_${currentSelectedLoc.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (languageCode == "ur") "📝 میرا سفرنامہ اور نجی یادداشتیں" else if (languageCode == "ar") "📝 سجلّ مذكرات الزيارة والعهد" else "📝 My Safarnama & Notes",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    Text(
                                        text = if (languageCode == "ur") {
                                            "اپنی حاضری کی تاریخ، نمازِ زیارات، یادگار لمحات یا دوستوں کی طرف سے دی گئی التماسِ دعا یہاں مستقل محفوظ کریں۔"
                                        } else if (languageCode == "ar") {
                                            "دوّن تاريخ زيارتك، صلواتك المستجابة، والتماسات الدعاء من الأهل والأصحاب للذكرى والعهد."
                                        } else {
                                            "Save your journey logs, prayers offered, spiritual emotions, or requests for prayers (Dua lists) permanently."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isEditingJournal) {
                                        // 1. Visit Date Field
                                        OutlinedTextField(
                                            value = visitDateText,
                                            onValueChange = { visitDateText = it },
                                            label = {
                                                Text(
                                                    text = if (languageCode == "ur") "سفر کی تاریخ / عیسوی یا ہجری" else if (languageCode == "ar") "تاريخ الزيارة" else "Date of Visit (Gregorian/Hijri)",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            placeholder = {
                                                Text(
                                                    text = if (languageCode == "ur") "مثلاً: 20 صفر 1447" else "e.g., 20 Safar 1447 / Oct 2026",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("field_journal_date"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        )

                                        // 2. Personal feelings & notes Field
                                        OutlinedTextField(
                                            value = journalNotesText,
                                            onValueChange = { journalNotesText = it },
                                            label = {
                                                Text(
                                                    text = if (languageCode == "ur") "احساسات اور یادداشتیں (Safarnama)" else if (languageCode == "ar") "الخواطر ومذكرات الزيارة" else "Journal Notes & Feelings",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            placeholder = {
                                                Text(
                                                    text = if (languageCode == "ur") "یہاں اپنے قلبی احساسات اور زیارات کے دوران بیتے لمحات لکھیں..." else "Describe your spiritual experiences & memories here...",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            minLines = 2,
                                            maxLines = 5,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("field_journal_notes"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        )

                                        // 3. Dua requests Field
                                        OutlinedTextField(
                                            value = prayRequestsText,
                                            onValueChange = { prayRequestsText = it },
                                            label = {
                                                Text(
                                                    text = if (languageCode == "ur") "التماسِ دعا کی فہرست (Dua Requests)" else if (languageCode == "ar") "لائحة التماسات الدعاء والزيابة نيابة" else "Dua Requests List",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            placeholder = {
                                                Text(
                                                    text = if (languageCode == "ur") "ان دوستوں اور رشتہ داروں کے نام لکھیں جنہوں نے دعا کرنے کا کہا..." else "Family and friends who requested prayers or Ziyarat-by-Proxy...",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            minLines = 1,
                                            maxLines = 3,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("field_journal_duas"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        )

                                        // Save Action button
                                        Button(
                                            onClick = {
                                                viewModel.saveZiyaratJournal(
                                                    siteId = currentSelectedLoc.id,
                                                    visitDate = visitDateText,
                                                    notes = journalNotesText,
                                                    duaRequests = prayRequestsText
                                                )
                                                isEditingJournal = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .testTag("btn_save_journal")
                                        ) {
                                            Text(
                                                text = if (languageCode == "ur") "یادداشت محفوظ کریں (Save)" else if (languageCode == "ar") "حفظ في المذكرات" else "Save Journal Entry",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    } else {
                                        // Read-Only Display of Saved Journal
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                if (visitDateText.isNotBlank()) {
                                                    Text(
                                                        text = if (languageCode == "ur") "تاریخ: $visitDateText" else "Date: $visitDateText",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                }
                                                if (journalNotesText.isNotBlank()) {
                                                    Text(
                                                        text = journalNotesText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                }
                                                if (prayRequestsText.isNotBlank()) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Text(
                                                                text = if (languageCode == "ur") "التماسِ دعا:" else "Dua Requests:",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.tertiary,
                                                                modifier = Modifier.padding(bottom = 4.dp)
                                                            )
                                                            Text(
                                                                text = prayRequestsText,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                OutlinedButton(
                                                    onClick = { isEditingJournal = true },
                                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        text = if (languageCode == "ur") "یادداشت میں ترمیم کریں" else "Edit Journal Entry",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasoomZiyaratCard(
    masoom: MasoomDetails,
    expanded: ExpandedMasoomData?,
    languageCode: String
) {
    val isLivingImam = masoom.index == 14
    val displayName = expanded?.name
        ?: LocalizationUtility.translateMasoomName(masoom.name, languageCode)
    val displayTitle = expanded?.title ?: masoom.title
    val birth = expanded?.born ?: translateIslamicDate(masoom.birthDate, languageCode)
    val status = expanded?.martyrdom ?: translateIslamicDate(masoom.martyrdomDate, languageCode)
    val burial = expanded?.burial ?: masoom.burialPlace
    val biography = expanded?.biography ?: masoom.bioSummary

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("masoom_ziyarat_details_${masoom.index}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            if (displayTitle.isNotBlank()) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            MasoomInfoLine(
                icon = Icons.Default.AutoAwesome,
                label = when (languageCode) {
                    "ur" -> "ولادت"
                    "ar" -> "الولادة"
                    else -> "Birth"
                },
                value = birth,
                tint = Color(0xFF2E7D32)
            )
            MasoomInfoLine(
                icon = if (isLivingImam) Icons.Default.VisibilityOff else Icons.Default.NightsStay,
                label = when {
                    isLivingImam && languageCode == "ur" -> "حیات و غیبت"
                    isLivingImam && languageCode == "ar" -> "الحياة والغيبة"
                    isLivingImam -> "Life & Occultation"
                    languageCode == "ur" -> "شہادت"
                    languageCode == "ar" -> "الشهادة"
                    else -> "Martyrdom"
                },
                value = status,
                tint = if (isLivingImam) MaterialTheme.colorScheme.primary else Color(0xFF455A64)
            )
            if (!isLivingImam && burial.isNotBlank()) {
                MasoomInfoLine(
                    icon = Icons.Default.Place,
                    label = when (languageCode) {
                        "ur" -> "مدفن"
                        "ar" -> "المدفن"
                        else -> "Burial"
                    },
                    value = burial,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = when (languageCode) {
                    "ur" -> "مختصر سوانح"
                    "ar" -> "نبذة من السيرة"
                    else -> "Brief biography"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = biography,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun MasoomInfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// Great-circle distance calculation
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val theta = lon1 - lon2
    var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.cos(Math.toRadians(theta))
    dist = Math.acos(dist)
    dist = Math.toDegrees(dist)
    dist = dist * 60 * 1.1515 * 1.609344 // in kilometers
    if (dist.isNaN()) return 0.0
    return dist
}

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)

    val y = Math.sin(deltaLambda) * Math.cos(phi2)
    val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
    var bearing = Math.toDegrees(Math.atan2(y, x))
    return (bearing + 360) % 360
}

fun getCardinalDirection(bearing: Double, languageCode: String): String {
    val index = (((bearing + 22.5) % 360) / 45).toInt()
    return when (languageCode) {
        "ur" -> when (index) {
            0 -> "شمال (North)"
            1 -> "شمال مشرق (North-East)"
            2 -> "مشرق (East)"
            3 -> "جنوب مشرق (South-East)"
            4 -> "جنوب (South)"
            5 -> "جنوب مغرب (South-West)"
            6 -> "مغرب (West)"
            7 -> "شمال مغرب (North-West)"
            else -> "شمال (North)"
        }
        "ar" -> when (index) {
            0 -> "شمال (North)"
            1 -> "شمال شرق (North-East)"
            2 -> "شرق (East)"
            3 -> "جنوب شرق (South-East)"
            4 -> "جنوب (South)"
            5 -> "جنوب غرب (South-West)"
            6 -> "غرب (West)"
            7 -> "شمال غرب (North-West)"
            else -> "شمال (North)"
        }
        else -> when (index) {
            0 -> "North"
            1 -> "North-East"
            2 -> "East"
            3 -> "South-East"
            4 -> "South"
            5 -> "South-West"
            6 -> "West"
            7 -> "North-West"
            else -> "North"
        }
    }
}

fun translateIslamicDate(dateEn: String, languageCode: String): String {
    if (languageCode == "en") return dateEn
    
    var translated = dateEn
    val monthMap = mapOf(
        "Muharram" to (if (languageCode == "ur") "محرم الحرام" else "محرم الحرام"),
        "Safar" to (if (languageCode == "ur") "صفر المظفر" else "صفر المظفر"),
        "Rabi' al-Awwal" to (if (languageCode == "ur") "ربیع الاول" else "ربيع الأول"),
        "Rabi' al-Thani" to (if (languageCode == "ur") "ربیع الثانی" else "ربيع الثاني"),
        "Jumada al-Awwal" to (if (languageCode == "ur") "جمادی الاول" else "جمادى الأولى"),
        "Jumada al-Thani" to (if (languageCode == "ur") "جمادی الثانی" else "جمادى الآخرة"),
        "Rajab" to (if (languageCode == "ur") "رجب المرجب" else "رجب المرجب"),
        "Sha'ban" to (if (languageCode == "ur") "شعبان المعظم" else "شعبان المعظم"),
        "Ramadan" to (if (languageCode == "ur") "رمضان المبارک" else "رمضان المبارك"),
        "Shawwal" to (if (languageCode == "ur") "شوال المکرم" else "شوال المكرم"),
        "Dhu al-Qa'dah" to (if (languageCode == "ur") "ذیقعدہ" else "ذو القعدة"),
        "Dhu al-Hijjah" to (if (languageCode == "ur") "ذی الحجہ" else "ذو الحجة"),
        "Last" to (if (languageCode == "ur") "آخری" else "أواخر")
    )
    
    for ((en, loc) in monthMap) {
        translated = translated.replace(en, loc, ignoreCase = true)
    }
    
    val engDigits = "0123456789"
    val arDigits = "٠١٢٣٤٥٦٧٨٩"
    val urDigits = "۰۱۲۳۴۵۶۷۸۹"
    
    val targetDigits = if (languageCode == "ur") urDigits else arDigits
    val builder = StringBuilder()
    for (char in translated) {
        val index = engDigits.indexOf(char)
        if (index != -1) {
            builder.append(targetDigits[index])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}

