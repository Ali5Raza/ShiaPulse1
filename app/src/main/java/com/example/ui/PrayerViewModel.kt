package com.example.ui

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.DailyContentClient
import com.example.api.ShiaQuote
import com.example.data.*
import com.example.utils.DeviceLocationAccuracy
import com.example.utils.HomeDayContext
import com.example.utils.HomeDayContextFactory
import com.example.utils.HomePrayerTimelines
import com.example.utils.LocationTimeZone
import com.example.utils.PrayerTimeCalculator
import com.example.utils.resolveDeviceLocationAccuracy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = PrayerDatabase.getDatabase(application).prayerDao()

    val allZiyarats: StateFlow<List<ZiyaratItem>> = dao.getAllZiyarats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allZiyaratVisits: StateFlow<List<ZiyaratVisitRecord>> = dao.getAllZiyaratVisits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allZiyaratJournals: StateFlow<List<ZiyaratJournalRecord>> = dao.getAllZiyaratJournals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allHajjUmrahStates: StateFlow<List<HajjUmrahChecklistItem>> = dao.getAllHajjUmrahItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCollections: StateFlow<List<MyCollection>> = dao.getAllCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDuaReadingLogs: StateFlow<List<DuaReadingLog>> = dao.getAllDuaReadingLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun logDuaReading(duaId: String, dateString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentLogs = dao.getAllDuaReadingLogs().first() // Get current logs to check streak
            dao.insertDuaReadingLog(DuaReadingLog(duaId = duaId, dateString = dateString))
            
            // Re-fetch to calculate new streak
            val updatedLogs = dao.getAllDuaReadingLogs().first()
            val logsByDate = updatedLogs.groupBy { it.dateString }
            var currentStreak = 0
            var checkDate = java.util.Calendar.getInstance()
            val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val todayStr = df.format(checkDate.time)
            
            while (true) {
                val dateStr = df.format(checkDate.time)
                if (logsByDate.containsKey(dateStr)) {
                    currentStreak++
                    checkDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
                } else if (dateStr == todayStr) {
                    checkDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }

            // Check if streak milestone hit today
            val prefs = getApplication<android.app.Application>().getSharedPreferences("dua_goals", android.content.Context.MODE_PRIVATE)
            val lastToastStreak = prefs.getInt("last_toast_streak", 0)
            
            if (currentStreak in listOf(3, 7, 30) && currentStreak > lastToastStreak) {
                prefs.edit().putInt("last_toast_streak", currentStreak).apply()
                val lang = appLanguage.value
                val msg = if (lang == "ur") "مبارک ہو! آپ نے مسلسل $currentStreak دن دعائیں پڑھیں \uD83C\uDF89" else "Congratulations! You hit a $currentStreak-day Dua streak! \uD83C\uDF89"
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun createCollection(name: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCollection(MyCollection(name = name, description = description))
        }
    }

    fun deleteCollection(collection: MyCollection) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCollection(collection)
        }
    }

    fun addDuaToCollection(collection: MyCollection, duaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDuaIds = collection.duaIds.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (currentDuaIds.add(duaId)) {
                val updated = collection.copy(duaIds = currentDuaIds.joinToString(","))
                dao.insertCollection(updated)
            }
        }
    }

    fun removeDuaFromCollection(collection: MyCollection, duaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDuaIds = collection.duaIds.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (currentDuaIds.remove(duaId)) {
                val updated = collection.copy(duaIds = currentDuaIds.joinToString(","))
                dao.insertCollection(updated)
            }
        }
    }

    fun addZiyaratToCollection(collection: MyCollection, ziyaratId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentZiyaratIds = collection.ziyaratIds.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (currentZiyaratIds.add(ziyaratId)) {
                val updated = collection.copy(ziyaratIds = currentZiyaratIds.joinToString(","))
                dao.insertCollection(updated)
            }
        }
    }

    fun removeZiyaratFromCollection(collection: MyCollection, ziyaratId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentZiyaratIds = collection.ziyaratIds.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (currentZiyaratIds.remove(ziyaratId)) {
                val updated = collection.copy(ziyaratIds = currentZiyaratIds.joinToString(","))
                dao.insertCollection(updated)
            }
        }
    }

    fun toggleHajjUmrahItem(type: String, stepId: Int, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertHajjUmrahItem(HajjUmrahChecklistItem(type, stepId, isCompleted))
        }
    }

    fun saveZiyaratJournal(siteId: String, visitDate: String, notes: String, duaRequests: String) {
        viewModelScope.launch {
            val record = ZiyaratJournalRecord(
                siteId = siteId,
                visitDate = visitDate,
                notes = notes,
                duaRequests = duaRequests,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertZiyaratJournal(record)
        }
    }

    fun toggleZiyaratVisit(siteId: String, type: String) {
        viewModelScope.launch {
            val currentRecord = allZiyaratVisits.value.find { it.siteId == siteId }
            val updatedRecord = when (type.lowercase()) {
                "virtual" -> {
                    val currentVirt = currentRecord?.virtuallyVisited ?: false
                    (currentRecord ?: ZiyaratVisitRecord(siteId)).copy(
                        virtuallyVisited = !currentVirt,
                        visitTimestamp = System.currentTimeMillis()
                    )
                }
                "physical" -> {
                    val currentPhys = currentRecord?.physicallyVisited ?: false
                    (currentRecord ?: ZiyaratVisitRecord(siteId)).copy(
                        physicallyVisited = !currentPhys,
                        visitTimestamp = System.currentTimeMillis()
                    )
                }
                else -> null
            }
            if (updatedRecord != null) {
                dao.insertZiyaratVisit(updatedRecord)
            }
        }
    }

    data class LocationPreset(
        val city: String,
        val lat: Double,
        val lon: Double,
        val timezone: Double,
        val timeZoneId: String? = null,
        val countryCode: String? = null
    ) {
        fun resolvedTimeZone(): TimeZone = LocationTimeZone.resolve(timeZoneId, timezone)

        fun hasValidCoordinates(): Boolean =
            lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0 &&
                timezone.isFinite() && Math.abs(timezone) <= 18.0
    }

    val locations = listOf(
        LocationPreset("Dearborn, USA", 42.3223, -83.1763, -4.0, "America/Detroit", "US"),
        LocationPreset("Najaf, Iraq", 31.9961, 44.3524, 3.0, "Asia/Baghdad", "IQ"),
        LocationPreset("Karbala, Iraq", 32.6160, 44.0248, 3.0, "Asia/Baghdad", "IQ"),
        LocationPreset("Madinah, Saudi Arabia", 24.4672, 39.6024, 3.0, "Asia/Riyadh", "SA"),
        LocationPreset("Tehran, Iran", 35.6892, 51.3890, 3.5, "Asia/Tehran", "IR"),
        LocationPreset("London, UK", 51.5074, -0.1278, 1.0, "Europe/London", "GB"),
        LocationPreset("Toronto, Canada", 43.6532, -79.3832, -4.0, "America/Toronto", "CA"),
        LocationPreset("Karachi, Pakistan", 24.8607, 67.0011, 5.0, "Asia/Karachi", "PK"),
        LocationPreset("Mumbai, India", 19.0760, 72.8777, 5.5, "Asia/Kolkata", "IN"),
        LocationPreset("Sydney, Australia", -33.8688, 151.2093, 10.0, "Australia/Sydney", "AU")
    )

    // Persistent Settings
    private val prefs = application.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE)

    private val _isLocationConfirmed = MutableStateFlow(prefs.getBoolean("is_location_confirmed", false))
    val isLocationConfirmed = _isLocationConfirmed.asStateFlow()

    private val _selectedLocation = MutableStateFlow(
        run {
            val savedCity = prefs.getString("selected_location_city", null)
            if (savedCity != null) {
                val savedLat = prefs.getFloat("selected_location_lat", 0.0f).toDouble()
                val savedLon = prefs.getFloat("selected_location_lon", 0.0f).toDouble()
                val savedTz = prefs.getFloat("selected_location_tz", 0.0f).toDouble()
                val matchingPreset = locations.firstOrNull { it.city == savedCity }
                val candidate = LocationPreset(
                    city = savedCity,
                    lat = savedLat,
                    lon = savedLon,
                    timezone = savedTz,
                    timeZoneId = prefs.getString("selected_location_zone_id", null)
                        ?: matchingPreset?.timeZoneId,
                    countryCode = prefs.getString("selected_location_country_code", null)
                        ?: matchingPreset?.countryCode
                        ?: com.example.utils.CountryLocationResolver.resolve(null, savedCity).takeIf { it.isNotBlank() }
                )
                when {
                    candidate.hasValidCoordinates() -> candidate
                    matchingPreset != null -> matchingPreset
                    else -> {
                        val tzOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
                        locations.minByOrNull { Math.abs(it.timezone - tzOffset) } ?: locations[0]
                    }
                }
            } else {
                val tzOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
                locations.minByOrNull { Math.abs(it.timezone - tzOffset) } ?: locations[0]
            }
        }
    )
    val selectedLocation = _selectedLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError = _locationError.asStateFlow()

    fun clearLocationError() {
        _locationError.value = null
    }

    private val _isAdhanEnabled = MutableStateFlow(prefs.getBoolean("adhan_enabled", false))
    val isAdhanEnabled = _isAdhanEnabled.asStateFlow()

    private val _isFajrAdhanEnabled = MutableStateFlow(prefs.getBoolean("adhan_fajr_enabled", true))
    val isFajrAdhanEnabled = _isFajrAdhanEnabled.asStateFlow()

    private val _isDhuhrAdhanEnabled = MutableStateFlow(prefs.getBoolean("adhan_dhuhr_enabled", true))
    val isDhuhrAdhanEnabled = _isDhuhrAdhanEnabled.asStateFlow()

    private val _isAsrAdhanEnabled = MutableStateFlow(prefs.getBoolean("adhan_asr_enabled", true))
    val isAsrAdhanEnabled = _isAsrAdhanEnabled.asStateFlow()

    private val _isMaghribAdhanEnabled = MutableStateFlow(prefs.getBoolean("adhan_maghrib_enabled", true))
    val isMaghribAdhanEnabled = _isMaghribAdhanEnabled.asStateFlow()

    private val _aamalNotifEnabled = MutableStateFlow(prefs.getBoolean("aamal_notif_enabled", true))
    val aamalNotifEnabled = _aamalNotifEnabled.asStateFlow()

    private val _aamalNotifAuto = MutableStateFlow(prefs.getBoolean("aamal_notif_auto", true))
    val aamalNotifAuto = _aamalNotifAuto.asStateFlow()

    private val _aamalNotifHour = MutableStateFlow(prefs.getInt("aamal_notif_hour", 20))
    val aamalNotifHour = _aamalNotifHour.asStateFlow()

    private val _aamalNotifMinute = MutableStateFlow(prefs.getInt("aamal_notif_minute", 0))
    val aamalNotifMinute = _aamalNotifMinute.asStateFlow()

    private val _isIshaAdhanEnabled = MutableStateFlow(prefs.getBoolean("adhan_isha_enabled", true))
    val isIshaAdhanEnabled = _isIshaAdhanEnabled.asStateFlow()

    fun setPrayerAdhanEnabled(prayer: String, enabled: Boolean) {
        val key = "adhan_${prayer.lowercase()}_enabled"
        prefs.edit().putBoolean(key, enabled).apply()
        when (prayer.lowercase()) {
            "fajr" -> _isFajrAdhanEnabled.value = enabled
            "dhuhr" -> _isDhuhrAdhanEnabled.value = enabled
            "asr" -> _isAsrAdhanEnabled.value = enabled
            "maghrib" -> _isMaghribAdhanEnabled.value = enabled
            "isha" -> _isIshaAdhanEnabled.value = enabled
        }
        if (_isAdhanEnabled.value) {
            com.example.receiver.AdhanScheduler.scheduleSavedAdhanAlarms(getApplication<Application>())
        }
    }

    // App Font Setings
    private val _appFont = MutableStateFlow(prefs.getString("app_font", "system") ?: "system")
    val appFont = _appFont.asStateFlow()
    
    fun setAppFont(font: String) {
        _appFont.value = font
        prefs.edit().putString("app_font", font).apply()
    }

    // App Language State Flow
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private val _autoOccasionTheme = MutableStateFlow(prefs.getBoolean("auto_occasion_theme", true))
    val autoOccasionTheme = _autoOccasionTheme.asStateFlow()

    fun setAutoOccasionTheme(enabled: Boolean) {
        _autoOccasionTheme.value = enabled
        prefs.edit().putBoolean("auto_occasion_theme", enabled).apply()
    }

    private val _themeOverride = MutableStateFlow(prefs.getString("theme_override", "none") ?: "none")
    val themeOverride = _themeOverride.asStateFlow()

    fun setThemeOverride(choice: String) {
        _themeOverride.value = choice
        prefs.edit().putString("theme_override", choice).apply()
    }

    // Adhan Sound Selection State
    private val _adhanSoundKey = MutableStateFlow(prefs.getString("adhan_sound_key", "mesmerizing_shia_azan") ?: "mesmerizing_shia_azan")
    val adhanSoundKey = _adhanSoundKey.asStateFlow()

    fun setAdhanSoundKey(key: String) {
        _adhanSoundKey.value = key
        prefs.edit().putString("adhan_sound_key", key).apply()
    }

    fun playAdhanPreview(context: android.content.Context, soundKey: String) {
        com.example.utils.ShiaAdhanPlayer.play(context, soundKey)
    }

    fun stopAdhanPreview() {
        com.example.utils.ShiaAdhanPlayer.stop()
    }

    private val _appLanguage = MutableStateFlow(prefs.getString("selected_language", "en") ?: "en")
    val appLanguage = _appLanguage.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "$") ?: "$")
    val currencySymbol = _currencySymbol.asStateFlow()

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    
    // --- Finance State ---
    private val _currencyCode = MutableStateFlow(prefs.getString("currency_code", "usd") ?: "usd")
    val currencyCode = _currencyCode.asStateFlow()

    private val _marja = MutableStateFlow(prefs.getString("finance_marja", "Ayatollah Sistani") ?: "Ayatollah Sistani")
    val marja = _marja.asStateFlow()

    private val _autoRefreshPrices = MutableStateFlow(prefs.getBoolean("finance_auto_refresh", true))
    val autoRefreshPrices = _autoRefreshPrices.asStateFlow()

    private val _khumsYearStartDate = MutableStateFlow(prefs.getString("khums_year_start_date", "") ?: "")
    val khumsYearStartDate = _khumsYearStartDate.asStateFlow()

    private val _goldPriceGram = MutableStateFlow<Double?>(null)
    val goldPriceGram = _goldPriceGram.asStateFlow()

    private val _silverPriceGram = MutableStateFlow<Double?>(null)
    val silverPriceGram = _silverPriceGram.asStateFlow()

    private val _financePricesLastUpdated = MutableStateFlow(0L)
    val financePricesLastUpdated = _financePricesLastUpdated.asStateFlow()

    private val _financePricesLoading = MutableStateFlow(false)
    val financePricesLoading = _financePricesLoading.asStateFlow()

    private val _financePricesError = MutableStateFlow<String?>(null)
    val financePricesError = _financePricesError.asStateFlow()

    val allFinanceTransactions: StateFlow<List<com.example.data.FinanceTransaction>> = dao.getAllFinanceTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadCachedFinancePrices()
        if (_autoRefreshPrices.value) {
            refreshFinancePrices()
        }
    }

    fun setCurrencyCode(code: String) {
        _currencyCode.value = code
        prefs.edit().putString("currency_code", code).apply()
        loadCachedFinancePrices()
        if (_autoRefreshPrices.value) {
            refreshFinancePrices()
        }
    }

    fun setMarja(marja: String) {
        _marja.value = marja
        prefs.edit().putString("finance_marja", marja).apply()
    }

    fun setAutoRefreshPrices(autoRefresh: Boolean) {
        _autoRefreshPrices.value = autoRefresh
        prefs.edit().putBoolean("finance_auto_refresh", autoRefresh).apply()
    }

    fun setKhumsYearStartDate(date: String) {
        _khumsYearStartDate.value = date
        prefs.edit().putString("khums_year_start_date", date).apply()
    }

    fun loadCachedFinancePrices() {
        val cached = com.example.data.FinanceRepository.getCachedPrices(_currencyCode.value, prefs)
        _goldPriceGram.value = cached.first
        _silverPriceGram.value = cached.second
        _financePricesLastUpdated.value = prefs.getLong("finance_last_updated_${_currencyCode.value.lowercase()}", 0L)
    }

    fun refreshFinancePrices() {
        viewModelScope.launch {
            _financePricesLoading.value = true
            _financePricesError.value = null
            
            val result = com.example.data.FinanceRepository.fetchLiveGoldSilverPrices(_currencyCode.value, prefs)
            if (result.first != null || result.second != null) {
                _goldPriceGram.value = result.first ?: _goldPriceGram.value
                _silverPriceGram.value = result.second ?: _silverPriceGram.value
                _financePricesLastUpdated.value = System.currentTimeMillis()
            } else {
                _financePricesError.value = "Failed to fetch live prices. Using cached values."
            }
            _financePricesLoading.value = false
        }
    }

    fun clearFinanceCache() {
        prefs.edit().remove("cached_gold_price_${_currencyCode.value.lowercase()}")
             .remove("cached_silver_price_${_currencyCode.value.lowercase()}")
             .remove("finance_last_updated_${_currencyCode.value.lowercase()}")
             .apply()
        _goldPriceGram.value = null
        _silverPriceGram.value = null
        _financePricesLastUpdated.value = 0L
    }

    fun insertFinanceTransaction(transaction: com.example.data.FinanceTransaction) {
        viewModelScope.launch {
            dao.insertFinanceTransaction(transaction)
        }
    }

    fun deleteFinanceTransaction(transaction: com.example.data.FinanceTransaction) {
        viewModelScope.launch {
            dao.deleteFinanceTransaction(transaction)
        }
    }
    // --- End Finance State ---
fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("selected_language", lang).apply()
    }

    private val _arabicUrduFontScale = MutableStateFlow(prefs.getFloat("arabic_urdu_font_scale", 1.0f))
    val arabicUrduFontScale = _arabicUrduFontScale.asStateFlow()

    fun setArabicUrduFontScale(scale: Float) {
        _arabicUrduFontScale.value = scale
        prefs.edit().putFloat("arabic_urdu_font_scale", scale).apply()
    }

    // Guidance on Startup
    private val _showGuidanceOnStartup = MutableStateFlow(prefs.getBoolean("show_guidance_on_startup", true))
    val showGuidanceOnStartup = _showGuidanceOnStartup.asStateFlow()

    fun setShowGuidanceOnStartup(show: Boolean) {
        _showGuidanceOnStartup.value = show
        prefs.edit().putBoolean("show_guidance_on_startup", show).apply()
    }

    // Time Format (12H / 24H)
    private val _use24HourFormat = MutableStateFlow(prefs.getBoolean("use_24_hour_format", false))
    val use24HourFormat = _use24HourFormat.asStateFlow()

    fun setUse24HourFormat(use24H: Boolean) {
        _use24HourFormat.value = use24H
        prefs.edit().putBoolean("use_24_hour_format", use24H).apply()
    }

    // Show Calendar in Mobile Notification Bar (Persistent System Notification Bar)
    private val _showCalendarInNotificationBar = MutableStateFlow(
        prefs.getBoolean("show_calendar_in_notification_bar", prefs.getBoolean("show_calendar_in_toolbar", false))
    )
    val showCalendarInNotificationBar = _showCalendarInNotificationBar.asStateFlow()

    fun setShowCalendarInNotificationBar(enabled: Boolean) {
        _showCalendarInNotificationBar.value = enabled
        _showCalendarInToolbar.value = false
        prefs.edit()
            .putBoolean("show_calendar_in_notification_bar", enabled)
            .putBoolean("show_calendar_in_toolbar", false)
            .apply()

        val context = getApplication<Application>()
        if (enabled) {
            com.example.receiver.CalendarNotificationManager.showOrUpdateCalendarNotification(context)
        } else {
            com.example.receiver.CalendarNotificationManager.cancelCalendarNotification(context)
        }
    }

    // In-app toolbar is disabled as user requested the calendar in the mobile's notification bar
    private val _showCalendarInToolbar = MutableStateFlow(false)
    val showCalendarInToolbar = _showCalendarInToolbar.asStateFlow()

    fun setShowCalendarInToolbar(enabled: Boolean) {
        setShowCalendarInNotificationBar(enabled)
    }

    // Guidance Language (Urdu or English) on Startup
    private val _guidanceLanguage = MutableStateFlow(prefs.getString("guidance_language", "en") ?: "en")
    val guidanceLanguage = _guidanceLanguage.asStateFlow()

    fun setGuidanceLanguage(lang: String) {
        _guidanceLanguage.value = lang
        prefs.edit().putString("guidance_language", lang).apply()
    }

    // Firebase states
    private val _hasSeenOnboarding = MutableStateFlow(prefs.getBoolean("has_seen_onboarding", false))
    val hasSeenOnboarding = _hasSeenOnboarding.asStateFlow()

    fun completeOnboarding() {
        _hasSeenOnboarding.value = true
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
    }

    // App launch counter tracker for rating dialog
    private val _showRateUsPrompt = MutableStateFlow(false)
    val showRateUsPrompt = _showRateUsPrompt.asStateFlow()

    fun dismissRateUsPrompt(disablePermanently: Boolean) {
        _showRateUsPrompt.value = false
        if (disablePermanently) {
            prefs.edit().putBoolean("has_rated_or_dismissed_rate_prompt", true).apply()
        }
    }

    fun resetLaunchCounter() {
        prefs.edit().putInt("app_launch_count", 0).apply()
        _showRateUsPrompt.value = false
    }

    private val auth: com.google.firebase.auth.FirebaseAuth? by lazy {
        try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (t: Throwable) { null }
    }
    private val database: com.google.firebase.database.FirebaseDatabase? by lazy {
        try { com.google.firebase.database.FirebaseDatabase.getInstance("https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app") } catch (t: Throwable) { null }
    }

    private val _isUserLoggedIn = MutableStateFlow(prefs.getBoolean("google_user_logged_in", auth?.currentUser != null))
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("google_user_email", auth?.currentUser?.email))
    val userEmail = _userEmail.asStateFlow()

    private val _userDisplayName = MutableStateFlow(prefs.getString("google_user_name", auth?.currentUser?.displayName ?: auth?.currentUser?.email?.substringBefore("@")))
    val userDisplayName = _userDisplayName.asStateFlow()

    private val _syncStatus = MutableStateFlow("Logged out (Offline)")
    val syncStatus = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate.asStateFlow()

    private val _homeClockMillis = MutableStateFlow(System.currentTimeMillis())
    val homeClockMillis = _homeClockMillis.asStateFlow()
    private var selectedDateFollowsToday = true

    private data class HomeTimelineSnapshot(
        val location: LocationPreset,
        val selectedDate: PrayerTimeCalculator.PrayerDate,
        val nowMillis: Long,
        val timelines: HomePrayerTimelines
    )

    private data class HomeTransitionSnapshot(
        val timelineSnapshot: HomeTimelineSnapshot,
        val nowMillis: Long,
        val hijriOffset: Int,
        val isPastMaghrib: Boolean
    )

    private val homeTimelineSnapshots = combine(
        _selectedLocation,
        _selectedDate,
        _homeClockMillis
    ) { location, selectedCalendar, nowMillis ->
        val timeZone = location.resolvedTimeZone()
        val selectedDate = PrayerTimeCalculator.PrayerDate.fromCalendar(selectedCalendar)
        val currentLocationDate = PrayerTimeCalculator.PrayerDate.fromCalendar(
            Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        )
        HomeTimelineSnapshot(
            location = location,
            selectedDate = selectedDate,
            nowMillis = nowMillis,
            timelines = HomeDayContextFactory.calculateTimelines(
                selectedDate = selectedDate,
                currentLocationDate = currentLocationDate,
                latitude = location.lat,
                longitude = location.lon,
                timeZone = timeZone
            )
        )
    }.distinctUntilChanged { old, new ->
        old.location == new.location &&
            old.selectedDate == new.selectedDate &&
            old.timelines == new.timelines
    }

    private val homeTransitions = combine(
        homeTimelineSnapshots,
        _homeClockMillis,
        snapshotFlow { com.example.utils.HijriCalendarHelper.globalOffsetDays }
    ) { timelineSnapshot, nowMillis, hijriOffset ->
        HomeTransitionSnapshot(
            timelineSnapshot = timelineSnapshot,
            nowMillis = nowMillis,
            hijriOffset = hijriOffset,
            isPastMaghrib = timelineSnapshot.selectedDate == timelineSnapshot.timelines.current.date &&
                nowMillis >= timelineSnapshot.timelines.selected.instant(
                    PrayerTimeCalculator.PrayerEvent.MAGHRIB
                )
        )
    }.distinctUntilChanged { old, new ->
        old.timelineSnapshot.location == new.timelineSnapshot.location &&
            old.timelineSnapshot.selectedDate == new.timelineSnapshot.selectedDate &&
            old.timelineSnapshot.timelines == new.timelineSnapshot.timelines &&
            old.hijriOffset == new.hijriOffset &&
            old.isPastMaghrib == new.isPastMaghrib
    }

    val homeDayContext: StateFlow<HomeDayContext> = homeTransitions.map { transition ->
        val snapshot = transition.timelineSnapshot
        createHomeDayContext(
            nowMillis = transition.nowMillis,
            hijriOffset = transition.hijriOffset,
            timelines = snapshot.timelines,
            location = snapshot.location,
            selectedDate = snapshot.selectedDate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createHomeDayContext(
            System.currentTimeMillis(),
            com.example.utils.HijriCalendarHelper.globalOffsetDays
        )
    )

    private fun createHomeDayContext(
        nowMillis: Long,
        hijriOffset: Int,
        timelines: HomePrayerTimelines? = null,
        location: LocationPreset = _selectedLocation.value,
        selectedDate: PrayerTimeCalculator.PrayerDate =
            PrayerTimeCalculator.PrayerDate.fromCalendar(_selectedDate.value)
    ): HomeDayContext {
        return HomeDayContextFactory.create(
            nowMillis = nowMillis,
            selectedDate = selectedDate,
            latitude = location.lat,
            longitude = location.lon,
            timeZone = location.resolvedTimeZone(),
            hijriOffset = hijriOffset,
            locationKey = location.city,
            countryCode = location.countryCode,
            timelines = timelines
        )
    }

    /** Advances a followed "today" at the selected location's midnight, not the device's. */
    fun refreshHomeClock(nowMillis: Long = System.currentTimeMillis()) {
        if (selectedDateFollowsToday) {
            val current = Calendar.getInstance(_selectedLocation.value.resolvedTimeZone()).apply {
                timeInMillis = nowMillis
            }
            val currentDate = PrayerTimeCalculator.PrayerDate.fromCalendar(current)
            val selected = PrayerTimeCalculator.PrayerDate.fromCalendar(_selectedDate.value)
            if (currentDate != selected) {
                _selectedDate.value = current
            }
        }
        _homeClockMillis.value = nowMillis
    }

    val formattedDate: StateFlow<String> = _selectedDate.map { cal ->
        PrayerTimeCalculator.PrayerDate.fromCalendar(cal).key
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PrayerTimeCalculator.PrayerDate.fromCalendar(Calendar.getInstance()).key
    )

    val displayDateStr: StateFlow<String> = _selectedDate.map { cal ->
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        sdf.timeZone = cal.timeZone
        sdf.format(cal.time)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Daily Logs Flow
    val todayLogs: StateFlow<List<PrayerLog>> = formattedDate.flatMapLatest { dateStr ->
        dao.getPrayerLogsForDate(dateStr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Qada tallies Flow
    val qadaTallies: StateFlow<List<QadaTally>> = dao.getAllQadaTallies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPrayerLogs: StateFlow<List<PrayerLog>> = dao.getAllPrayerLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorited Quotes Flow
    val favoriteQuotes: StateFlow<List<FavoriteQuote>> = dao.getFavoriteQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // Gemini Dynamic Hadees States
    private val _dailyQuote = MutableStateFlow<ShiaQuote?>(null)
    val dailyQuote = _dailyQuote.asStateFlow()

    private val _isGeneratingQuote = MutableStateFlow(false)
    val isGeneratingQuote = _isGeneratingQuote.asStateFlow()

    // Ayat of the Day States
    private val _dailyAyat = MutableStateFlow<ShiaQuote?>(null)
    val dailyAyat = _dailyAyat.asStateFlow()

    private val _isGeneratingAyat = MutableStateFlow(false)
    val isGeneratingAyat = _isGeneratingAyat.asStateFlow()

    // Tasbeeh States
    val tasbeehPresets = listOf("Tasbih Lady Fatima (sa)", "Salawat (100x)", "Istighfar (100x)", "Free Mode")
    private val _selectedTasbeeh = MutableStateFlow(tasbeehPresets[0])
    val selectedTasbeeh = _selectedTasbeeh.asStateFlow()

    private val _tasbeehCount = MutableStateFlow(0)
    val tasbeehCount = _tasbeehCount.asStateFlow()

    private val _tasbeehCycle = MutableStateFlow(0) // 0: Allahu Akbar, 1: Alhamdulillah, 2: SubhanAllah
    val tasbeehCycle = _tasbeehCycle.asStateFlow()
    private val tasbeehWriteMutex = Mutex()

    init {
        val launches = prefs.getInt("app_launch_count", 0)
        val ratedOrDismissed = prefs.getBoolean("has_rated_or_dismissed_rate_prompt", false)
        val newCount = launches + 1
        prefs.edit().putInt("app_launch_count", newCount).apply()
        if (newCount >= 5 && !ratedOrDismissed) {
            _showRateUsPrompt.value = true
        }

        if (_isUserLoggedIn.value) {
            try {
                auth?.signInAnonymously()
            } catch (e: Exception) {}
        }

        viewModelScope.launch {
            dao.getAllQadaTallies().first().let { tallies ->
                if (tallies.isEmpty()) {
                    listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
                        dao.insertQadaTally(QadaTally(name, 0))
                    }
                }
            }
            dao.getAllZiyarats().first().let { ziyaratList ->
                if (ziyaratList.isEmpty()) {
                    dao.insertZiyarats(getInitialZiyarats())
                } else {
                    val needsUpdate = ziyaratList.filter { it.audioUrl.isNotBlank() }
                    if (needsUpdate.isNotEmpty()) {
                        needsUpdate.forEach { 
                            dao.insertZiyarat(it.copy(audioUrl = ""))
                        }
                    }
                }
            }
        }
    }

    private fun fetchCurrentLocation(context: android.content.Context) {
        try {
            val fineGranted = androidx.core.app.ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarseGranted = androidx.core.app.ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val accuracy = resolveDeviceLocationAccuracy(fineGranted, coarseGranted)
            if (accuracy == null) {
                _locationError.value = "Location permission is required to detect your current city. You can still enter coordinates manually."
                return
            }

            val fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(context)
            val priority = if (accuracy == DeviceLocationAccuracy.HIGH_ACCURACY) {
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            } else {
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }
            fusedLocationClient.getCurrentLocation(priority, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        persistDeviceLocation(context, location)
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLocation ->
                                if (lastLocation != null) {
                                    persistDeviceLocation(context, lastLocation)
                                } else {
                                    _locationError.value = "Your current location is unavailable. Try again or enter coordinates manually."
                                }
                            }
                            .addOnFailureListener {
                                _locationError.value = "Your current location is unavailable. Try again or enter coordinates manually."
                            }
                    }
                }
                .addOnFailureListener {
                    _locationError.value = "Unable to detect your location. Enter coordinates manually or try again."
                }
        } catch (error: SecurityException) {
            _locationError.value = "Location permission is required. You can still enter coordinates manually."
        } catch (error: Throwable) {
            android.util.Log.e("PrayerViewModel", "Error fetching current location", error)
            _locationError.value = "Unable to detect your location. Enter coordinates manually or try again."
        }
    }

    private fun persistDeviceLocation(context: android.content.Context, location: android.location.Location) {
        viewModelScope.launch(Dispatchers.IO) {
            var city = "Current Location"
            var countryCode: String? = null
            try {
                val address = android.location.Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                city = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: city
                countryCode = address?.countryCode
            } catch (error: Throwable) {
                android.util.Log.w("PrayerViewModel", "Reverse geocoding unavailable", error)
            }
            val timeZone = TimeZone.getDefault()
            setCustomLocation(
                city = city,
                lat = location.latitude,
                lon = location.longitude,
                tz = timeZone.getOffset(System.currentTimeMillis()) / 3_600_000.0,
                context = context,
                timeZoneId = timeZone.id,
                countryCode = countryCode
            )
        }
    }

    fun requestExactLocation(context: android.content.Context) {
        fetchCurrentLocation(context)
    }

    fun setLocation(preset: LocationPreset, context: android.content.Context? = null) {
        if (!preset.hasValidCoordinates()) {
            _locationError.value = "Select a location with valid coordinates and timezone."
            return
        }
        com.example.utils.HijriConfigurationGeneration.invalidate()
        _selectedLocation.value = preset
        prefs.edit()
            .putString("selected_location_city", preset.city)
            .putFloat("selected_location_lat", preset.lat.toFloat())
            .putFloat("selected_location_lon", preset.lon.toFloat())
            .putFloat("selected_location_tz", preset.timezone.toFloat())
            .putString("selected_location_zone_id", preset.timeZoneId)
            .putString("selected_location_country_code", preset.countryCode)
            .putBoolean("is_location_confirmed", true)
            .apply()
        _isLocationConfirmed.value = true
        _locationError.value = null
        onLocationChanged(context ?: getApplication<Application>())
    }

    fun setCustomLocation(
        city: String,
        lat: Double,
        lon: Double,
        tz: Double,
        context: android.content.Context? = null,
        timeZoneId: String? = null,
        countryCode: String? = null
    ) {
        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _locationError.value = "Enter a valid latitude (-90 to 90) and longitude (-180 to 180)."
            return
        }
        if (!tz.isFinite() || Math.abs(tz) > 18.0) {
            _locationError.value = "Enter a valid fixed timezone offset between -18 and +18 hours."
            return
        }
        com.example.utils.HijriConfigurationGeneration.invalidate()
        val location = LocationPreset(
            city = city.trim().ifBlank { "Custom Location" },
            lat = lat,
            lon = lon,
            timezone = tz,
            timeZoneId = timeZoneId?.takeIf { LocationTimeZone.isValidId(it) },
            countryCode = com.example.utils.CountryLocationResolver.validatedCode(countryCode)
                ?: com.example.utils.CountryLocationResolver.resolve(null, city).takeIf { it.isNotBlank() }
        )
        _selectedLocation.value = location
        prefs.edit()
            .putString("selected_location_city", location.city)
            .putFloat("selected_location_lat", lat.toFloat())
            .putFloat("selected_location_lon", lon.toFloat())
            .putFloat("selected_location_tz", tz.toFloat())
            .putString("selected_location_zone_id", location.timeZoneId)
            .putString("selected_location_country_code", location.countryCode)
            .putBoolean("is_location_confirmed", true)
            .apply()
        _isLocationConfirmed.value = true
        _locationError.value = null
        onLocationChanged(context ?: getApplication<Application>())
    }

    private fun onLocationChanged(context: android.content.Context) {
        refreshHomeClock()
        IslamicEventManager.onLocationContextChanged()
        if (_isAdhanEnabled.value) {
            com.example.receiver.AdhanScheduler.scheduleSavedAdhanAlarms(context)
        }
        if (_aamalNotifEnabled.value) {
            com.example.receiver.AamalEventScheduler.scheduleNext(context)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val countryService = com.example.utils.DefaultCountryDetectionService(context)
                val remoteConfigService = com.example.utils.DefaultRemoteConfigurationService(context)
                com.example.utils.DefaultHijriConfigurationService(
                    countryService,
                    remoteConfigService
                ).resolveAndApplyOffset(context)
            } catch (error: Exception) {
                // Existing cached/default configuration remains available offline.
                android.util.Log.w("PrayerViewModel", "Hijri refresh used existing configuration", error)
            }
        }
    }

    fun setAdhanEnabled(enabled: Boolean, context: android.content.Context) {
        _isAdhanEnabled.value = enabled
        prefs.edit().putBoolean("adhan_enabled", enabled).apply()
        if (enabled) {
            val scheduled = com.example.receiver.AdhanScheduler.scheduleSavedAdhanAlarms(context)
            if (!scheduled) {
                _locationError.value = "Select a valid location before enabling Adhan reminders."
            }
        } else {
            com.example.receiver.AdhanScheduler.cancelAllAdhanAlarms(context)
        }
    }

    fun setAamalNotifEnabled(enabled: Boolean, context: android.content.Context): Boolean {
        val result = com.example.receiver.AamalEventScheduler.setEnabled(context, enabled)
        // Publish the actual persisted state, never the requested value after a failed commit.
        _aamalNotifEnabled.value = com.example.receiver.AamalEventScheduler.currentEnabled(context)
        if (!result.persisted) {
            // Dashboard already surfaces this existing settings-error event as a Toast.
            _locationError.value = "Unable to save Aamal reminder setting. Please try again."
        }
        return result.persisted
    }

    fun setAamalNotifAutoOrTime(isAuto: Boolean, hour: Int, minute: Int, context: android.content.Context) {
        _aamalNotifAuto.value = isAuto
        _aamalNotifHour.value = hour
        _aamalNotifMinute.value = minute
        prefs.edit()
            .putBoolean("aamal_notif_auto", isAuto)
            .putInt("aamal_notif_hour", hour)
            .putInt("aamal_notif_minute", minute)
            .apply()
        
        if (_aamalNotifEnabled.value) {
            com.example.receiver.AamalEventScheduler.scheduleNext(context)
        }
    }

    fun loginWithGoogle(email: String, name: String, idToken: String? = null) {
        _isUserLoggedIn.value = true
        _userEmail.value = email
        _userDisplayName.value = name
        _syncStatus.value = "Connected via Google Identity"
        prefs.edit()
            .putBoolean("google_user_logged_in", true)
            .putString("google_user_email", email)
            .putString("google_user_name", name)
            .apply()
        
        if (idToken != null && auth != null) {
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        restoreDataFromFirebase()
                    } else {
                        _syncStatus.value = "Firebase Auth Error: ${task.exception?.localizedMessage}"
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = "Firebase Auth Exception: ${e.localizedMessage}"
            }
        } else {
            try {
                auth?.signInAnonymously()?.addOnCompleteListener { task ->
                    restoreDataFromFirebase()
                } ?: run {
                    restoreDataFromFirebase()
                }
            } catch (e: Exception) {
                restoreDataFromFirebase()
            }
        }
    }

    fun logout() {
        _isUserLoggedIn.value = false
        _userEmail.value = null
        _userDisplayName.value = null
        _syncStatus.value = "Logged out (Offline)"
        prefs.edit()
            .putBoolean("google_user_logged_in", false)
            .putString("google_user_email", null)
            .putString("google_user_name", null)
            .apply()
    }

    fun backupDataToFirebase() {
        if (!_isUserLoggedIn.value) {
            _syncStatus.value = "Error: Please sign in first"
            return
        }
        val db = database
        if (db == null) {
            _syncStatus.value = "Sync Error: Realtime Database not initialized"
            return
        }
        _isSyncing.value = true
        _syncStatus.value = "Connecting to Firebase..."
        
        viewModelScope.launch {
            try {
                val currentLogs = dao.getAllPrayerLogs().first()
                val currentTallies = dao.getAllQadaTallies().first()
                
                val userId = auth?.currentUser?.uid
                if (userId == null) {
                    _syncStatus.value = "Sync Error: Not authenticated"
                    _isSyncing.value = false
                    return@launch
                }
                val userRef = db.getReference("users").child(userId)
                
                val updates = mapOf<String, Any>(
                    "logs" to currentLogs.map { mapOf("date" to it.dateString, "name" to it.prayerName, "status" to it.status) },
                    "tallies" to currentTallies.map { mapOf("name" to it.prayerName, "count" to it.count) }
                )
                
                userRef.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _syncStatus.value = "Synced! Saved ${currentLogs.size} logs & ${currentTallies.size} Qada tallies to Firebase."
                    } else {
                        _syncStatus.value = "Sync Error: ${task.exception?.localizedMessage ?: "Permission Denied or Unknown error"}"
                    }
                    _isSyncing.value = false
                }
            } catch (e: Exception) {
                _syncStatus.value = "Sync Error: ${e.localizedMessage ?: "Failed to backup data"}"
                _isSyncing.value = false
            }
        }
    }

    fun restoreDataFromFirebase() {
        if (!_isUserLoggedIn.value) {
            _syncStatus.value = "Error: Please sign in first"
            return
        }
        val db = database
        if (db == null) {
            _syncStatus.value = "Restore Error: Realtime Database not initialized"
            return
        }
        _isSyncing.value = true
        _syncStatus.value = "Restoring cloud databases..."
        
        val userId = auth?.currentUser?.uid
        if (userId == null) {
            _syncStatus.value = "Restore Error: Not authenticated"
            _isSyncing.value = false
            return
        }
        val userRef = db.getReference("users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                _syncStatus.value = "No backup found in cloud for this account."
                _isSyncing.value = false
                return@addOnSuccessListener
            }
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val logsSnapshot = snapshot.child("logs").children
                    var logsCount = 0
                    for (logSnap in logsSnapshot) {
                        val date = logSnap.child("date").getValue(String::class.java) ?: continue
                        val name = logSnap.child("name").getValue(String::class.java) ?: continue
                        val status = logSnap.child("status").getValue(String::class.java) ?: continue
                        dao.insertPrayerLog(com.example.data.PrayerLog(dateString = date, prayerName = name, status = status))
                        logsCount++
                    }
                    
                    val talliesSnapshot = snapshot.child("tallies").children
                    var talliesCount = 0
                    for (tallySnap in talliesSnapshot) {
                        val name = tallySnap.child("name").getValue(String::class.java) ?: continue
                        val count = tallySnap.child("count").getValue(Int::class.java) ?: continue
                        val existing = dao.getAllQadaTallies().first().firstOrNull { it.prayerName == name }
                        if (existing != null) {
                            dao.updateQadaTally(name, maxOf(existing.count, count))
                        } else {
                            dao.insertQadaTally(com.example.data.QadaTally(name, count))
                        }
                        talliesCount++
                    }
                    
                    _syncStatus.value = "Successfully restored $logsCount logs & $talliesCount Qada tallies."
                } catch (e: Exception) {
                    _syncStatus.value = "Restored locally (Firebase error: ${e.localizedMessage})."
                } finally {
                    _isSyncing.value = false
                }
            }
        }.addOnFailureListener { e ->
            _syncStatus.value = "Failed to restore: ${e.localizedMessage}"
            _isSyncing.value = false
        }
    }

    fun changeDate(offset: Int) {
        val newCal = _selectedDate.value.clone() as Calendar
        newCal.add(Calendar.DAY_OF_YEAR, offset)
        selectDate(newCal)
    }

    fun selectDate(calendar: Calendar) {
        _selectedDate.value = calendar
        val current = Calendar.getInstance(_selectedLocation.value.resolvedTimeZone())
        selectedDateFollowsToday = PrayerTimeCalculator.PrayerDate.fromCalendar(calendar) ==
            PrayerTimeCalculator.PrayerDate.fromCalendar(current)
        refreshHomeClock()
    }

    fun updatePrayerLog(prayerName: String, status: String) {
        viewModelScope.launch {
            val dateStr = formattedDate.value
            val logs = dao.getPrayerLogsForDate(dateStr).first()
            val existing = logs.firstOrNull { it.prayerName == prayerName }
            if (existing != null) {
                dao.insertPrayerLog(existing.copy(status = status))
            } else {
                dao.insertPrayerLog(PrayerLog(dateString = dateStr, prayerName = prayerName, status = status))
            }
            if (_isUserLoggedIn.value) {
                backupDataToFirebase()
            }
        }
    }

    fun modifyQadaCount(prayerName: String, delta: Int) {
        viewModelScope.launch {
            val tallies = dao.getAllQadaTallies().first()
            val existing = tallies.firstOrNull { it.prayerName == prayerName }
            if (existing != null) {
                val newCount = (existing.count + delta).coerceAtLeast(0)
                dao.updateQadaTally(prayerName, newCount)
            } else {
                val newCount = delta.coerceAtLeast(0)
                dao.insertQadaTally(QadaTally(prayerName, newCount))
            }
            if (_isUserLoggedIn.value) {
                backupDataToFirebase()
            }
        }
    }

    fun incrementAllQada() {
        viewModelScope.launch {
            val tallies = dao.getAllQadaTallies().first()
            tallies.forEach {
                dao.updateQadaTally(it.prayerName, it.count + 1)
            }
            if (_isUserLoggedIn.value) {
                backupDataToFirebase()
            }
        }
    }

    fun resetAllQada() {
        viewModelScope.launch {
            val tallies = dao.getAllQadaTallies().first()
            tallies.forEach {
                dao.updateQadaTally(it.prayerName, 0)
            }
            if (_isUserLoggedIn.value) {
                backupDataToFirebase()
            }
        }
    }

    private var currentDailyTopic = "daily"
    private var currentDailyMasoomIndex = 0
    private var currentDailyDayStamp: Int? = null
    private var dailyContentKey: String? = null
    private var dailyQuoteRequestId = 0L
    private var dailyAyatRequestId = 0L

    init {
        refreshDailyContentIfNeeded()
    }

    fun updateDailyContentTopic(topic: String, masoomIndex: Int = 0, dayStamp: Int? = null) {
        currentDailyTopic = topic.trim().ifBlank { "daily" }
        currentDailyMasoomIndex = masoomIndex.coerceIn(0, 14)
        currentDailyDayStamp = dayStamp
        refreshDailyContentIfNeeded()
    }

    fun refreshDailyContent() {
        dailyContentKey = null
        loadDailyQuote(currentDailyTopic)
        loadDailyAyat(currentDailyTopic)
    }

    fun refreshDailyContentIfNeeded() {
        val calendar = java.util.Calendar.getInstance()
        val actualDayStamp = calendar.get(java.util.Calendar.YEAR) * 1000 + calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val dayStamp = currentDailyDayStamp ?: actualDayStamp
        val newContentKey = "$dayStamp:$currentDailyTopic:$currentDailyMasoomIndex"
        if (newContentKey != dailyContentKey) {
            dailyContentKey = newContentKey
            loadDailyQuote(currentDailyTopic)
            loadDailyAyat(currentDailyTopic)
        }
    }

    fun loadDailyQuote(topic: String? = null) {
        val requestId = ++dailyQuoteRequestId
        val masoomIndex = currentDailyMasoomIndex
        viewModelScope.launch {
            _isGeneratingQuote.value = true
            try {
                val quote = DailyContentClient.getDailyShiaQuote(
                    getApplication<Application>(),
                    topic,
                    masoomIndex,
                    currentDailyDayStamp
                )
                if (requestId == dailyQuoteRequestId) _dailyQuote.value = quote
            } finally {
                if (requestId == dailyQuoteRequestId) _isGeneratingQuote.value = false
            }
        }
    }

    fun loadDailyAyat(topic: String? = null) {
        val requestId = ++dailyAyatRequestId
        viewModelScope.launch {
            _isGeneratingAyat.value = true
            try {
                val ayat = DailyContentClient.getDailyQuranAyat(
                    getApplication<Application>(),
                    topic,
                    currentDailyDayStamp
                )
                if (requestId == dailyAyatRequestId) _dailyAyat.value = ayat
            } finally {
                if (requestId == dailyAyatRequestId) _isGeneratingAyat.value = false
            }
        }
    }

    fun toggleFavoriteQuote() {
        viewModelScope.launch {
            val current = _dailyQuote.value ?: return@launch
            val favs = favoriteQuotes.value
            val existing = favs.firstOrNull { it.english == current.english }
            if (existing != null) {
                dao.deleteFavoriteQuote(existing)
            } else {
                dao.insertFavoriteQuote(
                    FavoriteQuote(
                        arabic = current.arabic,
                        english = current.english,
                        source = current.source
                    )
                )
            }
        }
    }

    fun toggleFavoriteAyat() {
        viewModelScope.launch {
            val current = _dailyAyat.value ?: return@launch
            val favs = favoriteQuotes.value
            val existing = favs.firstOrNull { it.english == current.english }
            if (existing != null) {
                dao.deleteFavoriteQuote(existing)
            } else {
                dao.insertFavoriteQuote(
                    FavoriteQuote(
                        arabic = current.arabic,
                        english = current.english,
                        source = current.source
                    )
                )
            }
        }
    }

    fun toggleFavoriteCustom(arabic: String, english: String, source: String) {
        viewModelScope.launch {
            val favs = favoriteQuotes.value
            val existing = favs.firstOrNull { it.english == english }
            if (existing != null) {
                dao.deleteFavoriteQuote(existing)
            } else {
                dao.insertFavoriteQuote(
                    FavoriteQuote(
                        arabic = arabic,
                        english = english,
                        source = source
                    )
                )
            }
        }
    }

    fun removeFavoriteQuote(quote: FavoriteQuote) {
        viewModelScope.launch {
            dao.deleteFavoriteQuote(quote)
        }
    }

    private val _favoriteDuaIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("favorite_duas", emptySet()) ?: emptySet()
    )
    val favoriteDuaIds = _favoriteDuaIds.asStateFlow()

    private val _duaBookmarks = MutableStateFlow<Map<String, Int>>(
        prefs.getString("dua_bookmarks", "{}")?.let { jsonString ->
            try {
                val jsonObject = org.json.JSONObject(jsonString)
                val map = mutableMapOf<String, Int>()
                jsonObject.keys().forEach { key ->
                    map[key] = jsonObject.getInt(key)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    )
    val duaBookmarks = _duaBookmarks.asStateFlow()

    private val _ziyaratBookmarks = MutableStateFlow<Map<String, Int>>(
        prefs.getString("ziyarat_bookmarks", "{}")?.let { jsonString ->
            try {
                val jsonObject = org.json.JSONObject(jsonString)
                val map = mutableMapOf<String, Int>()
                jsonObject.keys().forEach { key ->
                    map[key] = jsonObject.getInt(key)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    )
    val ziyaratBookmarks = _ziyaratBookmarks.asStateFlow()

    fun setDuaBookmark(duaId: String, lineIndex: Int) {
        val current = _duaBookmarks.value.toMutableMap()
        if (lineIndex < 0) {
            current.remove(duaId)
        } else {
            current[duaId] = lineIndex
        }
        _duaBookmarks.value = current
        val jsonObject = org.json.JSONObject(current as Map<*, *>)
        prefs.edit().putString("dua_bookmarks", jsonObject.toString()).apply()
    }

    fun setZiyaratBookmark(ziyaratId: String, index: Int) {
        val current = _ziyaratBookmarks.value.toMutableMap()
        if (index < 0) {
            current.remove(ziyaratId)
        } else {
            current[ziyaratId] = index
        }
        _ziyaratBookmarks.value = current
        val jsonObject = org.json.JSONObject(current as Map<*, *>)
        prefs.edit().putString("ziyarat_bookmarks", jsonObject.toString()).apply()
    }

    fun toggleDuaFavorite(duaId: String) {
        val current = _favoriteDuaIds.value.toMutableSet()
        if (current.contains(duaId)) {
            current.remove(duaId)
        } else {
            current.add(duaId)
        }
        prefs.edit().putStringSet("favorite_duas", current).apply()
        _favoriteDuaIds.value = current
    }

    fun selectTasbeeh(preset: String) {
        _selectedTasbeeh.value = preset
        _tasbeehCount.value = 0
        _tasbeehCycle.value = 0
    }

    fun incrementTasbeeh(onCycleComplete: () -> Unit) {
        val currentPreset = _selectedTasbeeh.value
        val count = _tasbeehCount.value
        val cycle = _tasbeehCycle.value

        if (currentPreset == "Tasbih Lady Fatima (sa)") {
            if (cycle == 0) {
                if (count < 34) {
                    _tasbeehCount.value = count + 1
                    if (_tasbeehCount.value == 34) {
                        onCycleComplete()
                        _tasbeehCycle.value = 1
                        _tasbeehCount.value = 0
                    }
                }
            } else if (cycle == 1) {
                if (count < 33) {
                    _tasbeehCount.value = count + 1
                    if (_tasbeehCount.value == 33) {
                        onCycleComplete()
                        _tasbeehCycle.value = 2
                        _tasbeehCount.value = 0
                    }
                }
            } else if (cycle == 2) {
                if (count < 33) {
                    _tasbeehCount.value = count + 1
                    if (_tasbeehCount.value == 33) {
                        onCycleComplete()
                        _tasbeehCycle.value = 3 // Finished cycle
                    }
                }
            }
        } else if (currentPreset == "Salawat (100x)" || currentPreset == "Istighfar (100x)") {
            if (count < 100) {
                _tasbeehCount.value = count + 1
                if (_tasbeehCount.value == 100) {
                    onCycleComplete()
                }
            }
        } else {
            _tasbeehCount.value = count + 1
        }
        
        // Serialize read/increment/write so rapid taps cannot overwrite each other.
        if (count != _tasbeehCount.value) {
            viewModelScope.launch(Dispatchers.IO) {
                tasbeehWriteMutex.withLock {
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val existing = dao.getTasbeehRecordForDate(dateStr)
                    val currentTotal = existing?.totalCount ?: 0
                    dao.insertTasbeehRecord(com.example.data.TasbeehDailyRecord(dateStr, currentTotal + 1))
                }
            }
        }
    }

    fun resetTasbeeh() {
        _tasbeehCount.value = 0
        _tasbeehCycle.value = 0
    }

    fun decrementTasbeeh() {
        if (_tasbeehCount.value > 0) {
            _tasbeehCount.value = _tasbeehCount.value - 1
        }
    }

    val tasbeehRecords: StateFlow<List<com.example.data.TasbeehDailyRecord>> = dao.getAllTasbeehRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ramadanRecords: StateFlow<List<RamadanDayRecord>> = dao.getAllRamadanRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateRamadanRecord(record: RamadanDayRecord) {
        viewModelScope.launch {
            dao.insertRamadanRecord(record)
        }
    }

    fun toggleZiyaratFavorite(item: ZiyaratItem) {
        viewModelScope.launch {
            dao.insertZiyarat(item.copy(isFavorite = !item.isFavorite))
        }
    }

    // Aamal-e-Rozana ViewModel integration
    val allCustomAamal: StateFlow<List<AamalCustomActivity>> = dao.getAllCustomAamal()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayAamalCompletions: StateFlow<List<AamalCompletion>> = formattedDate.flatMapLatest { dateStr ->
        dao.getAamalCompletionsForDate(dateStr)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAamalCompletions: StateFlow<List<AamalCompletion>> = dao.getAllAamalCompletions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleAamalCompletion(activityId: String, isCompleted: Boolean, dateStr: String, count: Int = 0) {
        viewModelScope.launch {
            val record = AamalCompletion(
                dateString = dateStr,
                activityId = activityId,
                isCompleted = isCompleted,
                completionCount = count
            )
            dao.insertAamalCompletion(record)
        }
    }

    fun addCustomAamal(title: String, category: String, dateStr: String) {
        viewModelScope.launch {
            val id = "custom_" + java.util.UUID.randomUUID().toString()
            val activity = AamalCustomActivity(
                id = id,
                title = title,
                category = category,
                isEnabled = true,
                dateAdded = dateStr
            )
            dao.insertCustomAamal(activity)
        }
    }

    fun deleteCustomAamal(id: String) {
        viewModelScope.launch {
            // Capture the real title before removing the current-activity row. Completion
            // rows are deliberately untouched, and no metadata tombstone enters totals.
            dao.getCustomAamalById(id)?.let { activity ->
                AamalHistoryMetadataStore.captureBeforeDeletion(
                    getApplication<Application>(),
                    activity
                )
            }
            dao.deleteCustomAamal(id)
        }
    }

    // --- News / Event Shia Ticker States & Methods ---
    private val _isTickerActive = MutableStateFlow(prefs.getBoolean("ticker_active", false))
    val isTickerActive = _isTickerActive.asStateFlow()

    fun setTickerActive(active: Boolean) {
        _isTickerActive.value = active
        prefs.edit().putBoolean("ticker_active", active).apply()
    }

    private val _tickerSpeed = MutableStateFlow(prefs.getString("ticker_speed", "Normal") ?: "Normal")
    val tickerSpeed = _tickerSpeed.asStateFlow()

    fun setTickerSpeed(speed: String) {
        _tickerSpeed.value = speed
        prefs.edit().putString("ticker_speed", speed).apply()
    }

    private val _tickerMutedCategories = MutableStateFlow(
        prefs.getStringSet("ticker_muted_cats", emptySet())?.map { TickerCategory.valueOf(it) }?.toSet() ?: emptySet()
    )
    val tickerMutedCategories = _tickerMutedCategories.asStateFlow()

    fun toggleTickerMuteCategory(category: TickerCategory) {
        val currentMuted = _tickerMutedCategories.value.toMutableSet()
        if (currentMuted.contains(category)) {
            currentMuted.remove(category)
        } else {
            currentMuted.add(category)
        }
        _tickerMutedCategories.value = currentMuted
        prefs.edit().putStringSet("ticker_muted_cats", currentMuted.map { it.name }.toSet()).apply()
    }

    private val _tickerItems = MutableStateFlow<List<TickerItem>>(emptyList())
    val tickerItems = _tickerItems.asStateFlow()

    private val _isRefreshingTicker = MutableStateFlow(false)
    val isRefreshingTicker = _isRefreshingTicker.asStateFlow()

    private val _submittedTickerEvents = MutableStateFlow<List<TickerItem>>(emptyList())
    val submittedTickerEvents = _submittedTickerEvents.asStateFlow()

    init {
        refreshTickerEvents()
    }

    fun submitUserEvent(text: String, category: TickerCategory, title: String, details: String) {
        viewModelScope.launch {
            val id = "user_" + System.currentTimeMillis()
            val newItem = TickerItem(
                id = id,
                text = text,
                category = category,
                fullTitle = title,
                fullDetails = details,
                source = "User Submitted",
                dateString = "Pending Admin Approval",
                isNew = true
            )
            _submittedTickerEvents.value = _submittedTickerEvents.value + newItem
        }
    }

    fun approveSubmittedEvent(id: String) {
        viewModelScope.launch {
            val approvedItem = _submittedTickerEvents.value.find { it.id == id }
            if (approvedItem != null) {
                _submittedTickerEvents.value = _submittedTickerEvents.value.filter { it.id != id }
                val validatedItem = approvedItem.copy(
                    id = "approved_" + approvedItem.id,
                    dateString = "Approved Now",
                    source = "User Contributed (Moderated)"
                )
                _tickerItems.value = listOf(validatedItem) + _tickerItems.value
            }
        }
    }

    fun rejectSubmittedEvent(id: String) {
        viewModelScope.launch {
            _submittedTickerEvents.value = _submittedTickerEvents.value.filter { it.id != id }
        }
    }

    fun deleteTickerItem(id: String) {
        _tickerItems.value = _tickerItems.value.filter { it.id != id }
    }

    fun refreshTickerEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingTicker.value = true
            
            // 1. Load Local Events & Guidance
            val localList = TickerLocalData.getLocalEvents(_appLanguage.value)
            
            _tickerItems.value = localList.distinctBy { it.id }
            _isRefreshingTicker.value = false
        }
    }

    private fun getInitialZiyarats(): List<ZiyaratItem> {
        return listOf(
            ZiyaratItem(
                id = "ziyarat_prophet",
                masoomIndex = 1,
                titleEn = "Ziyarah of Prophet Muhammad (saws)",
                titleAr = "زيارة النبي محمد (ص)",
                titleUr = "زیارت رسول اکرم صلی اللہ علیہ وآلہ وسلم",
                audioUrl = "",
                arText = "أَشْهَدُ أَنْ لا إِلَٰهَ إِلاَّ اللهُ وَحْدَهُ لا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّكَ مُحَمَّدٌ عَبْدُهُ وَرَسُولُهُ، أَرْسَلَكَ بِالْحَقِّ بَشِيراً وَنَذِيراً، وَدَاعِياً إِلَى اللهِ بِإِذْنِهِ وَسِرَاجاً مُنِيراً. أَشْهَدُ أَنَّكَ قَدْ بَلَّغْتَ رِسَالاتِ رَبِّكَ، وَنَصَحْتَ لأُمَّتِكَ، وَجَاهَدْتَ فِي سَبِيلِ اللهِ حَقَّ جِهَادِهِ. فَجَزَاكَ اللهُ أَفْضَلَ مَا جَزَى نَبِيّاً عَنْ أُمَّتِهِ.",
                enText = "I bear witness that there is no god but Allah, alone without partner, and I bear witness that you are Muhammad, His servant and Messenger. He sent you with truth as a bringer of glad tidings and a warner, calling to Allah by His permission, and a shining lamp. I bear witness that you conveyed the messages of your Lord, advised your nation, and strived in the way of Allah as is His right. May Allah reward you with the best reward He ever bestowed on a Prophet on behalf of his nation.",
                urText = "میں گواہی دیتا ہوں کہ اللہ کے سوا کوئی معبود نہیں وہ اکیلا ہے اس کا کوئی شریک نہیں، اور گواہی دیتا ہوں کہ آپ محمدؐ اس کے بندے اور رسول ہیں۔ آپ کو حق کے ساتھ خوشخبری دینے والا اور ڈرانے والا اور اللہ کی اجازت سے اس کی طرف بلانے والا اور روشن چراغ بنا کر بھیجا۔ میں گواہی دیتا ہوں کہ آپ نے اپنے رب کے پیغامات پہنچائے، اپنی امت کو نصیحت فرمائی اور اللہ کی راہ میں اس طرح جہاد کیا جیسا جہاد کا حق ہے۔ پس اللہ آپ کو بہترین جزا دے جو اس نے کسی نبی کو اس کی امت کی طرف سے دی ہے۔"
            ),
            ZiyaratItem(
                id = "ziyarat_aminullah",
                masoomIndex = 2,
                titleEn = "Ziyarah Aminullah (Imam Ali)",
                titleAr = "زيارة أمين الله (الإمام علي)",
                titleUr = "زیارت امین اللہ (حضرت امام علی علیہ السلام)",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا أَمِينَ اللهِ فِي أَرْضِهِ، وَحُجَّتَهُ عَلَى عِبَادِهِ، السَّلامُ عَلَيْكَ يَا أَمِيرَ الْمُؤْمِنِينَ. أَشْهَدُ أَنَّكَ جَاهَدْتَ فِي اللهِ حَقَّ جِهَادِهِ، وَعَمِلْتَ بِكِتَابِهِ، وَاتَّبَعْتَ سُنَنَ نَبِيِّهِ صَلَّى اللهُ عَلَيْهِ وَآلِهِ، حَتَّى دَعَاكَ اللهُ إِلَى جِوَارِهِ فَقَبَضَكَ إِلَيْهِ بِاخْتِيَارِهِ.",
                enText = "Peace be upon you, O trusted guardian of Allah on His earth and His witness over His servants. Peace be upon you, O Commander of the Faithful. I bear witness that you strived in the way of Allah as is His right, acted upon His Book, and followed the traditions of His Prophet, peace of Allah be upon him and his family, until Allah called you to His presence and took you by His choice.",
                urText = "سلام ہو آپ پر اے زمین میں اللہ کے امین اور اس کے بندوں پر اس کی حجت، سلام ہو آپ پر اے امیر المومنین۔ میں گواہی دیتا ہوں کہ آپ نے اللہ کی راہ میں اس طرح جہاد کیا جیسا جہاد کا حق ہے، اس کی کتاب پر عمل کیا اور اس کے نبی صلی اللہ علیہ وآلہ وسلم کی سنتوں کی پیروی کی، یہاں تک کہ اللہ نے آپ کو اپنے جوارِ رحمت میں بلا لیا اور اپنے اختیار سے آپ کی روح قبض کر لی۔"
            ),
            ZiyaratItem(
                id = "ziyarat_fatimah",
                masoomIndex = 3,
                titleEn = "Ziyarah of Lady Fatima al-Zahra (sa)",
                titleAr = "زيارة السيدة فاطمة الزهراء (ع)",
                titleUr = "زیارت سیدہ فاطمہ زہرا سلام اللہ علیہا",
                audioUrl = "",
                arText = "يَا مُمْتَحَنَةُ امْتَحَنَكِ اللهُ الَّذِي خَلَقَكِ قَبْلَ أَنْ يَخْلُقَكِ، فَوَجَدَكِ لِمَا امْتَحَنَكِ صَابِرَةً، وَنَحْنُ نَزْعُمُ أَنَّا لَكِ أَوْلِيَاءُ وَمُصَدِّقُونَ وَصَابِرُونَ لِكُلِّ مَا أَتَانَا بِهِ أَبُوكِ صَلَّى اللهُ عَلَيْهِ وَآلِهِ وَأَتَى بِهِ وَصِيُّهُ، فَإِنَّا نَسْأَلُكِ إِنْ كُنَّا صَدَّقْنَاكِ إِلاَّ أَلْحَقْتِنَا بِتَصْدِيقِنَا لَهُمَا لِنُبَشِّرَ أَنْفُسَنَا بِأَنَّا قَدْ طَهُرْنَا بِوِلايَتِكِ.",
                enText = "O carefully examined lady! Allah who created you tried you before He created you, and He found you patiently resigning to His trials. We believe we are your loyal friends, and we remain patient through everything brought to us by your father (saws) and his successor. We ask you, if we have been truthful to you, to join us to our testimony of them, so we may glad-tide ourselves that we have been purified by your guardianship.",
                urText = "اے وہ آزمائی ہوئی ہستی جس کا امتحان اس اللہ نے لیا جس نے تمہیں پیدا کرنے سے پہلے تمہیں آزمایا پس تمہیں اس امتحان میں صابر پایا۔ ہمارا دعویٰ ہے کہ ہم آپ کے دوستدار، تصدیق کرنے والے اور ہر اس چیز پر صبر کرنے والے ہیں جو آپ کے والدؐ اور ان کے وصیؑ لے کر آئے ہیں۔ پس اگر ہم نے آپ کی سچی فرمانبرداری کی ہے تو ہمیں ان کی تصدیق کے ساتھ ملا دیجئے تاکہ ہم خود کو بشارت دیں کہ ہم آپ کی ولایت کے طفیل پاکیزہ ہو چکے ہیں۔"
            ),
            ZiyaratItem(
                id = "ziyarat_hasan",
                masoomIndex = 4,
                titleEn = "Ziyarah of Imam Hasan al-Mujtaba (as)",
                titleAr = "زيارة الإمام الحسن المجتبى (ع)",
                titleUr = "زیارت حضرت امام حسن مجتبیٰ علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا ابْنَ رَسُولِ رَبِّ الْعَالَمِينَ، السَّلامُ عَلَيْكَ يَا ابْنَ أَمِيرِ الْمُؤْمِنِينَ، السَّلامُ عَلَيْكَ يَا ابْنَ فَاطَمَةَ الزَّهْرَاءِ. السَّلامُ عَلَيْكَ يَا حَبِيبَ اللهِ، السَّلامُ عَلَيْكَ يَا صَفْوَةَ اللهِ، السَّلامُ عَلَيْكَ يَا أَمِينَ اللهِ، أَرْجُو بِعِظَمِ حَقِّكَ عَلَى اللهِ تَعَالَى أَنْ يَجْعَلَنِي مِنْ أَنْصَارِكَ وَأَتْبَاعِكَ وَمُحِبِّيكَ.",
                enText = "Peace be upon you, O son of the Messenger of the Lord of the worlds. Peace be upon you, O son of the Commander of the Faithful. Peace be upon you, O son of Fatima al-Zahra. Peace be upon you, O beloved of Allah, O choice of Allah, O trusted one of Allah. I hope by the greatness of your right over Allah, that He counts me among your helpers, followers, and lovers.",
                urText = "سلام ہو آپ پر اے رب العالمین کے رسول کے فرزند، سلام ہو آپ پر اے امیر المومنین کے بیٹے، سلام ہو آپ پر اے سیدہ فاطمہ زہراؑ کے فرزند۔ سلام ہو آپ پر اے اللہ کے حبیب، سلام ہو آپ پر اے اللہ کے برگزیدہ بندے، سلام ہو آپ پر اے اللہ کے امین۔ میں امید رکھتا ہوں کہ اللہ کے ہاں آپ کے عظیم حق کے صدقے اللہ مجھے آپ کے مددگاروں، پیروکاروں اور محبت کرنے والوں میں شامل کرے گا۔"
            ),
            ZiyaratItem(
                id = "ziyarat_ashura",
                masoomIndex = 5,
                titleEn = "Ziyarah Ashura (Imam Hussain)",
                titleAr = "زيارة عاشوراء (الإمام الحسين)",
                titleUr = "زیارت عاشورہ (حضرت امام حسین علیہ السلام)",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا أَبَا عَبْدِاللهِ، السَّلامُ عَلَيْكَ يَا ابْنَ رَسُولِ اللهِ، السَّلامُ عَلَيْكَ يَا خِيَرَةَ اللهِ وَابْنَ خِيَرَتِهِ. السَّلامُ عَلَيْكَ يَا ابْنَ أَمِيرِ الْمُؤْمِنِينَ وَابْنَ سَيِّدِ الْوَصِيِّينَ، السَّلامُ عَلَيْكَ يَا ابْنَ فَاطَمَةَ سَيِّدَةِ نِسَاءِ الْعَالَمِينَ... صَلَّى اللهُ عَلَيْكَ وَعَلَى أَرْوَاحِكُمْ وَأَجْسَادِكُمْ.",
                enText = "Peace be upon you, O Aba Abdillah! Peace be upon you, O son of the Messenger of Allah, O choice of Allah and son of His choice. Peace be upon you, O son of the Commander of the Faithful and son of the master of successors. Peace be upon you, O son of Fatima, the leader of the women of the worlds. May Allah send blessings on you, and on your souls and bodies.",
                urText = "سلام ہو آپ پر اے ابا عبداللہ، سلام ہو آپ پر اے رسول خدا کے فرزند، سلام ہو آپ پر اے اللہ کے چنے ہوئے اور ان کے فرزند۔ سلام ہو آپ پر اے امیر المومنین اور اوصیاء کے سردار کے فرزند، سلام ہو آپ پر اے فاطمہ زہراؑ سیدہ نساء العالمین کے فرزند۔ اللہ کا درود ہو آپ پر اور آپ کی روحوں اور جسموں پر۔"
            ),
            ZiyaratItem(
                id = "ziyarat_sajjad",
                masoomIndex = 6,
                titleEn = "Ziyarah of Imam Ali al-Sajjad (as)",
                titleAr = "زيارة الإمام علي السجاد (ع)",
                titleUr = "زیارت حضرت امام زین العابدین علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا زَيْنَ الْعَابِدِينَ، السَّلامُ عَلَيْكَ يَا حُجَّةَ اللهِ عَلَى الْخَلْقِ، السَّلامُ عَلَيْكَ يَا مَنْ بَكَى الدُّمُوعَ لِمُصَابِ أَبِيهِ. أَشْهَدُ أَنَّكَ قَدْ أَقَمْتَ الصَّلاةَ، وَآتَيْتَ الزَّكَاةَ، وَأَمَرْتَ بِالْمَعْرُوفِ، وَنَهَيْتَ عَنِ الْمُنْكَرِ، وَتَلَوْتَ الْكِتَابَ حَقَّ تِلاوَتِهِ.",
                enText = "Peace be upon you, O adornment of worshippers! Peace be upon you, O proof of Allah over creation. Peace be upon you, O he who shed tears of blood for the tragedy of his father. I bear witness that you established prayer, gave charity, enjoined good, forbade evil, and recited the Book with its true recitation.",
                urText = "سلام ہو آپ پر اے عابدوں کی زینت، سلام ہو آپ پر اے مخلوق پر اللہ کی حجت۔ سلام ہو آپ پر اے وہ جنہوں نے اپنے مظلوم والد کے غم میں خون کے آنسو بہائے۔ میں گواہی دیتا ہوں کہ آپ نے نماز قائم کی، زکوٰۃ دی، نیکی کا حکم دیا، برائی سے روکا، اور کتابِ خدا کی اس طرح تلاوت کی جیسا تلاوت کا حق ہے۔"
            ),
            ZiyaratItem(
                id = "ziyarat_baqir",
                masoomIndex = 7,
                titleEn = "Ziyarah of Imam Muhammad al-Baqir (as)",
                titleAr = "زيارة الإمام محمد الباقر (ع)",
                titleUr = "زیارت حضرت امام محمد باقر علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا بَاقِرَ عِلْمِ النَّبِيِّينَ، وَخَازِنَ وَحْيِ رَبِّ الْعَالَمِينَ. أَشْهَدُ أَنَّكَ الْحَقُّ الظَّاهِرُ، وَالنُّورُ الْبَاهِرُ، وَصِرَاطُ اللهِ الْمُسْتَقِيمُ. جَزَاكَ اللهُ خَيْراً عَنِ الإِسْلامِ وَأَهْلِهِ، صَلَوَاتُ اللهِ عَلَيْكَ وَعَلَى آبَائِكَ الطَّاهِرِينَ.",
                enText = "Peace be upon you, O splitter of the knowledge of the Prophets and custodian of the revelation of the Lord of the worlds. I bear witness that you are the manifest truth, the brilliant light, and the straight path of Allah. May Allah reward you with goodness on behalf of Islam and its fellows. Blessings of Allah be upon you and your pure ancestors.",
                urText = "سلام ہو آپ پر اے نبیوں کے علم کو شگافتہ کرنے والے اور رب العالمین کے وحی کے امین۔ میں گواہی دیتا ہوں کہ آپ ہی حقِ ظاہر، چمکتا ہوا نور اور اللہ کا سیدھا راستہ ہیں۔ اللہ آپ کو اسلام اور اہلِ اسلام کی طرف سے بہترین جزائے خیر عطا فرمائے۔ اللہ کا درود ہو آپ پر اور آپ کے پاک آباء و اجداد پر۔"
            ),
            ZiyaratItem(
                id = "ziyarat_sadiq",
                masoomIndex = 8,
                titleEn = "Ziyarah of Imam Ja'far al-Sadiq (as)",
                titleAr = "زيارة الإمام جعفر الصادق (ع)",
                titleUr = "زیارت حضرت امام جعفر صادق علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا جَعْفَرَ بْنَ مُحَمَّدٍ الصَّادِقَ الأَمِينَ، السَّلامُ عَلَيْكَ يَا كَنْزَ الْحِكْمَةِ وَالْيَقِينِ. أَشْهَدُ أَنَّكَ نَشَرْتَ هُدَى جَدِّكَ، وَأَسَّسْتَ عُلُومَ الصِّدْقِ وَالْعَدْلِ، وَأَنَّ قَوْلَكَ حَقٌّ وَأَمْرَكَ رُشْدٌ. لَعَنَ اللهُ مَنْ جَحَدَ حَقَّكَ أَوْ ظَلَمَكَ.",
                enText = "Peace be upon you, O Ja'far ibn Muhammad, the truthful and trustworthy! Peace be upon you, O treasure of wisdom and certainty. I bear witness that you spread the guidance of your grandfather, established the sciences of truth and justice, and that your speech is truth and your command is guidance. May Allah condemn whoever rejects your right or oppresses you.",
                urText = "سلام ہو آپ پر اے جعفر بن محمد سچے اور امین، سلام ہو آپ پر اے حکمت اور یقین کے خزانے۔ میں گواہی دیتا ہوں کہ آپ نے اپنے ناناؐ کی ہدایت کو عام کیا، سچائی اور انصاف کے علوم کی بنیاد رکھی اور آپ کا قول حق اور آپ کا حکم سراسر ہدایت ہے۔ اللہ کی لعنت ہو اس پر جس نے آپ کے حق کا انکار کیا یا آپ پر ظلم کیا۔"
            ),
            ZiyaratItem(
                id = "ziyarat_kadhim",
                masoomIndex = 9,
                titleEn = "Ziyarah of Imam Musa al-Kadhim (as)",
                titleAr = "زيارة الإمام موسى الكاظم (ع)",
                titleUr = "زیارت حضرت امام موسیٰ کاظم علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا أَبَا الْحَسَنِ مُوسَى بْنَ جَعْفَرٍ، السَّلامُ عَلَيْكَ يَا حَلِيفَ السَّجْدَةِ الطَّوِيلَةِ، وَالدُّمُوعِ الْغَزِيرَةِ، وَالْقَبْرِ الْمَضْلُومِ السَّمِيمِ. أَشْهَدُ أَنَّكَ قَدْ صَبَرْتَ فِي اللَّيَالِي الْمُظْلِمَةِ فِي جَوْفِ السُّجُونِ حَتَّى أَتَاكَ الْيَقِينُ. صَلَّى اللهُ عَلَيْكَ النُّورَ الطَّاهِرَ الْمُظْهَرَ.",
                enText = "Peace be upon you, O Aba al-Hasan, Musa ibn Ja'far! Peace be upon you, O companion of the prolonged prostration, the abundant tears, and the oppressed poisoned grave. I bear witness that you remained patient in the dark nights in the depths of dungeons until certainty came to you. Blessings of Allah be upon you, the pure, immaculate light.",
                urText = "سلام ہو آپ پر اے ابوالحسن موسیٰ بن جعفر، سلام ہو آپ پر اے طویل سجدوں اور کثیر آنسوؤں والے اور ستم دیدہ زہر آلود قبر مطہر والے۔ میں گواہی دیتا ہوں کہ آپ نے قید خانوں کی ہولناک تاریک راتوں میں تاحیات اس وقت تک صبر کیا جب کہ موت نے آکر زندگی کو پا لیا۔ اللہ کا درود ہو آپ پر جو ایک نہایت بلند و پاکیزہ نور ہیں۔"
            ),
            ZiyaratItem(
                id = "ziyarat_ridha",
                masoomIndex = 10,
                titleEn = "Ziyarah of Imam Ali al-Ridha (as)",
                titleAr = "زيارة الإمام علي الرضا (ع)",
                titleUr = "زیارت حضرت امام علی رضا علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا عَلِيَّ بْنَ مُوسَى الرِّضَا الْمُرْتَضَى، السَّلامُ عَلَيْكَ يَا سُلْطَانَ أَرْضِ طُوسَ، السَّلامُ عَلَيْكَ يَا غَرِيبَ الْغُرَبَاءِ وَيَا ضَامِنَ الْجِنَانِ. أَشْهَدُ أَنَّكَ الإِمَامُ الْحَقُّ النَّاصِحُ، لَعَنَ اللهُ مَنْ قَتَلَكَ أَوْ آذَاكَ، جَعَلَنَا اللهُ مِنْ زُوَّارِكَ وَمَحْشُورِينَ فِي زُمْرَتِكَ.",
                enText = "Peace be upon you, O Ali ibn Musa, the well-pleased, the chosen! Peace be upon you, O king of the land of Tus. Peace be upon you, O stranger of strangers and guarantor of Paradise! I bear witness that you are the true, advising Imam. May Allah condemn whoever killed or offended you. May Allah count us among your visitors and gather us in your companionship.",
                urText = "سلام ہو آپ پر اے علی بن موسیٰ رضا مرتضیٰ، سلام ہو آپ پر اے سرزمینِ طوس کے بادشاہ، سلام ہو آپ پر اے مسافروں میں سب سے غریب اور اے جنت کے ضامن۔ میں گواہی دیتا ہوں کہ آپ برحق اور سچے خیر خواہ امام ہیں۔ اللہ کی لعنت ہو اس پر جس نے آپ کو قتل کیا یا اذیت پہنچائی، اللہ ہمیں آپ کے زائرین میں لکھے اور آپ کے گروہ میں محشور فرمائے۔"
            ),
            ZiyaratItem(
                id = "ziyarat_jawad",
                masoomIndex = 11,
                titleEn = "Ziyarah of Imam Muhammad al-Jawad (as)",
                titleAr = "زيارة الإمام محمد الجواد (ع)",
                titleUr = "زیارت حضرت امام محمد تقی الجواد علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا مُحَمَّدَ بْنَ عَلِيٍّ التَّقِيَّ الْجَوَادَ، السَّلامُ عَلَيْكَ يَا بَابَ الْمُرَادِ وَمَعْدِنَ الصَّفَاءِ وَالْوَدَادِ. السَّلامُ عَلَيْكَ يَا سَجَّادَ الْجُودِ، وَحُجَّةَ الْمَلِكِ الْمَعْبُودِ. أَشْهَدُ أَنَّكَ قَدْ أَدَّيْتَ الأَمَانَةَ، وَنَصَحْتَ لِعِبَادِ اللهِ حَتَّى قُبِضْتَ شَهِيداً مَظْلُوماً.",
                enText = "Peace be upon you, O Muhammad ibn Ali, the God-fearing, the Generous! Peace be upon you, O gate of wishes and source of purity and love. Peace be upon you, O worshipper of generosity and proof of the Worshipful King. I bear witness that you fulfilled the trust and advised the servants of Allah until you died as a martyr and oppressed.",
                urText = "سلام ہو آپ پر اے محمد بن علی تقی جواد، سلام ہو آپ پر اے مرادوں کے دروازے اور پاکیزگی و محبت کی کان۔ سلام ہو آپ پر اے سخاوت کے پیکر اور مالک و معبودِ حقیقی کی حجت۔ میں گواہی دیتا ہوں کہ آپ نے امانت پہنچائی اور اللہ کے بندوں کو نصیحت فرمائی یہاں تک کہ آپ نے جامِ شہادت نوش فرمایا جبکہ آپ مظلوم تھے۔"
            ),
            ZiyaratItem(
                id = "ziyarat_hadi",
                masoomIndex = 12,
                titleEn = "Ziyarah of Imam Ali al-Hadi (as)",
                titleAr = "زيارة الإمام علي الهادي (ع)",
                titleUr = "زیارت حضرت امام علی نقی الہادی علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا أَبَا الْحَسَنِ عَلِيَّ بْنَ مُحَمَّدٍ النَّقِيَّ الْهَادِي. السَّلامُ عَلَيْكَ يَا حُجَّةَ الرَّحْمَٰنِ، السَّلامُ عَلَيْكَ يَا مَفْزَعَ اللهْفَانِ. أَشْهَدُ أَنَّكَ حَبْلُ اللهِ الْمَتِينُ، وَعُرْوَتُهُ الْوُثْقَى، وَأَنَّ الْعَارِفَ بِكُمْ سَعِيدٌ وَالْمُنْكِرَ لَكُمْ شَقِيٌّ.",
                enText = "Peace be upon you, O Aba al-Hasan, Ali ibn Muhammad, the pure Guide! Peace be upon you, O proof of the Beneficent, O shelter of the grieved one. I bear witness that you are the firm rope of Allah, His strongest handle, and that whoever recognizes you is prosperous and whoever denies you is damned.",
                urText = "سلام ہو آپ پر اے ابوالحسن علی بن محمد نقی و ہادی۔ سلام ہو آپ پر اے خدا کے برگزیدہ بندے اور رحمان کی حجت، سلام ہو آپ پر اے سائلوں اور دکھ درد ماروں کی جائے پناہ۔ میں گواہی دیتا ہوں کہ آپ اللہ کی مضبوط رسی اور محکم سہارا ہیں، اور بے شک آپ کا عارف خوش نصیب ہے اور آپ کا منکر بدبخت ہے۔"
            ),
            ZiyaratItem(
                id = "ziyarat_askari",
                masoomIndex = 13,
                titleEn = "Ziyarah of Imam Hasan al-Askari (as)",
                titleAr = "زيارة الإمام الحسن العسكري (ع)",
                titleUr = "زیارت حضرت امام حسن عسکری علیہ السلام",
                audioUrl = "",
                arText = "السَّلامُ عَلَيْكَ يَا أَبَا مُحَمَّدٍ الْحَسَنَ بْنَ عَلِيٍّ الْعَسْكَرِيَّ الْمُؤْتَمَنَ. السَّلامُ عَلَيْكَ يَا مَوْلايَ يَا حُجَّةَ الْعَصْرِ، أَشْهَدُ أَنَّ اللَّٰهَ الإِمَامَ الْهَادِيَ لَكَ جَعَلَ، وَأَنَّكَ قَدْ أَوْصَيْتَ بِابْنِكَ الْقَائِمِ الْمَهْدِيِّ جَعَلَنَا اللهُ مِنْ خُدَّامِهِ.",
                enText = "Peace be upon you, O Aba Muhammad, Hasan ibn Ali, the trustworthy Soldier! Peace be upon you, O my master! I bear witness that Allah made you a guiding Imam, and that you made a testament regarding your son, the ruling Savior. May Allah make us among his servants.",
                urText = "سلام ہو آپ پر اے ابو محمد حسن بن علی عسکری امین۔ سلام ہو آپ پر اے میرے مولا، میں گواہی دیتا ہوں کہ اللہ نے آپ کو ہدایت دینے والا امام قرار دیا ہے اور آپ نے اپنے بیٹے قائمِ آلِ محمدؑ کے وصی ہونے کی وصیت فرمائی، اللہ تعالی ہمیں ان کے خادموں میں شمار کرے۔"
            ),
            ZiyaratItem(
                id = "ziyarat_yasin",
                masoomIndex = 14,
                titleEn = "Ziyarah Al-Yasin (Imam Mahdi)",
                titleAr = "زيارة آل ياسين (الإمام المهدي)",
                titleUr = "زیارت آلِ یاسین (حضرت صاحب الزماں عجل اللہ فرجہ)",
                audioUrl = "",
                arText = "سَلامٌ عَلَىٰ آلِ يَاسِينَ، السَّلامُ عَلَيْكَ يَا دَاعِيَ اللهِ وَرَبَّانِيَّ آيَاتِهِ، السَّلامُ عَلَيْكَ يَا بَابَ اللهِ وَدَيَّانَ دِينِهِ، السَّلامُ عَلَيْكَ يَا كَلِمَةَ اللهِ وَدَلِيلَ آيَاتِهِ، السَّلامُ عَلَيْكَ حِينَ تَقُومُ وَحِينَ تَقْعُدُ، السَّلامُ عَلَيْكَ حِينَ تَقْرَأُ وَتُبَيِّنُ، السَّلامُ عَلَيْكَ حِينَ تُصَلِّي وَتَقْنُتُ...",
                enText = "Peace be upon the family of Yasin! Peace be upon you, O caller to Allah and interpreter of His verses. Peace be upon you, O gate to Allah and judge of His religion. Peace be upon you, O word of Allah and guide to His verses. Peace be upon you when you stand and when you sit. Peace be upon you when you recite and explain, when you pray and supplicate...",
                urText = "سلام ہو آلِ یاسین پر، سلام ہو آپ پر اے اللہ کے بلانے والے اور اس کی آیات کے دانا مفسر۔ سلام ہو آپ پر اے اللہ کے علم و کرم کا دروازہ اور اس کے دینِ کامل کے عادل نگہبان۔ سلام ہو آپ پر اے کلمۃ اللہ اور اس کی نشانیوں کے رہنما۔ سلام ہو آپ پر جب آپ کھڑے ہوتے ہیں اور جب بیٹھتے ہیں، سلام ہو آپ پر جب تلاوت اور تشریح کرتے ہیں، جب نماز اور قنوت پڑھتے ہیں..."
            )
        )
    }
}
