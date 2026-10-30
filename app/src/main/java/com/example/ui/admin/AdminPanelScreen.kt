package com.example.ui.admin

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

enum class ContentType(val title: String, val versionKey: String, val dataKey: String, val backupNode: String) {
    DUAS("Duas", "version", "duas_json_string", "duas"),
    HADITH("Hadith (Quotes)", "hadith_version", "hadith_json_string", "hadith"),
    AYAT("Quran Ayat", "ayat_version", "ayat_json_string", "ayat"),
    AAMAL("Aamal", "aamal_version", "aamal_json_string", "aamal"),
    EVENTS("Islamic Events", "events_version", "events_json_string", "events")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Panel Login") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = false },
                    label = { Text("Admin Password") },
                    isError = passwordError,
                    singleLine = true
                )
                if (passwordError) {
                    Text("Incorrect password", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (password == "admin123") { // In a real app, this should be more secure
                        isAuthenticated = true
                    } else {
                        passwordError = true
                    }
                }) {
                    Text("Login")
                }
            }
        }
        return
    }

    AdminDashboardScreen(onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Islamic Events JSON", "Content Sync", "Remote Config")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> IslamicEventsAdminView()
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(ContentType.values()) { type ->
                            ContentAdminCard(type)
                        }
                    }
                }
                else -> RemoteConfigAdminView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteConfigAdminView() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val remoteConfigService = remember { com.example.utils.DefaultRemoteConfigurationService(context) }
    
    var isLoading by remember { mutableStateOf(true) }
    var config by remember { mutableStateOf<com.example.utils.AppRemoteConfig?>(null) }
    
    var defaultOffset by remember { mutableStateOf(0) }
    var countriesMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    var prayerCalcMethod by remember { mutableStateOf("MWL") }
    var maintenanceMode by remember { mutableStateOf(false) }
    var announcements by remember { mutableStateOf<List<String>>(emptyList()) }
    var featureFlags by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    
    var isSaving by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    
    var showCountryDialog by remember { mutableStateOf(false) }
    var dialogCountryCode by remember { mutableStateOf("") }
    var dialogOffset by remember { mutableStateOf(0) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var isEditingCode by remember { mutableStateOf(false) }

    fun loadConfig() {
        isLoading = true
        scope.launch {
            val remote = remoteConfigService.fetchRemoteConfig()
            config = remote
            defaultOffset = remote.hijri_config.defaultOffset
            countriesMap = remote.hijri_config.countries
            
            prayerCalcMethod = remote.prayer_calc_method
            maintenanceMode = remote.maintenance_mode
            announcements = remote.announcements
            featureFlags = remote.feature_flags
            
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadConfig()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hijri Default Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Default Offset", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Applied if no country offset is found (-2 to +2)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (defaultOffset > -2) defaultOffset-- },
                                enabled = defaultOffset > -2
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = if (defaultOffset >= 0) "+$defaultOffset" else "$defaultOffset",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { if (defaultOffset < 2) defaultOffset++ },
                                enabled = defaultOffset < 2
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Country Specific Offsets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                dialogCountryCode = ""
                                dialogOffset = 0
                                dialogError = null
                                isEditingCode = false
                                showCountryDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Country", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (countriesMap.isEmpty()) {
                        Text(
                            text = "No country overrides set. Default offset will apply everywhere.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            countriesMap.forEach { (countryCode, offset) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = countryCode,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Offset: ${if (offset >= 0) "+$offset" else "$offset"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            dialogCountryCode = countryCode
                                            dialogOffset = offset
                                            dialogError = null
                                            isEditingCode = true
                                            showCountryDialog = true
                                        }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Offset",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            countriesMap = countriesMap - countryCode
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Offset",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Configuration & Feature Flags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage system behaviors and flags dynamically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Maintenance Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Bridges app with offline placeholder screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = maintenanceMode,
                            onCheckedChange = { maintenanceMode = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Prayer Calculation Method", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Current default method for new users", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { /* Future method selection */ }) {
                            Text(prayerCalcMethod)
                        }
                    }
                }
            }
        }

        item {
            Column {
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Publishing configurations to Firebase Realtime DB...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                val currentSaveStatus = saveStatus
                if (currentSaveStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentSaveStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentSaveStatus.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            saveStatus = "Validating entries..."
                            
                            val invalidCountries = countriesMap.filter { (code, offset) ->
                                code.length != 2 || !code.all { it.isLetter() } || offset < -2 || offset > 2
                            }
                            if (invalidCountries.isNotEmpty()) {
                                saveStatus = "Error: Invalid country formats found."
                                isSaving = false
                                return@launch
                            }
                            
                            val updatedConfig = com.example.utils.AppRemoteConfig(
                                hijri_config = com.example.utils.HijriConfig(
                                    defaultOffset = defaultOffset,
                                    countries = countriesMap,
                                    lastUpdated = ""
                                ),
                                prayer_calc_method = prayerCalcMethod,
                                maintenance_mode = maintenanceMode,
                                announcements = announcements,
                                feature_flags = featureFlags
                            )
                            
                            val success = remoteConfigService.saveRemoteConfig(updatedConfig)
                            if (success) {
                                saveStatus = "Success! Remote configuration updated successfully."
                                try {
                                    val countryService = com.example.utils.DefaultCountryDetectionService(context)
                                    val hijriConfigService = com.example.utils.DefaultHijriConfigurationService(countryService, remoteConfigService)
                                    hijriConfigService.resolveAndApplyOffset(context)
                                } catch (e: Exception) {
                                    android.util.Log.e("AdminPanel", "Error re-resolving locally", e)
                                }
                            } else {
                                saveStatus = "Error: Failed to upload configuration to Firebase."
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish Configuration")
                }
            }
        }
    }

    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text(if (isEditingCode) "Edit Country Offset" else "Add Country Offset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dialogCountryCode,
                        onValueChange = {
                            if (it.length <= 2) {
                                dialogCountryCode = it.uppercase(java.util.Locale.US)
                                dialogError = null
                            }
                        },
                        label = { Text("Country Code (ISO Alpha-2)") },
                        placeholder = { Text("e.g. PK, SA, IN") },
                        enabled = !isEditingCode,
                        singleLine = true,
                        isError = dialogError != null
                    )
                    
                    val currentDialogError = dialogError
                    if (currentDialogError != null) {
                        Text(
                            text = currentDialogError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Text("Select Offset (-2 to +2):", style = MaterialTheme.typography.labelLarge)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (offsetVal in -2..2) {
                            FilterChip(
                                selected = dialogOffset == offsetVal,
                                onClick = { dialogOffset = offsetVal },
                                label = { Text(if (offsetVal >= 0) "+$offsetVal" else "$offsetVal") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val code = dialogCountryCode.trim().uppercase(java.util.Locale.US)
                    if (code.length != 2 || !code.all { it.isLetter() }) {
                        dialogError = "Invalid ISO country code. Must be exactly 2 letters."
                        return@Button
                    }
                    if (!isEditingCode && countriesMap.containsKey(code)) {
                        dialogError = "Country code already exists."
                        return@Button
                    }
                    
                    countriesMap = countriesMap + (code to dialogOffset)
                    showCountryDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentAdminCard(type: ContentType) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentVersion by remember { mutableStateOf<Long?>(null) }
    var lastUpdated by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var parsedJsonString by remember { mutableStateOf<String?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValid by remember { mutableStateOf(false) }
    var itemCount by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("Duas") }
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance("https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app")

    fun loadData() {
        isLoading = true
        database.reference.child(type.versionKey).get().addOnSuccessListener { vSnap ->
            currentVersion = vSnap.getValue(Long::class.java) ?: 0L
            database.reference.child("${type.versionKey}_last_updated").get().addOnSuccessListener { tSnap ->
                lastUpdated = tSnap.getValue(String::class.java) ?: "Never"
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(type) {
        loadData()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedUri = uri
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonString = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (jsonString != null) {
                    parsedJsonString = jsonString
                    // Validate JSON
                    try {
                        val array = JSONArray(jsonString)
                        itemCount = array.length()
                        if (itemCount > 0) {
                            isValid = true
                            validationMessage = "Valid JSON array with $itemCount items."
                        } else {
                            isValid = false
                            validationMessage = "JSON array is empty."
                        }
                    } catch (e: Exception) {
                        try {
                            val obj = org.json.JSONObject(jsonString)
                            itemCount = 1
                            isValid = true
                            validationMessage = "Valid single JSON object."
                        } catch (e2: Exception) {
                            isValid = false
                            validationMessage = "Invalid JSON format: Not an array or object."
                        }
                    }
                }
            } catch (e: Exception) {
                isValid = false
                validationMessage = "Error reading file: ${e.message}"
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(type.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Current Version: ${currentVersion ?: 0}")
                Text("Last Updated: ${lastUpdated ?: "Unknown"}")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launcher.launch("application/json") }) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select JSON")
                }
                OutlinedButton(onClick = { showRestoreDialog = true }) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            }

            val currentValidationMessage = validationMessage
            if (currentValidationMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentValidationMessage,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (type == ContentType.DUAS) {
                Spacer(modifier = Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Target Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Duas", "Munajats", "Ziyarats", "Amaal", "Prayers").forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isValid && !isUploading) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showConfirmDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload & Publish")
                }
            }

            if (isUploading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Uploading...", style = MaterialTheme.typography.bodySmall)
            }

            val currentUploadStatus = uploadStatus
            if (currentUploadStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentUploadStatus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Upload") },
            text = { 
                if (itemCount == 1 && type == ContentType.DUAS) {
                    Text("You are about to append this ${type.title} to the '$selectedCategory' folder. This will create a backup, increment the version, and publish to all users. Continue?")
                } else {
                    Text("You are about to replace the current ${type.title} data. This will create a backup, increment the version, and publish to all users. Continue?") 
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    isUploading = true
                    uploadStatus = "Creating backup..."
                    scope.launch {
                        val confirmedJson = parsedJsonString
                        if (confirmedJson == null) {
                            isUploading = false
                            uploadStatus = "No validated JSON to upload."
                            return@launch
                        }
                        try {
                            val rootRef = database.reference
                            // 1. Create backup
                            val currentDataSnap = rootRef.child(type.dataKey).get().await()
                            val currentData = currentDataSnap.getValue(String::class.java)
                            val timestamp = System.currentTimeMillis()
                            if (currentData != null) {
                                rootRef.child("backups").child(type.backupNode).child(timestamp.toString()).setValue(currentData).await()
                            }
                            
                            uploadStatus = "Uploading new data..."
                            // 2. Upload new data
                            val newDataString = if (type == ContentType.DUAS) {
                                val currentArrayStr = currentData ?: "[]"
                                val currentArray = try { org.json.JSONArray(currentArrayStr) } catch (e: Exception) { org.json.JSONArray() }
                                
                                val uploadedData = try {
                                    org.json.JSONArray(confirmedJson)
                                } catch (e: Exception) {
                                    org.json.JSONArray().put(org.json.JSONObject(confirmedJson))
                                }
                                
                                for (j in 0 until uploadedData.length()) {
                                    val newObj = uploadedData.getJSONObject(j)
                                    newObj.put("category", selectedCategory)
                                    val newId = newObj.optString("id")
                                    
                                    var replaced = false
                                    for (i in 0 until currentArray.length()) {
                                        val existingObj = currentArray.getJSONObject(i)
                                        if (existingObj.optString("id") == newId) {
                                            currentArray.put(i, newObj)
                                            replaced = true
                                            break
                                        }
                                    }
                                    if (!replaced) {
                                        currentArray.put(newObj)
                                    }
                                }
                                currentArray.toString()
                            } else {
                                confirmedJson
                            }
                            rootRef.child(type.dataKey).setValue(newDataString).await()
                            
                            // 3. Update version & timestamp
                            val newVersion = (currentVersion ?: 0L) + 1L
                            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val dateString = formatter.format(java.util.Date(timestamp))
                            
                            rootRef.child(type.versionKey).setValue(newVersion).await()
                            rootRef.child("${type.versionKey}_last_updated").setValue(dateString).await()
                            
                            uploadStatus = "Success! Published version $newVersion."
                            isUploading = false
                            selectedUri = null
                            parsedJsonString = null
                            isValid = false
                            validationMessage = null
                            loadData()
                            
                        } catch (e: Exception) {
                            uploadStatus = "Error: ${e.message}"
                            isUploading = false
                        }
                    }
                }) {
                    Text("Yes, Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestoreDialog) {
        RestoreDialog(type, database) {
            showRestoreDialog = false
            loadData()
        }
    }
}

@Composable
fun RestoreDialog(type: ContentType, database: FirebaseDatabase, onDismiss: () -> Unit) {
    var backups by remember { mutableStateOf<List<DataSnapshot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.reference.child("backups").child(type.backupNode).get().addOnSuccessListener { snap ->
            val list = snap.children.toList().sortedByDescending { it.key?.toLongOrNull() ?: 0L }
            backups = list
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (backups.isEmpty()) {
                Text("No backups found.")
            } else {
                LazyColumn {
                    items(backups) { backupSnap ->
                        val ts = backupSnap.key?.toLongOrNull() ?: 0L
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val dateString = formatter.format(java.util.Date(ts))
                        
                        ListItem(
                            headlineContent = { Text(dateString) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val backupData = backupSnap.getValue(String::class.java)
                                    if (backupData != null) {
                                        // Upload backup data
                                        database.reference.child(type.dataKey).setValue(backupData).await()
                                        // Increment version
                                        val vSnap = database.reference.child(type.versionKey).get().await()
                                        val currentVersion = vSnap.getValue(Long::class.java) ?: 0L
                                        database.reference.child(type.versionKey).setValue(currentVersion + 1L).await()
                                        database.reference.child("${type.versionKey}_last_updated").setValue("Restored: $dateString").await()
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicEventsAdminView() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var eventsList by remember { mutableStateOf<List<com.example.data.MasoomEvent>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    // Dialog states
    var showEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<com.example.data.MasoomEvent?>(null) }
    var isNewEvent by remember { mutableStateOf(false) }

    fun refreshEvents() {
        eventsList = com.example.data.IslamicEventManager.getEvents(context)
    }

    LaunchedEffect(Unit) {
        refreshEvents()
    }

    val filteredEvents = remember(eventsList, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            eventsList
        } else {
            val q = searchQuery.lowercase()
            eventsList.filter { 
                it.title.lowercase().contains(q) ||
                it.titleUrdu.contains(q) ||
                it.dateStringHijri.lowercase().contains(q) ||
                it.eventType.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📅 Islamic Events & Masoomeen JSON Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Any dates modified or added here will automatically reflect across the entire app (Calendar, Islamic Events Calendar, Cards & Notifications).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Country / Regional Narration Preference Card
        var selectedRegionPref by remember { mutableStateOf(com.example.data.IslamicEventManager.getRegionPreference(context)) }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🌍", fontSize = 16.sp)
                    Text(
                        text = "Country / Regional Narration Setting (علاقائی / روایتی ترجیح)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Select region standard for multi-narration dates (e.g. 17 Safar for Pak/South Asia vs 30 Safar for Iran/Hawza):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.example.data.RegionConvention.values().forEach { reg ->
                        val isSel = selectedRegionPref == reg
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedRegionPref = reg
                                com.example.data.IslamicEventManager.setRegionPreference(context, reg)
                                refreshEvents()
                                statusMessage = "Set country convention to: ${reg.flag} ${reg.label}"
                            },
                            label = {
                                Text("${reg.flag} ${reg.label.split(" ").first()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Action controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search dates, events...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            Button(onClick = {
                editingEvent = com.example.data.MasoomEvent(
                    id = "event_${System.currentTimeMillis()}",
                    title = "",
                    titleUrdu = "",
                    dateStringHijri = "13 Safar",
                    hijriMonth = 2,
                    hijriDay = 13,
                    eventType = com.example.data.EventType.SHAHADAT,
                    description = ""
                )
                isNewEvent = true
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Date")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Events: ${eventsList.size} (${filteredEvents.size} shown)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(onClick = {
                com.example.data.IslamicEventManager.resetToDefault(context)
                refreshEvents()
                statusMessage = "Reset to default JSON successfully!"
            }) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Defaults")
            }
        }

        statusMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // List of Events
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredEvents, key = { it.id.ifEmpty { "${it.title}_${it.dateStringHijri}" } }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (item.region != "ALL") {
                                    val regEnum = com.example.data.RegionConvention.values().find { it.code == item.region }
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "${regEnum?.flag ?: "🌐"} ${regEnum?.code ?: item.region}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                Surface(
                                    color = if (item.eventType == com.example.data.EventType.SHAHADAT) 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else 
                                        MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${item.eventType.name} • ${item.dateStringHijri}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = if (item.eventType == com.example.data.EventType.SHAHADAT) 
                                            MaterialTheme.colorScheme.onErrorContainer 
                                        else 
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (item.titleUrdu.isNotBlank()) {
                            Text(
                                text = item.titleUrdu,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (item.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                editingEvent = item
                                isNewEvent = false
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                val newList = eventsList.filterNot { it == item || (it.id.isNotBlank() && it.id == item.id) }
                                com.example.data.IslamicEventManager.saveEvents(context, newList)
                                // Sync to Firebase safely
                                scope.launch {
                                    try {
                                        com.example.data.IslamicEventManager.publishEventsToFirebase(newList)

                                        refreshEvents()
                                        statusMessage = "Deleted '${item.title}' and synced to Firebase!"
                                    } catch (e: Exception) {
                                        statusMessage = "Local Delete OK, but Cloud Sync Failed: ${e.message}"
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit/Add Dialog
    val eventBeingEdited = editingEvent
    if (showEditDialog && eventBeingEdited != null) {
        var title by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.title) }
        var titleUrdu by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.titleUrdu) }
        var titleArabic by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.titleArabic) }
        var dateStringHijri by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.dateStringHijri) }
        var hijriMonth by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.hijriMonth.toString()) }
        var hijriDay by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.hijriDay.toString()) }
        var eventType by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.eventType) }
        var region by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.region) }
        var description by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.description) }
        var descriptionUrdu by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.descriptionUrdu) }
        var descriptionArabic by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.descriptionArabic) }
        var sourceUrl by remember(eventBeingEdited) { mutableStateOf(eventBeingEdited.sourceUrl) }

        val monthNumber = hijriMonth.toIntOrNull()
        val dayNumber = hijriDay.toIntOrNull()
        val isEventValid = title.isNotBlank() && dateStringHijri.isNotBlank() &&
            monthNumber != null && monthNumber in 1..12 &&
            dayNumber != null && dayNumber in 1..30 &&
            (sourceUrl.isBlank() || sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://"))

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (isNewEvent) "Add Islamic Calendar Event" else "Edit Event Details") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = titleUrdu,
                        onValueChange = { titleUrdu = it },
                        label = { Text("Urdu Title (اردو عنوان)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = titleArabic,
                        onValueChange = { titleArabic = it },
                        label = { Text("Arabic Title (العنوان بالعربية)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateStringHijri,
                        onValueChange = { dateStringHijri = it },
                        label = { Text("Hijri Date String (e.g., 13 Safar)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hijriMonth,
                            onValueChange = { hijriMonth = it.filter(Char::isDigit).take(2) },
                            label = { Text("Month (1-12)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hijriDay,
                            onValueChange = { hijriDay = it.filter(Char::isDigit).take(2) },
                            label = { Text("Day (1-30)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = eventType == com.example.data.EventType.SHAHADAT,
                            onClick = { eventType = com.example.data.EventType.SHAHADAT },
                            label = { Text("SHAHADAT") }
                        )
                        FilterChip(
                            selected = eventType == com.example.data.EventType.WILADAT,
                            onClick = { eventType = com.example.data.EventType.WILADAT },
                            label = { Text("WILADAT") }
                        )
                    }

                    Text("Region Tag (روایتی / علاقائی ترجیح):", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        com.example.data.RegionConvention.values().forEach { reg ->
                            FilterChip(
                                selected = region == reg.code,
                                onClick = { region = reg.code },
                                label = { Text("${reg.flag} ${reg.code}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = descriptionUrdu,
                        onValueChange = { descriptionUrdu = it },
                        label = { Text("Description (اردو)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = descriptionArabic,
                        onValueChange = { descriptionArabic = it },
                        label = { Text("Description (العربية)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("Reference URL (optional)") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = isEventValid,
                    onClick = {
                        val updatedItem = eventBeingEdited.copy(
                            title = title.trim(),
                            titleUrdu = titleUrdu.trim(),
                            titleArabic = titleArabic.trim(),
                            dateStringHijri = dateStringHijri.trim(),
                            hijriMonth = monthNumber ?: eventBeingEdited.hijriMonth,
                            hijriDay = dayNumber ?: eventBeingEdited.hijriDay,
                            eventType = eventType,
                            region = region,
                            description = description.trim(),
                            descriptionUrdu = descriptionUrdu.trim(),
                            descriptionArabic = descriptionArabic.trim(),
                            sourceUrl = sourceUrl.trim()
                        )
                        val newList = if (isNewEvent) {
                            eventsList + updatedItem
                        } else {
                            eventsList.map {
                                if (it.id == updatedItem.id || (it.id.isBlank() && it.title == eventBeingEdited.title)) updatedItem else it
                            }
                        }
                        com.example.data.IslamicEventManager.saveEvents(context, newList)

                        statusMessage = "Uploading to Firebase..."
                        scope.launch {
                            try {
                                com.example.data.IslamicEventManager.publishEventsToFirebase(newList)

                                refreshEvents()
                                showEditDialog = false
                                statusMessage = "Successfully updated and synced to Firebase!"
                            } catch (e: Exception) {
                                refreshEvents()
                                showEditDialog = false
                                statusMessage = "Saved Locally, but Firebase Upload Failed: ${e.message} - Check Firebase Rules!"
                            }
                        }
                    }
                ) {
                    Text("Save & Sync JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
