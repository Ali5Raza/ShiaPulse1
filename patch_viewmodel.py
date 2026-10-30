import re

with open("app/src/main/java/com/example/ui/PrayerViewModel.kt", "r") as f:
    content = f.read()

# Insert Finance state variables
finance_vars = """
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
"""

# Insert inside the class just before the last closing brace
# But let's be careful and put it where the other MutableStateFlows are.
# Find `val currencySymbol = _currencySymbol.asStateFlow()` and insert after the setter.
insert_pos = content.find("fun setAppLanguage(lang: String) {")

if insert_pos != -1:
    content = content[:insert_pos] + finance_vars + content[insert_pos:]
    with open("app/src/main/java/com/example/ui/PrayerViewModel.kt", "w") as f:
        f.write(content)
    print("Patched PrayerViewModel")
else:
    print("Could not find insert position")

