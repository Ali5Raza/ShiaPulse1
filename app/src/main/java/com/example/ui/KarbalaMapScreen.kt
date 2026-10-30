package com.example.ui

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.KarbalaAssetLoader
import com.example.data.KarbalaFullData
import com.example.data.KnowledgeReference
import com.example.data.KnowledgeReferenceType
import kotlinx.coroutines.launch
import org.json.JSONObject

private fun karbalaText(key: String, languageCode: String): String {
    val values = when (languageCode) {
        "ur" -> mapOf(
            "title" to "سفرِ کربلا",
            "back" to "واپس",
            "timeline" to "سفرنامہ و نقشہ",
            "companions" to "شہداء و اصحاب",
            "family" to "شجرہ نسب",
            "arbaeen" to "مشایۃ اربعین",
            "ziyarat" to "زیارات",
            "places" to "تاریخی مقامات",
            "journey_title" to "ترتیب کے ساتھ سفر",
            "journey_notice" to "نمبر دار نشانات واقعات کی ترتیب دکھاتے ہیں۔ سیدھے منقطع خطوط محض خاکہ ہیں، حقیقی راستوں یا فاصلے کا دعویٰ نہیں۔ نقشہ کے ٹائلز دستیاب نہ ہوں تو مآخذ کے ساتھ نیچے مکمل سفرنامہ موجود رہے گا۔",
            "historical_source" to "تاریخی ماخذ",
            "prominent_companions" to "منتخب اصحاب و شہداء",
            "companions_notice" to "یہ 72 شہداء کی مکمل فہرست نہیں، صرف چند نمایندہ شخصیات کا تعارف ہے۔"
        )
        "ar" -> mapOf(
            "title" to "رحلة كربلاء",
            "back" to "رجوع",
            "timeline" to "الرحلة والخريطة",
            "companions" to "الشهداء والأصحاب",
            "family" to "شجرة العائلة",
            "arbaeen" to "مشي الأربعين",
            "ziyarat" to "الزيارات",
            "places" to "الأماكن التاريخية",
            "journey_title" to "الرحلة بالترتيب",
            "journey_notice" to "تبيّن العلامات المرقّمة ترتيب الأحداث. الخطوط المتقطعة المستقيمة توضيحية وليست ادعاءً لمسارات أو مسافات دقيقة. ويبقى سرد الرحلة ومصادره متاحاً أدناه عند تعذر تحميل الخريطة.",
            "historical_source" to "المصدر التاريخي",
            "prominent_companions" to "مختارات من الأصحاب والشهداء",
            "companions_notice" to "هذه ليست قائمة كاملة بشهداء كربلاء الاثنين والسبعين، بل تعريف بشخصيات مختارة."
        )
        else -> mapOf(
            "title" to "Karbala Journey",
            "back" to "Back",
            "timeline" to "Timeline & Map",
            "companions" to "Shuhada & Companions",
            "family" to "Family Tree",
            "arbaeen" to "Arbaeen Walk",
            "ziyarat" to "Ziyarat Guide",
            "places" to "Historical Places",
            "journey_title" to "The journey, in order",
            "journey_notice" to "Numbered markers show event order. Straight dashed connectors are schematic, not a claim about exact roads or distances. The sourced journey below remains available if map tiles cannot load.",
            "historical_source" to "Historical source",
            "prominent_companions" to "Selected companions and martyrs",
            "companions_notice" to "This is not a complete list of the 72 martyrs; it introduces selected figures."
        )
    }
    return values[key] ?: values["title"] ?: key
}

class KarbalaWebInterface(
    private val onMarkerClick: (String) -> Unit
) {
    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val json = JSONObject(message)
            if (json.has("action") && json.getString("action") == "markerClick") {
                onMarkerClick(json.getString("id"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbalaMapScreen(currentLang: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val karbalaData = remember { KarbalaAssetLoader.loadKarbalaData(context) }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        karbalaText("timeline", currentLang),
        karbalaText("companions", currentLang),
        karbalaText("family", currentLang),
        karbalaText("arbaeen", currentLang),
        karbalaText("ziyarat", currentLang),
        karbalaText("places", currentLang)
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = karbalaText("title", currentLang),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = karbalaText("back", currentLang),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Crossfade(targetState = selectedTabIndex, label = "KarbalaTabs") { tabIndex ->
                when (tabIndex) {
                    0 -> TimelineAndMapTab(karbalaData, currentLang)
                    1 -> CompanionsTab(karbalaData, currentLang)
                    2 -> FamilyTreeTab(karbalaData, currentLang)
                    3 -> ArbaeenTab(karbalaData, currentLang)
                    4 -> ZiyaratTab(karbalaData, currentLang)
                    5 -> PlacesTab(karbalaData, currentLang)
                }
            }
        }
    }
}

@Composable
fun TimelineAndMapTab(data: KarbalaFullData, currentLang: String) {
    var selectedEventId by remember(data.timeline) {
        mutableStateOf(data.timeline.firstOrNull()?.id)
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val leafHtml = """
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
                width: 100%;
                margin: 0;
                padding: 0;
            }
            .custom-icon {
                background: none;
                border: none;
            }
            .pin-circle {
                width: 28px;
                height: 28px;
                background-color: #B34700;
                border-radius: 50%;
                border: 3px solid white;
                box-shadow: 0 0 5px rgba(0,0,0,0.5);
                color: white;
                display: flex;
                align-items: center;
                justify-content: center;
                font: bold 13px sans-serif;
            }
            .pin-circle.selected {
                border-color: #FFEA00;
                transform: scale(1.25);
                transition: 0.2s;
            }
            .direction-icon {
                background: none;
                border: none;
                color: #6D28A8;
                font: bold 22px sans-serif;
                text-shadow: 0 1px 2px white;
                pointer-events: none;
            }
        </style>
    </head>
    <body style="margin: 0;">
        <div id="map"></div>
        <script>
            var map = L.map('map', {zoomControl: false}).setView([32.6160, 44.0249], 6);
            
            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                attribution: '&copy; OpenStreetMap contributors &copy; CARTO'
            }).addTo(map);

            var markers = {};
            var latlngs = [];
            var routeLayers = [];

            function initMarkers(dataStr) {
                var data = JSON.parse(dataStr);
                routeLayers.forEach(function(layer) { map.removeLayer(layer); });
                routeLayers = [];
                markers = {};
                latlngs = [];

                data.forEach(function(ev, index) {
                    var customIcon = L.divIcon({
                        className: 'custom-icon',
                        html: '<div id="pin-'+ev.id+'" class="pin-circle">'+(index + 1)+'</div>',
                        iconSize: [28, 28],
                        iconAnchor: [14, 14]
                    });
                    
                    var marker = L.marker([ev.lat, ev.lon], {icon: customIcon})
                        .addTo(map)
                        .bindTooltip((index + 1)+'. '+ev.id.replace(/_/g, ' '))
                        .on('click', function(e) {
                            selectMarker(ev.id);
                            if (window.AndroidBridge) {
                                window.AndroidBridge.postMessage(JSON.stringify({action: 'markerClick', id: ev.id}));
                            }
                        });
                    markers[ev.id] = marker;
                    routeLayers.push(marker);
                    latlngs.push([ev.lat, ev.lon]);
                });

                if (latlngs.length > 0) {
                    var polyline = L.polyline(latlngs, {
                        color: '#6D28A8',
                        weight: 4,
                        dashArray: '6, 10'
                    }).addTo(map);
                    routeLayers.push(polyline);
                    addDirectionArrows(latlngs);
                    map.fitBounds(polyline.getBounds(), {padding: [30, 30]});
                }
            }

            function addDirectionArrows(points) {
                for (var i = 0; i < points.length - 1; i++) {
                    var start = L.latLng(points[i]);
                    var end = L.latLng(points[i + 1]);
                    if (start.distanceTo(end) < 5000) continue;
                    var midpoint = L.latLng(
                        (start.lat + end.lat) / 2,
                        (start.lng + end.lng) / 2
                    );
                    var angle = Math.atan2(end.lat - start.lat, end.lng - start.lng) * 180 / Math.PI;
                    var arrow = L.marker(midpoint, {
                        interactive: false,
                        keyboard: false,
                        icon: L.divIcon({
                            className: 'direction-icon',
                            html: '<div style="transform: rotate('+angle+'deg)">➜</div>',
                            iconSize: [24, 24],
                            iconAnchor: [12, 12]
                        })
                    }).addTo(map);
                    routeLayers.push(arrow);
                }
            }

            function selectMarker(id) {
                for (var key in markers) {
                    var el = document.getElementById('pin-'+key);
                    if (el) el.classList.remove('selected');
                }
                var selectedEl = document.getElementById('pin-'+id);
                if (selectedEl) selectedEl.classList.add('selected');
                
                if (markers[id]) {
                    map.flyTo(markers[id].getLatLng(), 8, {duration: 1.5});
                }
            }
        </script>
    </body>
    </html>
    """.trimIndent()

    Column(modifier = Modifier.fillMaxSize()) {
        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.destroy()
                webViewRef = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f)
                .background(Color.Gray)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        
                        addJavascriptInterface(KarbalaWebInterface(
                            onMarkerClick = { id ->
                                scope.launch {
                                    selectedEventId = id
                                    val index = data.timeline.indexOfFirst { it.id == id }
                                    if (index != -1) {
                                        listState.animateScrollToItem(index + 1)
                                    }
                                }
                            }
                        ), "AndroidBridge")
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val jsonArray = org.json.JSONArray()
                                data.timeline.forEach { ev ->
                                    val obj = org.json.JSONObject()
                                    obj.put("id", ev.id)
                                    obj.put("lat", ev.lat)
                                    obj.put("lon", ev.lon)
                                    jsonArray.put(obj)
                                }
                                val markerScript = "initMarkers(${JSONObject.quote(jsonArray.toString())});"
                                val selectionScript = selectedEventId?.let { id ->
                                    "selectMarker(${JSONObject.quote(id)});"
                                }.orEmpty()
                                view?.evaluateJavascript(markerScript + selectionScript, null)
                            }
                        }
                        
                        loadDataWithBaseURL("https://leaflet", leafHtml, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.60f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = karbalaText("journey_title", currentLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = karbalaText("journey_notice", currentLang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    CompactReferenceLink(
                        url = "https://en.wikishia.net/view/Uprising_of_Imam_al-Husayn_(a)",
                        languageCode = currentLang,
                        label = karbalaText("historical_source", currentLang)
                    )
                }
            }

            itemsIndexed(data.timeline) { index, event ->
                val isSelected = event.id == selectedEventId
                val titleText = event.localizedTitle(currentLang)
                val descText = event.localizedDescription(currentLang)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .testTag("karbala_journey_stop_${event.id}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(30.dp).fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (index < data.timeline.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                selectedEventId = event.id
                                webViewRef?.evaluateJavascript("selectMarker(${JSONObject.quote(event.id)});", null)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = event.localizedDate(currentLang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = descText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (event.sourceLabel.isNotBlank()) {
                                KnowledgeReferenceCard(
                                    reference = KnowledgeReference(
                                        type = KnowledgeReferenceType.HISTORY,
                                        citation = event.sourceLabel,
                                        url = event.sourceUrl
                                    ),
                                    languageCode = currentLang
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanionsTab(data: KarbalaFullData, currentLang: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = karbalaText("prominent_companions", currentLang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = karbalaText("companions_notice", currentLang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(data.companions) { comp ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = comp.localizedName(currentLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = comp.localizedRole(currentLang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = comp.localizedDescription(currentLang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CompactReferenceLink(
                        url = comp.sourceUrl,
                        languageCode = currentLang,
                        label = karbalaText("historical_source", currentLang)
                    )
                }
            }
        }
    }
}

@Composable
fun FamilyTreeTab(data: KarbalaFullData, currentLang: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(data.familyTree) { member ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = member.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = member.relationEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArbaeenTab(data: KarbalaFullData, currentLang: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(data.arbaeenRoute) { route ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (currentLang == "ur") route.nameUr else route.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = route.descEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZiyaratTab(data: KarbalaFullData, currentLang: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(data.ziyaratGuide) { ziyarat ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (currentLang == "ur") ziyarat.titleUr else ziyarat.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ziyarat.descEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlacesTab(data: KarbalaFullData, currentLang: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(data.historicalPlaces) { place ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (currentLang == "ur") place.nameUr else place.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = place.descEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
