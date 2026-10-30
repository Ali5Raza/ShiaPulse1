package com.example.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    viewModel: PrayerViewModel,
    currentLang: String,
    onBack: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onNotificationPermissionRequested: (NotificationPermissionTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val themeMode by viewModel.themeMode.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val autoOccasionTheme by viewModel.autoOccasionTheme.collectAsState()
    val themeOverride by viewModel.themeOverride.collectAsState()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val isAdhanEnabled by viewModel.isAdhanEnabled.collectAsState()
    val isFajrAdhanEnabled by viewModel.isFajrAdhanEnabled.collectAsState()
    val isDhuhrAdhanEnabled by viewModel.isDhuhrAdhanEnabled.collectAsState()
    val isAsrAdhanEnabled by viewModel.isAsrAdhanEnabled.collectAsState()
    val isMaghribAdhanEnabled by viewModel.isMaghribAdhanEnabled.collectAsState()
    val isIshaAdhanEnabled by viewModel.isIshaAdhanEnabled.collectAsState()

    val aamalNotifEnabled by viewModel.aamalNotifEnabled.collectAsState()
    val aamalNotifAuto by viewModel.aamalNotifAuto.collectAsState()
    val aamalNotifHour by viewModel.aamalNotifHour.collectAsState()
    val aamalNotifMinute by viewModel.aamalNotifMinute.collectAsState()

    val showGuidanceOnStartup by viewModel.showGuidanceOnStartup.collectAsState()
    val adhanSoundKey by viewModel.adhanSoundKey.collectAsState()
    val isTickerActiveVal by viewModel.isTickerActive.collectAsState()
    val showCalendarInNotificationBar by viewModel.showCalendarInNotificationBar.collectAsState()
    val use24HourFormat by viewModel.use24HourFormat.collectAsState()

    fun settingsText(key: String): String = com.example.utils.LocalizationUtility.get(key, currentLang)

    fun setNotificationSetting(
        target: NotificationPermissionTarget,
        enabled: Boolean,
        applySetting: (Boolean) -> Unit
    ) {
        val permissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (enabled && NotificationPermissionPolicy.shouldRequestRuntimePermission(
                android.os.Build.VERSION.SDK_INT,
                permissionGranted
            )
        ) {
            onNotificationPermissionRequested(target)
        } else {
            applySetting(enabled)
        }
    }

    var playingAdhanPreviewKey by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAdhanPreview()
        }
    }

    val faqList = remember(currentLang) {
        when (currentLang) {
            "ur" -> listOf(
                "نماز کے اوقات کیسے شمار ہوتے ہیں؟" to "اوقات آپ کے مقام کے مطابق شمار کیے جاتے ہیں۔ ظہر شمسی عبور پر، فجر 16° کے زاویے پر، اور مغرب غروب کے بعد تقریباً 4 ڈگری (15–20 منٹ) کے شیعہ فقہی ضابطے پر ہے۔",
                "میرے مقام سے قبلے کا زاویہ کیا ہے؟" to "قبلے کا زاویہ مکہ میں خانہ کعبہ (21.4225° N، 39.8262° E) سے عظیم دائرے کے حساب سے نکالا جاتا ہے۔ اپنے کمپس سینسر کی کیلیبریشن ضرور چیک کریں۔",
                "کیا آف لائن بیک اپ دستیاب ہے؟" to "جی ہاں! گوگل لاگ اِن کے بعد آپ کے ٹریکنگ لاگز اور قضا شمار مقامی طور پر محفوظ رہتے ہیں اور انٹرنیٹ دستیاب ہونے پر فائربیس سے سنک ہوتے ہیں۔"
            )
            "ar" -> listOf(
                "كيف تُحسب أوقات الصلاة؟" to "تُحسب الأوقات حسب موقعك: يبدأ الظهر عند الزوال، والفجر عند زاوية 16°، والمغرب بعد الغروب بنحو 4 درجات (15–20 دقيقة) وفق الضابط الشيعي.",
                "ما زاوية القبلة من موقعي؟" to "تُحسب زاوية القبلة بالدائرة العظيمة إلى الكعبة في مكة (21.4225° N، 39.8262° E). تأكد من معايرة مستشعر البوصلة.",
                "هل تتوفر النسخ الاحتياطية دون اتصال؟" to "نعم! بعد تسجيل الدخول باستخدام Google، تُحفظ سجلات التتبع وعدادات القضاء محليًا وتتم مزامنتها مع Firebase عند توفر الإنترنت."
            )
            else -> listOf(
                "How are prayer times calculated?" to "Timings utilize exact geolocal structures. Dhuhr starts at astronomical solar transit, Fajr uses 16° angle, and Maghrib is strictly 4 degrees (around 15-20 min astronomical delay) past sunset representing the Shia school.",
                "What is the Qibla angle from my location?" to "The Qibla angle is computed based on Great Circle distance to the Kaaba in Mecca, Saudi Arabia (21.4225° N, 39.8262° E). Ensure the compass sensor is calibrated.",
                "Are offline backups supported?" to "Yes! Once you log in with Google, your tracking logs and Qada counts are continuously cached locally and synced automatically with the Firebase cloud database whenever an internet connection is available."
            )
        }
    }
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    var isAboutUsExpanded by remember { mutableStateOf(false) }
    var isTermsExpanded by remember { mutableStateOf(false) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }

    val settingsLayoutDirection = if (currentLang == "ur" || currentLang == "ar") {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    } else {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides settingsLayoutDirection
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PremiumProfileHubHeader(onBack = onBack, currentLang = currentLang)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)
                ) {
                    // PROFILE / AUTH SECTION
                    item {
                        PremiumSettingsSectionHeading(settingsText("settings_account_cloud"))
                        if (isLoggedIn) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                                shape = RoundedCornerShape(18.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFD4AF37), Color(0xFF00E676))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (userDisplayName ?: "H").take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = userDisplayName ?: "Shia Pulse Companion",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = userEmail ?: "User Account",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Realtime Cloud Sync Status Output
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (syncStatus.startsWith("Error") || syncStatus.contains("fail", ignoreCase = true)) Color(0xFFEF4444)
                                                        else Color(0xFF00E676)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cloud Sync: $syncStatus",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Manual Trigger Actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.backupDataToFirebase() },
                                            enabled = !isSyncing,
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, "Backup", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(settingsText("settings_backup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.restoreDataFromFirebase() },
                                            enabled = !isSyncing,
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(Icons.Default.CloudDownload, "Restore", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(settingsText("settings_restore"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Google Connected",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF00E676),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.logout() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Text(settingsText("settings_sign_out"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                                shape = RoundedCornerShape(18.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    PremiumSettingsRow(
                                        title = "Cloud Sync & Backup",
                                        subtitle = "Sign in with Google to securely back up prayer logs, Tasbeeh tallies, and personalized settings.",
                                        icon = Icons.Default.CloudUpload
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val googleIdOption = GetGoogleIdOption.Builder()
                                                        .setFilterByAuthorizedAccounts(false)
                                                        .setServerClientId("692524830790-d0acko25ljbhlf5qd7ddil20osqcgmfb.apps.googleusercontent.com")
                                                        .setAutoSelectEnabled(true)
                                                        .build()

                                                    val request = GetCredentialRequest.Builder()
                                                        .addCredentialOption(googleIdOption)
                                                        .build()

                                                    val result = credentialManager.getCredential(context = context, request = request)
                                                    val credential = result.credential
                                                    if (credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                                        val idToken = googleIdTokenCredential.idToken
                                                        val email = googleIdTokenCredential.id
                                                        val displayName = googleIdTokenCredential.displayName ?: email
                                                        viewModel.loginWithGoogle(email, displayName, idToken)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("GoogleSignIn", "Failed to sign in", e)
                                                    android.widget.Toast.makeText(context, settingsText("onboarding_sign_in_error"), android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_login_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBox, "Google", tint = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(settingsText("settings_sign_in"), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // APPEARANCE & TYPOGRAPHY
                    item {
                        PremiumSettingsSectionHeading(settingsText("settings_personalization"))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PremiumSettingsRow(
                                    title = "Arabic & Urdu Script Size",
                                    subtitle = "Adjust text scaling for prayer times, Duas, and Ziyarats",
                                    icon = Icons.Default.TextFields
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sizes = listOf(
                                        1.0f to "1.0x",
                                        1.2f to "1.2x",
                                        1.4f to "1.4x",
                                        1.6f to "1.6x"
                                    )
                                    sizes.forEach { (scale, labelScale) ->
                                        val fontScaleVal by viewModel.arabicUrduFontScale.collectAsState()
                                        val isSelected = Math.abs(fontScaleVal - scale) < 0.05f
                                        PremiumSettingsChoiceButton(
                                            text = labelScale,
                                            selected = isSelected,
                                            onClick = { viewModel.setArabicUrduFontScale(scale) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("dialog_font_scale_btn_${scale.toString().replace(".", "_")}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // LANGUAGE OPTION
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = settingsText("settings_interface_language"),
                                icon = Icons.Default.Translate
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("en" to "English", "ar" to "العربية", "ur" to "اردو").forEach { (code, langLabel) ->
                                    PremiumSettingsChoiceButton(
                                        text = langLabel,
                                        selected = currentLang == code,
                                        onClick = { viewModel.setAppLanguage(code) },
                                        modifier = Modifier.weight(1f).testTag("lang_btn_$code")
                                    )
                                }
                            }
                        }
                    }

                    // APP THEME OPTION
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = settingsText("settings_theme"),
                                icon = Icons.Default.Palette
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("dark" to settingsText("settings_dark"), "light" to settingsText("settings_light"), "system" to settingsText("settings_system")).forEach { (code, themeLabel) ->
                                    PremiumSettingsChoiceButton(
                                        text = themeLabel,
                                        selected = themeMode == code,
                                        onClick = { viewModel.setThemeMode(code) },
                                        modifier = Modifier.weight(1f).testTag("theme_btn_$code")
                                    )
                                }
                            }
                        }
                    }

                    // NEWS TICKER ON/OFF SETTING
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = "News & Events Ticker Bar",
                                subtitle = "Toggle sticky bottom news & Shi'ite event ticker stream",
                                icon = Icons.Default.Notifications
                            ) {
                                Switch(
                                    checked = isTickerActiveVal,
                                    onCheckedChange = { viewModel.setTickerActive(it) },
                                    modifier = Modifier.testTag("dialog_ticker_active_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // OCCASION THEME SETTING
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = "Occasion-Aware Palette",
                                subtitle = "Automate festive green for Wiladats / dark solemn rose for Shahadats",
                                icon = Icons.Default.AutoAwesome
                            ) {
                                Switch(
                                    checked = autoOccasionTheme,
                                    onCheckedChange = { viewModel.setAutoOccasionTheme(it) },
                                    modifier = Modifier.testTag("dialog_auto_occasion_theme_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = "Ahlul Bayt Occasion Theme Previews",
                                icon = Icons.Default.AutoAwesome
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "none" to "Auto Settings",
                                    "wiladat" to "Wiladat Mode",
                                    "shahadat" to "Shahadat Mode"
                                ).forEach { (choice, label) ->
                                    PremiumSettingsChoiceButton(
                                        text = label,
                                        selected = themeOverride == choice,
                                        onClick = { viewModel.setThemeOverride(choice) },
                                        modifier = Modifier.weight(1f).testTag("dialog_override_btn_$choice")
                                    )
                                }
                            }
                        }
                    }

                    // HIJRI OFFSET SETTING
                    item {
                        val context = LocalContext.current
                        val prefs = remember { context.getSharedPreferences("shiapulse_prefs", android.content.Context.MODE_PRIVATE) }
                        var tempOffset by remember { mutableStateOf(com.example.utils.HijriCalendarHelper.globalOffsetDays) }
                        var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("hijri_auto_sync", true)) }
                        var sourceDesc by remember { mutableStateOf(prefs.getString("hijri_offset_source_desc", "") ?: "") }
                        val coroutineScope = rememberCoroutineScope()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            PremiumSettingsRow(
                                title = "Hijri Calendar Offset",
                                subtitle = "Adjust the Hijri calendar if the moon is sighted early or late in your region (-2 to +2 days).",
                                icon = Icons.Default.DateRange
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (tempOffset > -2) {
                                        tempOffset--
                                        prefs.edit()
                                            .putInt("user_manual_offset", tempOffset)
                                            .putInt("hijri_offset_days", tempOffset)
                                            .putInt("resolved_hijri_offset", tempOffset)
                                            .putBoolean("hijri_auto_sync", false)
                                            .putString("hijri_offset_source", "manual")
                                            .putString("hijri_offset_source_desc", "Manual Custom Offset (${if (tempOffset > 0) "+$tempOffset" else "$tempOffset"}d)")
                                            .apply()
                                        autoSyncEnabled = false
                                        sourceDesc = "Manual Custom Offset (${if (tempOffset > 0) "+$tempOffset" else "$tempOffset"}d)"
                                        com.example.utils.HijriCalendarHelper.globalOffsetDays = tempOffset
                                        // Force widgets update
                                        try {
                                            val widgetIntent = android.content.Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                                                component = android.content.ComponentName(context, com.example.receiver.HijriWidgetProvider::class.java)
                                            }
                                            context.sendBroadcast(widgetIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        viewModel.changeDate(0)
                                    }
                                }) {
                                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = if (tempOffset > 0) "+$tempOffset Days" else if (tempOffset < 0) "$tempOffset Days" else "0 Days",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                IconButton(onClick = {
                                    if (tempOffset < 2) {
                                        tempOffset++
                                        prefs.edit()
                                            .putInt("user_manual_offset", tempOffset)
                                            .putInt("hijri_offset_days", tempOffset)
                                            .putInt("resolved_hijri_offset", tempOffset)
                                            .putBoolean("hijri_auto_sync", false)
                                            .putString("hijri_offset_source", "manual")
                                            .putString("hijri_offset_source_desc", "Manual Custom Offset (${if (tempOffset > 0) "+$tempOffset" else "$tempOffset"}d)")
                                            .apply()
                                        autoSyncEnabled = false
                                        sourceDesc = "Manual Custom Offset (${if (tempOffset > 0) "+$tempOffset" else "$tempOffset"}d)"
                                        com.example.utils.HijriCalendarHelper.globalOffsetDays = tempOffset
                                        // Force widgets update
                                        try {
                                            val widgetIntent = android.content.Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                                                component = android.content.ComponentName(context, com.example.receiver.HijriWidgetProvider::class.java)
                                            }
                                            context.sendBroadcast(widgetIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        viewModel.changeDate(0)
                                    }
                                }) {
                                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PremiumSettingsRow(
                                title = "Auto-Sync Hijri Calendar",
                                subtitle = if (autoSyncEnabled) {
                                    if (sourceDesc.isNotEmpty()) "Auto-sync active: $sourceDesc"
                                    else "Auto-sync active (Priority 1: Content Management by country, Priority 2: Auto library/API)."
                                } else {
                                    "Auto-sync disabled. Your manual custom offset is active."
                                },
                                icon = Icons.Default.Refresh
                            ) {
                                Switch(
                                    checked = autoSyncEnabled,
                                    onCheckedChange = { isChecked ->
                                        com.example.utils.HijriConfigurationGeneration.invalidate()
                                        autoSyncEnabled = isChecked
                                        prefs.edit().putBoolean("hijri_auto_sync", isChecked).apply()
                                        if (isChecked) {
                                            coroutineScope.launch {
                                                try {
                                                    val countryService = com.example.utils.DefaultCountryDetectionService(context)
                                                    val remoteConfigService = com.example.utils.DefaultRemoteConfigurationService(context)
                                                    val hijriConfigService = com.example.utils.DefaultHijriConfigurationService(countryService, remoteConfigService)
                                                    hijriConfigService.resolveAndApplyOffset(context)
                                                    com.example.utils.HijriCalendarHelper.fetchAndSyncOffsetFromApi(context)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                tempOffset = com.example.utils.HijriCalendarHelper.globalOffsetDays
                                                sourceDesc = prefs.getString("hijri_offset_source_desc", "") ?: ""
                                                viewModel.changeDate(0)
                                                try {
                                                    val widgetIntent = android.content.Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                                                        `package` = context.packageName
                                                    }
                                                    context.sendBroadcast(widgetIntent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        } else {
                                            prefs.edit()
                                                .putInt("user_manual_offset", tempOffset)
                                                .putInt("hijri_offset_days", tempOffset)
                                                .putInt("resolved_hijri_offset", tempOffset)
                                                .putString("hijri_offset_source", "manual")
                                                .putString("hijri_offset_source_desc", "Manual Custom Offset (${if (tempOffset > 0) "+$tempOffset" else "$tempOffset"}d)")
                                                .apply()
                                            com.example.utils.HijriCalendarHelper.globalOffsetDays = tempOffset
                                            sourceDesc = "Manual Custom Offset (${if (tempOffset > 0) "+$tempOffset" else "$tempOffset"}d)"
                                            viewModel.changeDate(0)
                                            try {
                                                val widgetIntent = android.content.Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                                                    `package` = context.packageName
                                                }
                                                context.sendBroadcast(widgetIntent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // SHOW CALENDAR IN MOBILE NOTIFICATION BAR (Default: OFF)
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = "Show Calendar in Notification Bar",
                                subtitle = "Display persistent Hijri & Gregorian date with Shia occasions in your mobile's notification bar.",
                                icon = Icons.Default.DateRange
                            ) {
                                Switch(
                                    checked = showCalendarInNotificationBar,
                                    onCheckedChange = { enabled ->
                                        setNotificationSetting(
                                            NotificationPermissionTarget.CALENDAR,
                                            enabled,
                                            viewModel::setShowCalendarInNotificationBar
                                        )
                                    },
                                    modifier = Modifier.testTag("show_calendar_in_notification_bar_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // TIME FORMAT (12H / 24H)
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = "Use 24-Hour Format",
                                icon = Icons.Default.AccessTime
                            ) {
                                Switch(
                                    checked = use24HourFormat,
                                    onCheckedChange = { viewModel.setUse24HourFormat(it) },
                                    modifier = Modifier.testTag("use_24h_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // CALCULATOR CURRENCY OPTION
                    item {
                        PremiumSettingsGroup {
                            PremiumSettingsRow(
                                title = "Calculator Currency",
                                icon = Icons.Default.CurrencyExchange
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("$" to "USD/CAD", "PKR" to "PKR", "£" to "GBP", "€" to "EUR", "₹" to "INR", "AED" to "AED").forEach { (code, label) ->
                                    PremiumSettingsChoiceButton(
                                        text = "$code ($label)",
                                        selected = currencySymbol == code,
                                        onClick = { viewModel.setCurrencySymbol(code) },
                                        modifier = Modifier.testTag("currency_btn_$code")
                                    )
                                }
                            }
                        }
                    }

                    // ADHAN NOTIFICATIONS
                    item {
                        PremiumSettingsSectionHeading(settingsText("settings_notifications"))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                PremiumSettingsRow(
                                    title = "Adhan & Prayer Notifications",
                                    subtitle = (if (isAdhanEnabled) "Adhan Active" else "Adhan Muted") + " • Play beautiful Shia Adhan stream on timing intervals.",
                                    icon = Icons.Default.Notifications
                                ) {
                                    Switch(
                                        checked = isAdhanEnabled,
                                        onCheckedChange = { isEnabled ->
                                            setNotificationSetting(
                                                NotificationPermissionTarget.ADHAN,
                                                isEnabled
                                            ) { enabled ->
                                                viewModel.setAdhanEnabled(enabled, context)
                                            }
                                        },
                                        modifier = Modifier.testTag("adhan_settings_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }

                                if (isAdhanEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Namaz Specific Alarm Settings:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val enabledByPrayer = mapOf(
                                        "Fajr" to isFajrAdhanEnabled,
                                        "Dhuhr" to isDhuhrAdhanEnabled,
                                        "Asr" to isAsrAdhanEnabled,
                                        "Maghrib" to isMaghribAdhanEnabled,
                                        "Isha" to isIshaAdhanEnabled
                                    )
                                    val prayerToggles = com.example.receiver.AdhanScheduler
                                        .prayerNamesForSettings()
                                        .map { prayerName -> prayerName to enabledByPrayer.getValue(prayerName) }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        prayerToggles.forEach { (pName, isEnabled) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                            ) {
                                                PremiumSettingsRow(
                                                    title = pName,
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                ) {
                                                    Switch(
                                                        checked = isEnabled,
                                                        onCheckedChange = { viewModel.setPrayerAdhanEnabled(pName, it) },
                                                        modifier = Modifier.testTag("adhan_switch_${pName.lowercase()}"),
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ADHAN SOUND NOTIFICATION
                    if (isAdhanEnabled) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                                shape = RoundedCornerShape(18.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    PremiumSettingsRow(
                                        title = "Adhan Sound Notification",
                                        icon = Icons.Default.MusicNote
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val adhanSounds = listOf(
                                        Triple("mesmerizing_shia_azan", "Mesmerizing Shia Azan", "Beautiful, highly melodious and spiritually resonant local Shia Azan")
                                    )

                                    adhanSounds.forEach { (key, title, desc) ->
                                        val isSelected = adhanSoundKey == key
                                        val isCurrentlyPlaying = playingAdhanPreviewKey == key

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 72.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { viewModel.setAdhanSoundKey(key) }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { viewModel.setAdhanSoundKey(key) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = title,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = desc,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (isCurrentlyPlaying) {
                                                            viewModel.stopAdhanPreview()
                                                            playingAdhanPreviewKey = null
                                                        } else {
                                                            viewModel.stopAdhanPreview()
                                                            viewModel.playAdhanPreview(context, key)
                                                            playingAdhanPreviewKey = key
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCurrentlyPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                                                        contentDescription = "Test Sound",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // AAMAL NOTIFICATION SECTION
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("settings_aamal_notification_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                PremiumSettingsRow(
                                    title = "Aamal Habit Reminder Settings",
                                    subtitle = (if (aamalNotifEnabled) "Aamal Reminder Active" else "Aamal Reminder Disabled") + " • Receive notifications to complete your daily Aamals.",
                                    icon = Icons.Default.Notifications
                                ) {
                                    Switch(
                                        checked = aamalNotifEnabled,
                                        onCheckedChange = { isEnabled ->
                                            setNotificationSetting(
                                                NotificationPermissionTarget.AAMAL,
                                                isEnabled
                                            ) { enabled ->
                                                viewModel.setAamalNotifEnabled(enabled, context)
                                            }
                                        },
                                        modifier = Modifier.testTag("aamal_notif_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }

                                if (aamalNotifEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            PremiumSettingsRow(
                                                title = "Notification Timing",
                                                subtitle = if (aamalNotifAuto) "Automatically determined by activity times" else "Custom fixed time",
                                                icon = Icons.Default.AccessTime
                                            ) {
                                                Switch(
                                                    checked = aamalNotifAuto,
                                                    onCheckedChange = { isAuto ->
                                                        viewModel.setAamalNotifAutoOrTime(isAuto, aamalNotifHour, aamalNotifMinute, context)
                                                    },
                                                    modifier = Modifier.testTag("aamal_notif_auto_switch"),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                )
                                            }

                                            if (!aamalNotifAuto) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    var typedHour by remember(aamalNotifHour) { mutableStateOf(aamalNotifHour.toString()) }
                                                    var typedMin by remember(aamalNotifMinute) { mutableStateOf(String.format("%02d", aamalNotifMinute)) }

                                                    androidx.compose.material3.OutlinedTextField(
                                                        value = typedHour,
                                                        onValueChange = { typedHour = it.take(2) },
                                                        label = { Text("Hour (0-23)", fontSize = 10.sp) },
                                                        modifier = Modifier.weight(1f).height(56.dp),
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(":", fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    androidx.compose.material3.OutlinedTextField(
                                                        value = typedMin,
                                                        onValueChange = { typedMin = it.take(2) },
                                                        label = { Text("Min (0-59)", fontSize = 10.sp) },
                                                        modifier = Modifier.weight(1f).height(56.dp),
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            val h = typedHour.toIntOrNull() ?: 20
                                                            val m = typedMin.toIntOrNull() ?: 0
                                                            if (h in 0..23 && m in 0..59) {
                                                                viewModel.setAamalNotifAutoOrTime(false, h, m, context)
                                                                android.widget.Toast.makeText(context, "Time updated to ${String.format("%02d:%02d", h, m)}", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.height(40.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("Set", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ABOUT US SECTION
                    item {
                        PremiumSettingsSectionHeading(settingsText("settings_support"))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { isAboutUsExpanded = !isAboutUsExpanded }
                                .testTag("settings_about_us_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PremiumSettingsRow(
                                    title = "About Us",
                                    icon = Icons.Default.Info
                                ) {
                                    Icon(
                                        imageVector = if (isAboutUsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand About Us",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isAboutUsExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "This app is created with the sole intention of serving the Shia community by spreading the true message of Ahlulbayt (AS).\n\n" +
                                               "\"Indeed, my prayer, my sacrifice, my living, and my dying are for Allah, Lord of the worlds.\" (Holy Qur'an 6:162)\n\n" +
                                               "Our message is the same message that Rasulullah (SAWW) left behind:\n\n" +
                                               "\"I leave behind two weighty things for you: The Book of Allah and my Ahlulbayt (AS). If you hold onto both, you will never go astray.\" (Hadith al-Thaqalayn)\n\n" +
                                               "اللهم صل علی محمد و آل محمد\n\n" +
                                               "O Allah, send your blessings upon Muhammad and the family of Muhammad, as you have blessed Ibrahim and the family of Ibrahim. Indeed, you are Praiseworthy and Glorious.\n\n" +
                                               "This humble app is a small effort to keep the name of Muhammad (SAWW) and his pure Aale Muhammad (AS) alive in every heart.\n\n" +
                                               "For any feedback, suggestions, or duas:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Email action
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                                            .clickable {
                                                try {
                                                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:aliraza_riu@hotmail.com")
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Email"))
                                                } catch (e: Exception) {}
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "\u202Aaliraza_riu@hotmail.com\u202C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // WhatsApp action
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                                            .clickable {
                                                try {
                                                    val waIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        data = Uri.parse("https://wa.me/923485033712")
                                                    }
                                                    context.startActivity(waIntent)
                                                } catch (e: Exception) {}
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "WhatsApp",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "\u202A+92 348 5033712\u202C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF25D366),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "— Mojedarya (Individual Developer)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    // TERMS & CONDITIONS SECTION
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { isTermsExpanded = !isTermsExpanded }
                                .testTag("settings_terms_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PremiumSettingsRow(
                                    title = "Terms & Conditions",
                                    icon = Icons.Default.Lock
                                ) {
                                    Icon(
                                        imageVector = if (isTermsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Terms",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isTermsExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "By referencing or downloading the Shia Pulse software application, you acknowledge and agree to be bound by the following terms:\n\n" +
                                               "1. Data Privacy: All daily prayer logs, Tasbeeh counter logs, and user activity data belong to the user. We do not transmit or sell tracking metrics to secondary systems. Cloud syncing via Google and Firebase is exclusively voluntary.\n\n" +
                                               "2. Guidance Verification: Calculations for Shiah Astrological and astronomical timings (Dhuhr, Fajr Angle, Maghrib delay) are executed locally with maximum scientific calibration. However, they serve as general spiritual guidance.\n\n" +
                                               "3. Intellectual Property: All custom system code, graphics, branding, and resource layouts are the exclusive property of Mojedarya Software Solutions. Unauthorized redistribution is restricted.\n\n" +
                                               "Thank you for trusting Shia Pulse.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // PRIVACY POLICY SECTION
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { isPrivacyExpanded = !isPrivacyExpanded }
                                .testTag("settings_privacy_policy_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PremiumSettingsRow(
                                    title = "Privacy Policy",
                                    icon = Icons.Default.Check
                                ) {
                                    Icon(
                                        imageVector = if (isPrivacyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Privacy Policy",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isPrivacyExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Your privacy is of paramount importance to us. This Privacy Policy outlines exactly how Shia Pulse handles data, complying directly with Google Play Store guidelines:\n\n" +
                                               "1. Local SQLite Database (Room): All interactive data entered by you, including daily prayer logs, tasbeeh tallies, Qada prayer balances, and saved bookmarked Ayat/Hadith/Dua nodes, are saved locally on your active handset device in a secure SQLite structure powered by Room persistence.\n\n" +
                                               "2. Offline Configurations: User selections like font scaling settings, custom translation languages (English, Arabic, Urdu), and geographic location data for local prayer mathematical offsets are stored strictly on-device utilizing local Android SharedPreferences.\n\n" +
                                               "3. Voluntary Cloud backup: Syncing settings and database entries to the cloud is completely optional. If and only if you voluntarily authenticate and choose to log in using Google/Firebase, are your records safely backed up to your personalized cloud container to preserve user states across multi-device configurations.\n\n" +
                                               "4. Transit and Processing Safety: No personal identifiers, bookmarks, activity tracks, or telemetry analytics are gathered, cataloged, or forwarded to external servers or secondary marketing brokers. When selecting AI features (Gemini API) or Location finders, processing is done securely without transit tracking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // SHARE APP OPTION
                    item {
                        Button(
                            onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Shia Pulse")
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "Assalamu Alaikum! Discover Shia Pulse - The comprehensive high-fidelity Shia companion app. Track your daily salat, keep counter of Tasbeeh, calculate Islamic inheritance and dues, view masoomeen calendar events and much more! Save & sync online. Download here: https://play.google.com/store/apps/details?id=com.mojedarya.shiapulse"
                                    )
                                }
                                try {
                                    if (shareIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Shia Pulse via"))
                                    } else {
                                        android.widget.Toast.makeText(context, "No app available to share.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .height(48.dp)
                                .testTag("share_app_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(settingsText("settings_share"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // FAQ OPTION LIST
                    item {
                        PremiumSettingsSectionHeading(settingsText("settings_faq"))

                        faqList.forEachIndexed { index, (question, answer) ->
                            val isExpanded = expandedFaqIndex == index
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { expandedFaqIndex = if (isExpanded) null else index },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                                shape = RoundedCornerShape(18.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    PremiumSettingsRow(
                                        title = question,
                                        icon = Icons.Default.Info
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand FAQ",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = answer,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (FirebaseAuth.getInstance().currentUser?.email == "syedmohammadaliraza572@gmail.com") {
                        item {
                            Button(
                                onClick = {
                                    onOpenAdminPanel()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, "Admin Panel", tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Content Admin Panel", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
