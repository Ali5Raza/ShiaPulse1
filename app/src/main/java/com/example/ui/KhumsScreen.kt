package com.example.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.HijriCalendarHelper
import com.example.utils.LocalizationUtility
import java.util.Calendar

data class CalcTool(
    val key: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhumsScreen(
    viewModel: PrayerViewModel,
    initialTool: String? = null,
    onBack: () -> Unit,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    var selectedTool by remember { mutableStateOf<String?>(initialTool) }
    
    BackHandler(enabled = selectedTool != null) {
        selectedTool = null
    }

    val tools = listOf(
        CalcTool("khums", LocalizationUtility.get("khums_calc", currentLang), LocalizationUtility.get("khums_calc_desc", currentLang), Icons.Default.AccountBalanceWallet),
        CalcTool("zakat", LocalizationUtility.get("zakat_calc", currentLang), LocalizationUtility.get("zakat_calc_desc", currentLang), Icons.Default.Payments),
        CalcTool("fitrana", LocalizationUtility.get("fitrana_calc", currentLang), LocalizationUtility.get("fitrana_calc_desc", currentLang), Icons.Default.Calculate),
        CalcTool("ushar", when (currentLang) { "ur" -> "عشر کیلکولیٹر"; "ar" -> "حاسبة العشر"; else -> "Ushar Calculator" }, when (currentLang) { "ur" -> "پیداوار پر عشر یا نصف عشر کا حساب"; "ar" -> "احسب العشر أو نصف العشر على المحصول (10٪/5٪)."; else -> "Calculate Shia agricultural tax (10%/5%)." }, Icons.Default.Grass),
        CalcTool("mehr", when (currentLang) { "ur" -> "مہرِ فاطمیؑ کیلکولیٹر"; "ar" -> "حاسبة المهر الفاطمي"; else -> "Mehr-e-Fatimi Calculator" }, when (currentLang) { "ur" -> "سیدہ فاطمہؑ کے مہرِ سنت کے مطابق چاندی کی قیمت"; "ar" -> "احسب مهر السنة (1250 غرامًا من الفضة الخالصة)."; else -> "Calculate Mehr al-Sunnah (1250g pure Silver)." }, Icons.Default.BrightnessHigh),
        CalcTool("hajj_umrah", when (currentLang) { "ur" -> "حج و عمرہ رہنمائے"; "ar" -> "دليل الحج والعمرة"; else -> "Hajj & Umrah Guide" }, when (currentLang) { "ur" -> "مناسک اور دعاؤں کی قدم بہ قدم رہنمائی"; "ar" -> "قائمة خطوات المناسك والأدعية."; else -> "Step-by-step rituals and duas checklist." }, Icons.Default.EventNote),
        CalcTool("qadha", when (currentLang) { "ur" -> "قضا نمازیں"; "ar" -> "الصلوات الفائتة"; else -> "Qadha Prayers" }, when (currentLang) { "ur" -> "قضا نمازوں کا حساب اور ٹریکنگ"; "ar" -> "تتبع الصلوات الفائتة واحسبها."; else -> "Track and calculate your missed prayers." }, Icons.Default.Event),
        CalcTool("musafir", LocalizationUtility.get("musafir_calc", currentLang), LocalizationUtility.get("musafir_calc_desc", currentLang), Icons.Default.AirplanemodeActive),
        CalcTool("inheritance", LocalizationUtility.get("inheritance_calc", currentLang), LocalizationUtility.get("inheritance_calc_desc", currentLang), Icons.Default.FamilyRestroom),
        CalcTool("date_conv", LocalizationUtility.get("date_conv_calc", currentLang), LocalizationUtility.get("greg_hijri_desc", currentLang), Icons.Default.DateRange),
        CalcTool("sadaqah", when (currentLang) { "ur" -> "صدقہ ٹریکر"; "ar" -> "متتبع الصدقة"; else -> "Sadaqah Tracker" }, when (currentLang) { "ur" -> "اپنے روزمرہ کے صدقے کا حساب رکھیں"; "ar" -> "تتبع صدقاتك اليومية."; else -> "Track your daily Sadaqah." }, Icons.Default.VolunteerActivism),
        CalcTool("donations", when (currentLang) { "ur" -> "فنانس کی تاریخ"; "ar" -> "السجل المالي"; else -> "Finance History" }, when (currentLang) { "ur" -> "اپنے تمام مالی معاملات دیکھیں"; "ar" -> "اعرض جميع معاملاتك المالية."; else -> "View your complete finance logs." }, Icons.Default.HistoryEdu),
        CalcTool("finance_settings", when (currentLang) { "ur" -> "فنانس کی ترتیبات"; "ar" -> "الإعدادات المالية"; else -> "Finance Settings" }, when (currentLang) { "ur" -> "کرنسی، مرجع، اور قیمتوں کی ترتیبات"; "ar" -> "إعدادات العملة والمرجع والأسعار المباشرة."; else -> "Settings for Currency, Marja and Live Prices." }, Icons.Default.Settings),
        CalcTool("qurbani", when (currentLang) { "ur" -> "قربانی کیلکولیٹر"; "ar" -> "حاسبة الأضاحي"; else -> "Qurbani Calculator" }, when (currentLang) { "ur" -> "قربانی کے حصوں کا حساب لگائیں"; "ar" -> "احسب حصص الأضحية."; else -> "Calculate Qurbani shares." }, Icons.Default.Pets)
    )

    val titleText = when (selectedTool) {
        "khums" -> LocalizationUtility.get("khums_calc", currentLang)
        "zakat" -> LocalizationUtility.get("zakat_calc", currentLang)
        "fitrana" -> LocalizationUtility.get("fitrana_calc", currentLang)
        "ushar" -> when (currentLang) { "ur" -> "عشر کیلکولیٹر"; "ar" -> "حاسبة العشر"; else -> "Ushar Calculator" }
        "mehr" -> when (currentLang) { "ur" -> "مہرِ فاطمیؑ کیلکولیٹر"; "ar" -> "حاسبة المهر الفاطمي"; else -> "Mehr-e-Fatimi Calculator" }
        "sadaqah" -> when (currentLang) { "ur" -> "صدقہ ٹریکر"; "ar" -> "متتبع الصدقة"; else -> "Sadaqah Tracker" }
        "donations" -> when (currentLang) { "ur" -> "فنانس کی تاریخ"; "ar" -> "السجل المالي"; else -> "Finance History" }
        "finance_settings" -> when (currentLang) { "ur" -> "فنانس کی ترتیبات"; "ar" -> "الإعدادات المالية"; else -> "Finance Settings" }
        "qurbani" -> when (currentLang) { "ur" -> "قربانی کیلکولیٹر"; "ar" -> "حاسبة الأضاحي"; else -> "Qurbani Calculator" }
        "hajj_umrah" -> when (currentLang) { "ur" -> "حج و عمرہ رہنمائے"; "ar" -> "دليل الحج والعمرة"; else -> "Hajj & Umrah Guide" }
        "qadha" -> when (currentLang) { "ur" -> "قضا نمازیں"; "ar" -> "الصلوات الفائتة"; else -> "Qadha Prayers" }
        "musafir" -> LocalizationUtility.get("musafir_calc", currentLang)
        "inheritance" -> LocalizationUtility.get("inheritance_calc", currentLang)
        "date_conv" -> LocalizationUtility.get("date_conv_calc", currentLang)
        else -> LocalizationUtility.get("islamic_calculators", currentLang)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PremiumScreenTopBar(
                title = titleText,
                subtitle = if (selectedTool == null) LocalizationUtility.get("select_calc_sub", currentLang) else null,
                onBack = {
                    if (selectedTool != null && initialTool == null) {
                        selectedTool = null
                    } else {
                        onBack()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedTool == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunkedTools = tools.chunked(2)
                    chunkedTools.forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTools.forEach { tool ->
                                GridPlatformTile(
                                    title = tool.title,
                                    icon = tool.icon,
                                    onClick = { selectedTool = tool.key },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTools.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                when (selectedTool) {
                    "khums" -> KhumsTab(currentLang, currencySymbol, viewModel)
                    "zakat" -> ZakatTab(currentLang, currencySymbol, viewModel)
                    "fitrana" -> FitranaTab(currentLang, currencySymbol, viewModel)
                    "ushar" -> UsharTab(currentLang, currencySymbol, viewModel)
                    "mehr" -> MehrTab(currentLang, currencySymbol, viewModel)
                    "hajj_umrah" -> HajjUmrahTab(
                        viewModel = viewModel,
                        languageCode = currentLang,
                        onOpenQuran = onOpenQuran
                    )
                    "qadha" -> QadhaTab(viewModel = viewModel, currentLang = currentLang)
                    "date_conv" -> DateConverterTab(currentLang)
                    "musafir" -> MusafirTab(currentLang)
                    "inheritance" -> InheritanceTab(currentLang, currencySymbol)
                    "sadaqah" -> SadaqahTrackerTab(currentLang, currencySymbol, viewModel)
                    "donations" -> FinanceHistoryTab(currentLang, currencySymbol, viewModel)
                    "finance_settings" -> FinanceSettingsTab(currentLang, currencySymbol, viewModel)
                    "qurbani" -> QurbaniCalculatorTab(currentLang, currencySymbol, viewModel)
                }
            }
        }
    }
}


@Composable
fun KhumsTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
    val khumsYearStartDate by viewModel.khumsYearStartDate.collectAsState()
    var startDateInput by remember { mutableStateOf(khumsYearStartDate) }
    
    // To calculate remaining days
    val remainingDays = remember(startDateInput) {
        if (startDateInput.isNotEmpty()) {
            try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val startDate = format.parse(startDateInput)
                if (startDate != null) {
                    val cal = java.util.Calendar.getInstance()
                    val today = cal.timeInMillis
                    
                    cal.time = startDate
                    cal.add(java.util.Calendar.YEAR, 1) // next due date
                    val dueDate = cal.timeInMillis
                    
                    val diff = dueDate - today
                    if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() else 0
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var savingsInput by remember { mutableStateOf("") }
    var expensesInput by remember { mutableStateOf("") }

    val totalSavings = savingsInput.toDoubleOrNull() ?: 0.0
    val totalExpenses = expensesInput.toDoubleOrNull() ?: 0.0
    val surplus = (totalSavings - totalExpenses).coerceAtLeast(0.0)
    val khums = surplus * 0.20
    val sehmImam = khums / 2
    val sehmSadat = khums / 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = LocalizationUtility.get("khums_info_text", currentLang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        OutlinedTextField(
            value = savingsInput,
            onValueChange = { savingsInput = it },
            label = { Text(LocalizationUtility.get("annual_income", currentLang)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("$currencySymbol ") }
        )

        OutlinedTextField(
            value = expensesInput,
            onValueChange = { expensesInput = it },
            label = { Text(LocalizationUtility.get("allowable_expenses", currentLang)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("$currencySymbol ") }
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = LocalizationUtility.get("total_surplus", currentLang) + ": $currencySymbol${String.format("%.2f", surplus)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LocalizationUtility.get("khums_due", currentLang) + " (20%): $currencySymbol${String.format("%.2f", khums)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = LocalizationUtility.get("sehm_imam_label", currentLang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", sehmImam)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = LocalizationUtility.get("sehm_sadat_label", currentLang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", sehmSadat)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZakatTab(languageCode: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var goldGramInput by remember { mutableStateOf("") }
    var silverGramInput by remember { mutableStateOf("") }
    var cashSavingsInput by remember { mutableStateOf("") }

    // Prefilled values for prices per gram
    var goldPriceInput by remember { mutableStateOf("75.00") }
    var silverPriceInput by remember { mutableStateOf("0.90") }

    val goldGrams = goldGramInput.toDoubleOrNull() ?: 0.0
    val silverGrams = silverGramInput.toDoubleOrNull() ?: 0.0
    val cashSavings = cashSavingsInput.toDoubleOrNull() ?: 0.0

    val goldPrice = goldPriceInput.toDoubleOrNull() ?: 75.0
    val silverPrice = silverPriceInput.toDoubleOrNull() ?: 0.90

    // Shia Jafari Jurisprudence Nisab constants
    // Nisab for Gold coins is 20 Dinars ~ 96 grams (exact: 15 mithqal ~ 86g/96g based on rulings)
    // Nisab for Silver is 200 Dirhams ~ 700 grams (exact: 105 mithqal ~ 483g/700g based on rulings)
    val goldNisabGrams = 96.0
    val silverNisabGrams = 700.0

    val isGoldEligible = goldGrams >= goldNisabGrams
    val isSilverEligible = silverGrams >= silverNisabGrams

    val goldZakat = if (isGoldEligible) (goldGrams * goldPrice * 0.025) else 0.0
    val silverZakat = if (isSilverEligible) (silverGrams * silverPrice * 0.025) else 0.0
    val mustahabCashZakat = cashSavings * 0.025

    val totalZakat = goldZakat + silverZakat

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Shia Ruling",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (languageCode == "ur") "فقہ جعفریہ کے مطابق زکوٰۃ کا قانون:" else if (languageCode == "ar") "ملاحظة حول فقه الجعفري:" else "Shia Jurisprudence (Fiqh-e-Ja'fari) Note:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (languageCode == "ur") "زکوٰۃ صرف 9 چیزوں پر واجب ہے: سونے اور چاندی کے سکے، اونٹ، گائے، بھیڑ، گندم، جو، کھجور، اور کشمش۔ عمومی بچت، کرنسی، اور تجارتی مال پر 2.5 فیصد زکوٰۃ دینا مستحب ہے۔" else if (languageCode == "ar") "تجب الزكاة في تسعة أشياء فقط: الذهب والفضة (المسكوكان)، الإبل، البقر، الغنم، الحنطة، الشعير، التمر، والزبيب. وتستحب الزكاة بنسبة 2.5٪ على المدخرات العامة والعملات." else "Zakat is obligatorily due (Wajib) only on 9 items: Gold coins, Silver coins, Camels, Cows, Sheep, Wheat, Barley, Dates, and Raisins. It is highly recommended (Mustahab) on general saves, currencies, and trade goods at a rate of 2.5%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (languageCode == "ur") "نوٹ: یہ کیلکولیٹر سونا، چاندی اور کیش پر مرکوز ہے جو کہ زیادہ تر لوگوں کے لئے لاگو ہوتا ہے۔ زراعت اور مویشیوں کے لئے مخصوص مقداری حدود ہیں۔" else if (languageCode == "ar") "ملاحظة: تركز هذه الحاسبة على الذهب والفضة والنقد. تتطلب العناصر الزراعية والحيوانية الأخرى حدودًا كمية محددة." else "Note: This calculator focuses on Gold, Silver, and fiat Cash, which apply to most modern users. The 7 agricultural and livestock items require specific quantitative thresholds (e.g. 5 camels, 40 sheep) rather than direct market values.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = goldGramInput,
                onValueChange = { goldGramInput = it },
                label = { Text(if (languageCode == "ur") "سونے کا وزن (گرام)" else if (languageCode == "ar") "وزن الذهب (جرام)" else "Gold Weight (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = silverGramInput,
                onValueChange = { silverGramInput = it },
                label = { Text(if (languageCode == "ur") "چاندی کا وزن (گرام)" else if (languageCode == "ar") "وزن الفضة (جرام)" else "Silver Weight (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = goldPriceInput,
                onValueChange = { goldPriceInput = it },
                label = { Text(if (languageCode == "ur") "سونا $currencySymbol/g" else if (languageCode == "ar") "الذهب $currencySymbol/g" else "Gold $currencySymbol/g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = silverPriceInput,
                onValueChange = { silverPriceInput = it },
                label = { Text(if (languageCode == "ur") "چاندی $currencySymbol/g" else if (languageCode == "ar") "الفضة $currencySymbol/g" else "Silver $currencySymbol/g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = cashSavingsInput,
            onValueChange = { cashSavingsInput = it },
            label = { Text(if (languageCode == "ur") "مستحب زکوٰۃ کے لئے کیش/بچت" else if (languageCode == "ar") "المدخرات للزكاة المستحبة" else "Recommended (Mustahab) Cash/Savings") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("$currencySymbol ") }
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (languageCode == "ur") "زکوٰۃ واجب (حساب):" else if (languageCode == "ar") "حساب الزكاة الواجبة:" else "Zakat Obligatory Calculation (Wajib):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "سونے کی زکوٰۃ:" else if (languageCode == "ar") "زكاة الذهب:" else "Gold Coins Zakat:", style = MaterialTheme.typography.bodyMedium)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$currencySymbol${String.format("%.2f", goldZakat)}", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isGoldEligible) (if (languageCode == "ur") "نصاب (96g) سے متجاوز" else if (languageCode == "ar") "يتجاوز النصاب (96g)" else "Exceeds Nisab (96g)") else (if (languageCode == "ur") "نصاب (96g) سے کم" else if (languageCode == "ar") "أقل من النصاب (96g)" else "Below Nisab (96g)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGoldEligible) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "چاندی کی زکوٰۃ:" else if (languageCode == "ar") "زكاة الفضة:" else "Silver Coins Zakat:", style = MaterialTheme.typography.bodyMedium)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$currencySymbol${String.format("%.2f", silverZakat)}", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isSilverEligible) (if (languageCode == "ur") "نصاب (700g) سے متجاوز" else if (languageCode == "ar") "يتجاوز النصاب (700g)" else "Exceeds Nisab (700g)") else (if (languageCode == "ur") "نصاب (700g) سے کم" else if (languageCode == "ar") "أقل من النصاب (700g)" else "Below Nisab (700g)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSilverEligible) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "کل واجب زکوٰۃ:" else if (languageCode == "ar") "إجمالي الزكاة الواجبة:" else "Total Wajib Zakat Due:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$currencySymbol${String.format("%.2f", totalZakat)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "مستحب زکوٰۃ (کیش پر 2.5%):" else if (languageCode == "ar") "الزكاة المستحبة على النقد (2.5٪):" else "Recommended (Mustahab) Cash Zakat (2.5%):", style = MaterialTheme.typography.bodyMedium)
                    Text("$currencySymbol${String.format("%.2f", mustahabCashZakat)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FitranaTab(languageCode: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var familyMembersInput by remember { mutableStateOf("1") }
    var fitranaRateInput by remember { mutableStateOf("15.00") }

    val members = familyMembersInput.toIntOrNull() ?: 1
    val rate = fitranaRateInput.toDoubleOrNull() ?: 15.0
    val totalFitrana = members * rate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Fitrana Info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (languageCode == "ur") "زکوٰۃ الفطر (فطرانہ)" else if (languageCode == "ar") "زكاة الفطرة" else "Zakat al-Fitr (Fitrana)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (languageCode == "ur") "فطرانہ ہر اس شخص پر واجب ہے جو عاقل، بالغ اور دینے کی استطاعت رکھتا ہو۔ یہ عید الفطر کی رات واجب ہوتا ہے۔ اسے اپنے اور اپنے زیرِ کفالت تمام افراد کے لیے ادا کرنا ضروری ہے۔" else if (languageCode == "ar") "زكاة الفطرة واجبة على كل من توفرت فيه الشروط، وتدفع عن نفسه وعمن يعولهم ليلة عيد الفطر. والمقدار المعتاد نحو 3 كغم من القوت الشائع أو قيمته النقدية." else "Fitrana is Wajib (obligatory) upon whoever is sane, mature, and has the means to pay. It is due on the eve of Eid ul-Fitr. It must be paid for oneself and all dependents. The standard amount is approximately 3 kg of staple food (wheat, rice, dates, etc.) or its monetary equivalent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        OutlinedTextField(
            value = familyMembersInput,
            onValueChange = { familyMembersInput = it },
            label = { Text(if (languageCode == "ur") "خاندان کے افراد / زیرِ کفالت افراد کی تعداد" else if (languageCode == "ar") "عدد أفراد الأسرة" else "Number of Family Members / Dependents") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fitranaRateInput,
            onValueChange = { fitranaRateInput = it },
            label = { Text(if (languageCode == "ur") "فطرانہ فی کس (مقامی کرنسی میں)" else if (languageCode == "ar") "مبلغ الفطرة لكل شخص" else "Fitrana Rate per Person") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("$currencySymbol ") }
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (languageCode == "ur") "کل فطرانہ:" else if (languageCode == "ar") "إجمالي زكاة الفطرة:" else "Total Fitrana Due:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$currencySymbol${String.format("%.2f", totalFitrana)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DateConverterTab(languageCode: String) {
    var isGregorianToHijri by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle direction
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGregorianToHijri) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isGregorianToHijri = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (languageCode == "ur") "عیسوی ➔ ہجری" else if (languageCode == "ar") "ميلادي ➔ هجري" else "Gregorian ➔ Hijri",
                    color = if (isGregorianToHijri) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isGregorianToHijri) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isGregorianToHijri = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (languageCode == "ur") "ہجری ➔ عیسوی" else if (languageCode == "ar") "هجري ➔ ميلادي" else "Hijri ➔ Gregorian",
                    color = if (!isGregorianToHijri) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (isGregorianToHijri) {
            GregorianToHijriView(languageCode)
        } else {
            HijriToGregorianView(languageCode)
        }
    }
}

@Composable
fun GregorianToHijriView(languageCode: String) {
    var dayInput by remember { mutableStateOf("05") }
    var monthInput by remember { mutableStateOf("07") }
    var yearInput by remember { mutableStateOf("1995") }

    var resultText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Perform initial calculations automatically or on click
    LaunchedEffect(dayInput, monthInput, yearInput, languageCode) {
        val d = dayInput.toIntOrNull() ?: 1
        val m = monthInput.toIntOrNull() ?: 1
        val y = yearInput.toIntOrNull() ?: 1995
        try {
            val hDate = HijriCalendarHelper.convertGregorianToHijri(y, m, d)
            resultText = "${hDate.day} ${hDate.monthName} ${hDate.year} AH"
        } catch (e: Exception) {
            resultText = if (languageCode == "ur") "درست تاریخ درج کریں۔" else if (languageCode == "ar") "أدخل تاريخاً صحيحاً." else "Enter a valid date."
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (languageCode == "ur") "عیسوی (انگریزی) تاریخ کو قمری (ہجری) میں تبدیل کریں" else if (languageCode == "ar") "تحويل التاريخ الميلادي (الإنجليزي) إلى هجري قمري" else "Convert Western/English Date to Islamic Hijri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            yearInput = y.toString()
                            monthInput = String.format("%02d", m + 1)
                            dayInput = String.format("%02d", d)
                        },
                        yearInput.toIntOrNull() ?: calendar.get(Calendar.YEAR),
                        (monthInput.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1,
                        dayInput.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Date Picker")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (languageCode == "ur") "انگریزی کلینڈر کی تاریخ منتحب کریں" else if (languageCode == "ar") "فتح محدد التاريخ الميلادي" else "Open English Calendar Date Picker")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { dayInput = it },
                    label = { Text(if (languageCode == "ur") "دن" else if (languageCode == "ar") "يوم" else "Day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = monthInput,
                    onValueChange = { monthInput = it },
                    label = { Text(if (languageCode == "ur") "مہینہ (1-12)" else if (languageCode == "ar") "شهر (1-12)" else "Month (1-12)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = yearInput,
                    onValueChange = { yearInput = it },
                    label = { Text(if (languageCode == "ur") "سال" else if (languageCode == "ar") "سنة" else "Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f)
                )
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (languageCode == "ur") "ہجری تاریخ کا نتیجہ" else if (languageCode == "ar") "تاريخ الهجري الناتج" else "Resulting Shia Hijri Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun HijriToGregorianView(languageCode: String) {
    var hDayInput by remember { mutableStateOf("07") }
    var hMonthIndex by remember { mutableStateOf(1) } // 1-indexed, starting Safar (index 1 is Safar)
    var hYearInput by remember { mutableStateOf("1416") }

    var expandedMonthDropdown by remember { mutableStateOf(false) }
    var resultGregorianText by remember { mutableStateOf("") }

    LaunchedEffect(hDayInput, hMonthIndex, hYearInput, languageCode, HijriCalendarHelper.globalOffsetDays) {
        val hd = hDayInput.toIntOrNull() ?: 7
        val hm = hMonthIndex
        val hy = hYearInput.toIntOrNull() ?: 1416
        try {
            val cal = HijriCalendarHelper.convertHijriToGregorian(hy, hm, hd)
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sunday"
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                else -> "Saturday"
            }
            val engMonth = when (cal.get(Calendar.MONTH)) {
                0 -> "January"
                1 -> "February"
                2 -> "March"
                3 -> "April"
                4 -> "May"
                5 -> "June"
                6 -> "July"
                7 -> "August"
                8 -> "September"
                9 -> "October"
                10 -> "November"
                else -> "December"
            }
            resultGregorianText = "$dayOfWeek, ${cal.get(Calendar.DAY_OF_MONTH)} $engMonth ${cal.get(Calendar.YEAR)}"
        } catch (e: Exception) {
            resultGregorianText = if (languageCode == "ur") "درست تاریخ درج کریں۔" else if (languageCode == "ar") "أدخل تاريخاً صحيحاً." else "Enter a valid Hijri date."
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (languageCode == "ur") "قمری (ہجری) سے عیسوی (انگریزی) تاریخ میں تبدیل کریں" else if (languageCode == "ar") "تحويل التاريخ الهجري إلى الميلادي الإنجليزي" else "Convert Islamic Hijri to Western/English Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hDayInput,
                    onValueChange = { hDayInput = it },
                    label = { Text(if (languageCode == "ur") "ہجری دن" else if (languageCode == "ar") "يوم هجري" else "Hijri Day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.weight(1.8f)) {
                    OutlinedTextField(
                        value = HijriCalendarHelper.MONTH_NAMES.getOrElse(hMonthIndex - 1) { "" },
                        onValueChange = {},
                        label = { Text(if (languageCode == "ur") "ہجری مہینہ" else if (languageCode == "ar") "شهر هجري" else "Hijri Month") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { expandedMonthDropdown = true }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Select Month", modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedMonthDropdown,
                        onDismissRequest = { expandedMonthDropdown = false }
                    ) {
                        HijriCalendarHelper.MONTH_NAMES.forEachIndexed { idx, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    hMonthIndex = idx + 1
                                    expandedMonthDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hYearInput,
                    onValueChange = { hYearInput = it },
                    label = { Text(if (languageCode == "ur") "ہجری سال" else if (languageCode == "ar") "سنة هجرية" else "Hijri Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (languageCode == "ur") "انگریزی تاریخ کا نتیجہ" else if (languageCode == "ar") "التاريخ الميلادي الناتج" else "Resulting Gregorian Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resultGregorianText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MusafirTab(languageCode: String) {
    var travelDistanceInput by remember { mutableStateOf("") }
    var stayDaysInput by remember { mutableStateOf("") }
    
    // Unit state: 0 = Kilometers, 1 = Miles, 2 = Farsakh
    var selectedUnitIndex by remember { mutableIntStateOf(0) }
    val units = listOf("Kilometers (km)", "Miles (mi)", "Farsakh")

    var expandedUnitSelect by remember { mutableStateOf(false) }
    var isPurposeLawful by remember { mutableStateOf(true) }

    val distance = travelDistanceInput.toDoubleOrNull() ?: 0.0
    val days = stayDaysInput.toIntOrNull() ?: 0

    // Shia (Fiqh-e-Ja'fari) Travel Qasr rules:
    // Minimum round travel distance: 8 Farsakh
    // 8 Farsakh is exactly:
    // - 44 Kilometers (if single trip or round trip - must exceed 22km one-way)
    // - 27.5 Miles
    // If distance >= minimum AND stay is < 10 days AND purpose is lawful (not a journey for sin):
    // Prayer becomes QSAR (Shortened), Fasting becomes VOID (must make up later).
    
    val minDistanceKm = 44.0
    val minDistanceMi = 27.5
    val minDistanceFarsakh = 8.0

    val actualDistanceInKm = when (selectedUnitIndex) {
        0 -> distance
        1 -> distance * 1.60934
        else -> distance * 5.5 // 1 Farsakh is approx 5.5 km
    }

    val exceedsDistanceLimit = when (selectedUnitIndex) {
        0 -> distance >= minDistanceKm
        1 -> distance >= minDistanceMi
        else -> distance >= minDistanceFarsakh
    }

    val staysLessThan10Days = days < 10

    val qualifiesForQasr = exceedsDistanceLimit && staysLessThan10Days && isPurposeLawful

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Travel Rule",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Musafir (Traveler) Shia Rules:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Under Fiqh-e-Ja'fari, a traveler who journeys 8 Farsakh (~44 km or 27.5 mi) either as a single trip or combined roundtrip must shorten 4-unit prayers to 2-units (Qasr) and excuse fasting, provided their stay is under 10 days and journey is halal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = travelDistanceInput,
                onValueChange = { travelDistanceInput = it },
                label = { Text("Total Distance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.5f)
            )

            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedTextField(
                    value = units[selectedUnitIndex],
                    onValueChange = {},
                    label = { Text("Unit") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expandedUnitSelect = true }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Select Unit", modifier = Modifier.size(16.dp))
                        }
                    }
                )
                DropdownMenu(
                    expanded = expandedUnitSelect,
                    onDismissRequest = { expandedUnitSelect = false }
                ) {
                    units.forEachIndexed { idx, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedUnitIndex = idx
                                expandedUnitSelect = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = stayDaysInput,
            onValueChange = { stayDaysInput = it },
            label = { Text("Intended Stay (Days)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { isPurposeLawful = !isPurposeLawful }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Halal / Livelihood Purpose Journey", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (isPurposeLawful) "Lawful journey" else "Journey of sin (prayers are completely complete - Tamam)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isPurposeLawful,
                onCheckedChange = { isPurposeLawful = it }
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (qualifiesForQasr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Travel Calculator Verdict:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (qualifiesForQasr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Distance Met (>44 km):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (exceedsDistanceLimit) "Yes (${String.format("%.1f", actualDistanceInKm)} km)" else "No (${String.format("%.1f", actualDistanceInKm)} km)",
                        fontWeight = FontWeight.Bold,
                        color = if (exceedsDistanceLimit) Color.Green else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stay is under 10 Days:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (staysLessThan10Days) "Yes ($days days)" else "No ($days days)",
                        fontWeight = FontWeight.Bold,
                        color = if (staysLessThan10Days) Color.Green else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Halal Purpose Journey:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (isPurposeLawful) "Yes" else "No",
                        fontWeight = FontWeight.Bold,
                        color = if (isPurposeLawful) Color.Green else Color.Red
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column {
                    Text("Prayers Status during Journey:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = if (qualifiesForQasr) "QSAR (Shortened to 2 Rak'at)" else "TAMAM (Complete 4 Rak'at)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (qualifiesForQasr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Fasting Requirement:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = if (qualifiesForQasr) "EXEMPTED / VOID (Must keep as Qaza later)" else "OBLIGATORY (Must complete fast)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (qualifiesForQasr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// ==========================================
// Islamic Inheritance Calculator (Al-Fara'id)
// ==========================================

data class HeirShare(
    val relationship: String,
    val count: Int,
    val fraction: String,
    val percentage: Double,
    val value: Double,
    val note: String
)

@Composable
fun InheritanceTab(languageCode: String, currencySymbol: String) {
    var estateInput by remember { mutableStateOf("100000") }
    var selectedSpouseIndex by remember { mutableIntStateOf(0) } // 0 = None, 1 = Husband, 2 = Wife
    val spouses = listOf("None/Unmarried", "Husband", "Wife")

    // Class 1 State
    var hasFather by remember { mutableStateOf(false) }
    var hasMother by remember { mutableStateOf(false) }
    var hasHajbSiblings by remember { mutableStateOf(false) }
    var numSons by remember { mutableStateOf(0) }
    var numDaughters by remember { mutableStateOf(0) }

    // Class 2 State
    var hasPaternalGrandfather by remember { mutableStateOf(false) }
    var hasPaternalGrandmother by remember { mutableStateOf(false) }
    var hasMaternalGrandfather by remember { mutableStateOf(false) }
    var hasMaternalGrandmother by remember { mutableStateOf(false) }
    var numFullBrothers by remember { mutableStateOf(0) }
    var numFullSisters by remember { mutableStateOf(0) }
    var numMaternalSiblings by remember { mutableStateOf(0) }

    val totalEstate = estateInput.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?: 0.0

    val hasClass1 = hasFather || hasMother || numSons > 0 || numDaughters > 0

    val spouseType = when (selectedSpouseIndex) {
        1 -> "Husband"
        2 -> "Wife"
        else -> "None"
    }

    val shares = remember(
        totalEstate, spouseType, hasFather, hasMother, hasHajbSiblings, numSons, numDaughters,
        hasPaternalGrandfather, hasPaternalGrandmother, hasMaternalGrandfather, hasMaternalGrandmother,
        numFullBrothers, numFullSisters, numMaternalSiblings
    ) {
        calculateJafariInheritance(
            totalEstate = totalEstate,
            spouseType = spouseType,
            hasFather = hasFather,
            hasMother = hasMother,
            hasHajbSiblings = hasHajbSiblings,
            numSons = numSons,
            numDaughters = numDaughters,
            hasPaternalGrandfather = hasPaternalGrandfather,
            hasPaternalGrandmother = hasPaternalGrandmother,
            hasMaternalGrandfather = hasMaternalGrandfather,
            hasMaternalGrandmother = hasMaternalGrandmother,
            numFullBrothers = numFullBrothers,
            numFullSisters = numFullSisters,
            numMaternalSiblings = numMaternalSiblings
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (languageCode) {
                    "ur" -> "یہ صرف تعلیمی تخمینہ ہے۔ قرض، وصیت، عول، رد اور مرجع کے احکام وراثت بدل سکتے ہیں۔ تقسیم سے پہلے مستند شیعہ عالم سے تصدیق کرائیں۔"
                    "ar" -> "هذا تقدير تعليمي فقط. قد تؤثر الديون والوصية والعول والرد وفتوى المرجع في الإرث. تحقق من عالم شيعي مؤهل قبل التوزيع."
                    else -> "Educational estimate only. Debts, wills, awl, radd, and marja-specific rulings can affect inheritance. Verify with a qualified Shia scholar before distribution."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Inheritance Info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Shia Ja'fari Law of Inheritance (Al-Fara'id)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Fiqh-e-Ja'fari categorizes kinship into three exclusive Classes. Preceding classes completely exclude subsequent classes. Spouses are never excluded from inheriting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Net Estate Value input
        OutlinedTextField(
            value = estateInput,
            onValueChange = { estateInput = it },
            label = { Text("Net Distributable Estate Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("$currencySymbol ") },
            modifier = Modifier.fillMaxWidth()
        )

        // Spouse Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Surviving Spouse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                spouses.forEachIndexed { index, text ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedSpouseIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedSpouseIndex = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = if (selectedSpouseIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Class 1 Heirs Accordion
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Class 1: Parents & Children",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { hasFather = !hasFather }
                    ) {
                        Checkbox(checked = hasFather, onCheckedChange = { hasFather = it })
                        Text("Father", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { hasMother = !hasMother }
                    ) {
                        Checkbox(checked = hasMother, onCheckedChange = { hasMother = it })
                        Text("Mother", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (hasMother) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasHajbSiblings = !hasHajbSiblings }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = hasHajbSiblings, onCheckedChange = { hasHajbSiblings = it })
                        Column {
                            Text("Has Sibling Hajb (2+ Brothers etc.)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Reduces mother's share from 1/3 to 1/6 when no children", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Children counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sons", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (numSons > 0) numSons-- }) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            numSons.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { numSons++ }) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daughters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (numDaughters > 0) numDaughters-- }) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            numDaughters.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { numDaughters++ }) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Class 2 Heirs Accordion (Conditionally greyed or disabled)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasClass1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Class 2: Brothers, Sisters & Grandparents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasClass1) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
                    )
                    if (hasClass1) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("Excluded", modifier = Modifier.padding(4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (hasClass1) {
                    Text(
                        text = "⚠️ Class 1 heirs are selected above. Under Shia Ja'fari jurisprudence, Class 1 heirs completely exclude Class 2 members from inheriting. Reset Class 1 counts to calculate sibling/grandparent allocations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Activates because there are no parents or children present.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Fiqh Note: Under Ja'fari fiqh, Maternal Grandparents share their 1/3 portion equally (1:1). Paternal Grandfathers are NOT excluded by Full Brothers; they share the 2/3 paternal portion together based on a 2:1 male-to-female ratio.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Grandparents checkboxes
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hasPaternalGrandfather = !hasPaternalGrandfather }) {
                                Checkbox(checked = hasPaternalGrandfather, onCheckedChange = { hasPaternalGrandfather = it })
                                Text("Pat. Grandfather", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hasPaternalGrandmother = !hasPaternalGrandmother }) {
                                Checkbox(checked = hasPaternalGrandmother, onCheckedChange = { hasPaternalGrandmother = it })
                                Text("Pat. Grandmother", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hasMaternalGrandfather = !hasMaternalGrandfather }) {
                                Checkbox(checked = hasMaternalGrandfather, onCheckedChange = { hasMaternalGrandfather = it })
                                Text("Mat. Grandfather", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { hasMaternalGrandmother = !hasMaternalGrandmother }) {
                                Checkbox(checked = hasMaternalGrandmother, onCheckedChange = { hasMaternalGrandmother = it })
                                Text("Mat. Grandmother", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Sibling counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Paternal/Full Brothers", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Inherit 2/3 portion (double sister)", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (numFullBrothers > 0) numFullBrothers-- }) {
                                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                numFullBrothers.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { numFullBrothers++ }) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Paternal/Full Sisters", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Shared in 2/3 portion (single share)", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (numFullSisters > 0) numFullSisters-- }) {
                                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                numFullSisters.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { numFullSisters++ }) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Maternal (Uterine) Siblings", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Collectively share 1/3 split equally", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (numMaternalSiblings > 0) numMaternalSiblings-- }) {
                                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                numMaternalSiblings.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { numMaternalSiblings++ }) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Distribution Outputs Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Islamic Distribution Al-Fara'id Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (shares.isEmpty()) {
                    Text(
                        text = "Please select at least one eligible heir (Spouse, Parents, Children, Siblings or Grandparents) to initiate calculations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                } else {
                    // Visually graph the distribution using fractional colored bars
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Visual Split", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            val colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                                Color(0xFF00C853), // Green
                                Color(0xFFFF9100), // Orange
                                Color(0xFFD500F9)  // Violet
                            )
                            shares.forEachIndexed { sIdx, s ->
                                val weightValue = s.percentage.toFloat()
                                if (weightValue > 0.1f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(weightValue)
                                            .fillMaxHeight()
                                            .background(colors.getOrElse(sIdx % colors.size) { MaterialTheme.colorScheme.primary }),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${String.format("%.0f", s.percentage)}%",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    // Detail list of shares
                    shares.forEach { s ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (s.count > 1) "${s.relationship} (x${s.count})" else s.relationship,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Fraction base: ${s.fraction}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currencySymbol${String.format("%,.2f", s.value)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${String.format("%.2f", s.percentage)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = s.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Sum confirmation
                    val totalCalculatedPercentage = shares.sumOf { it.percentage }
                    val totalDistributedValue = shares.sumOf { it.value }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Apportioned:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$currencySymbol${String.format("%,.2f", totalDistributedValue)} (${String.format("%.1f", totalCalculatedPercentage)}%)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// Shia Ja'fari inheritance calculator formula implementation
fun calculateJafariInheritance(
    totalEstate: Double,
    spouseType: String,
    hasFather: Boolean,
    hasMother: Boolean,
    hasHajbSiblings: Boolean,
    numSons: Int,
    numDaughters: Int,
    hasPaternalGrandfather: Boolean,
    hasPaternalGrandmother: Boolean,
    hasMaternalGrandfather: Boolean,
    hasMaternalGrandmother: Boolean,
    numFullBrothers: Int,
    numFullSisters: Int,
    numMaternalSiblings: Int
): List<HeirShare> {
    val results = mutableListOf<HeirShare>()
    if (totalEstate <= 0.0) return results

    val hasChildren = numSons > 0 || numDaughters > 0
    val hasClass1 = hasFather || hasMother || hasChildren

    // spouses inherit across all levels
    var spousePercentage = 0.0
    var spouseFraction = ""
    var spouseNote = ""

    if (spouseType == "Husband") {
        if (hasChildren) {
            spousePercentage = 25.0
            spouseFraction = "1/4"
            spouseNote = "Husband's share is 1/4 when there are children of the deceased."
        } else {
            spousePercentage = 50.0
            spouseFraction = "1/2"
            spouseNote = "Husband receives 1/2 because no children exist."
        }
        results.add(HeirShare("Husband", 1, spouseFraction, spousePercentage, totalEstate * (spousePercentage / 100.0), spouseNote))
    } else if (spouseType == "Wife") {
        if (hasChildren) {
            spousePercentage = 12.5
            spouseFraction = "1/8"
            spouseNote = "Wife gets 1/8 when children are alive."
        } else {
            spousePercentage = 25.0
            spouseFraction = "1/4"
            spouseNote = "Wife's portion is 1/4 because no children exist."
        }
        results.add(HeirShare("Wife", 1, spouseFraction, spousePercentage, totalEstate * (spousePercentage / 100.0), spouseNote))
    }

    val remainingPctAfterSpouse = 100.0 - spousePercentage

    if (hasClass1) {
        // Class 1 Calculation
        var mPct = 0.0
        var fPct = 0.0
        var mFrac = ""
        var fFrac = ""
        var mNote = ""
        var fNote = ""

        if (hasChildren) {
            // With children: Parents have fixed shares of 1/6 each
            if (hasMother) {
                mPct = 100.0 / 6.0
                mFrac = "1/6"
                mNote = "Mother receives a fixed 1/6 share when the deceased has children."
            }
            if (hasFather) {
                fPct = 100.0 / 6.0
                fFrac = "1/6"
                fNote = "Father receives a fixed 1/6 share when the deceased has children."
            }

            val fixedSumCombinedAndSpouse = spousePercentage + mPct + fPct
            val remainingForChildren = (100.0 - fixedSumCombinedAndSpouse).coerceAtLeast(0.0)

            if (numSons == 0 && numDaughters > 0) {
                // Girls only. In Ja'fari Shia Fiqh:
                // Fixed share of 1 daughter = 1/2 (50%). 2+ daughters = 2/3 (66.67%).
                val girlsFixedSharePct = if (numDaughters == 1) 50.0 else (200.0 / 3.0)
                
                if (fixedSumCombinedAndSpouse + girlsFixedSharePct > 100.0) {
                    // Deficit present. Deficit is borne ONLY by the daughters (No 'Awl in Shia law).
                    val daughterFinalCombinedPct = (100.0 - fixedSumCombinedAndSpouse).coerceAtLeast(0.0)
                    results.add(HeirShare(
                        relationship = "Daughter",
                        count = numDaughters,
                        fraction = if (numDaughters == 1) "1/2 (Deficit adjusted)" else "2/3 (Deficit adjusted)",
                        percentage = daughterFinalCombinedPct,
                        value = totalEstate * (daughterFinalCombinedPct / 100.0),
                        note = "Daughter(s) bear the deficit under Ja'fari rules. Parents & spouse have preferential fixed shares."
                    ))
                } else {
                    // There is surplus remaining (surplus = 100 - fixedSumCombinedAndSpouse - girlsFixedSharePct)
                    // Under Jafari law, this surplus is returned (Radd) to daughters, father, and mother (if mother no sister/brother block)
                    val dWeight = if (numDaughters == 1) 0.5 else (2.0 / 3.0)
                    val fWeight = if (hasFather) (1.0 / 6.0) else 0.0
                    val mWeight = if (hasMother && !hasHajbSiblings) (1.0 / 6.0) else 0.0
                    val totalWeight = dWeight + fWeight + mWeight

                    val finalAvailableClass1 = 100.0 - spousePercentage
                    val daughterFactor = dWeight / totalWeight
                    val fatherFactor = if (hasFather) (fWeight / totalWeight) else 0.0
                    val motherFactor = if (hasMother && !hasHajbSiblings) (mWeight / totalWeight) else 0.0

                    val finalGirlsPct = finalAvailableClass1 * daughterFactor
                    results.add(HeirShare(
                        relationship = "Daughter",
                        count = numDaughters,
                        fraction = "Fixed + Radd",
                        percentage = finalGirlsPct,
                        value = totalEstate * (finalGirlsPct / 100.0),
                        note = "Daughters receive their Quranic portion plus a proportional Radd (refund) of surplus."
                    ))

                    if (hasFather) {
                        fPct = finalAvailableClass1 * fatherFactor
                        fFrac = "1/6 + Radd"
                        fNote = "Father receives fixed 1/6 portion plus proportional Radd (surplus) refund."
                    }
                    if (hasMother) {
                        if (!hasHajbSiblings) {
                            mPct = finalAvailableClass1 * motherFactor
                            mFrac = "1/6 + Radd"
                            mNote = "Mother receives fixed 1/6 portion plus proportional Radd (surplus) refund."
                        } else {
                            mPct = 100.0 / 6.0
                            mFrac = "1/6"
                            mNote = "Mother gets 1/6. She is excluded from receiving Radd (surplus) by sibling block."
                        }
                    }
                }
            } else {
                // There are sons (and possibly daughters).
                // They share the remaining as residuaries at a 2:1 ratio (no other fixed children shares apply)
                val sonsWeightFactors = numSons * 2.0
                val girlsWeightFactors = numDaughters * 1.0
                val combinedFactors = sonsWeightFactors + girlsWeightFactors

                val unitSlice = remainingForChildren / combinedFactors
                if (numSons > 0) {
                    val combinedSonsPct = unitSlice * 2.0 * numSons
                    results.add(HeirShare(
                        relationship = "Son",
                        count = numSons,
                        fraction = "Residuary (2x ratio)",
                        percentage = combinedSonsPct,
                        value = totalEstate * (combinedSonsPct / 100.0),
                        note = "Sons inherit as residuaries with double the portion of daughters."
                    ))
                }
                if (numDaughters > 0) {
                    val combinedGirlsPct = unitSlice * numDaughters
                    results.add(HeirShare(
                        relationship = "Daughter",
                        count = numDaughters,
                        fraction = "Residuary (1x ratio)",
                        percentage = combinedGirlsPct,
                        value = totalEstate * (combinedGirlsPct / 100.0),
                        note = "Daughters inherit as residuaries alongside sons (1x daughter ratio)."
                    ))
                }
            }

            if (hasMother) {
                results.add(HeirShare("Mother", 1, mFrac, mPct, totalEstate * (mPct / 100.0), mNote))
            }
            if (hasFather) {
                results.add(HeirShare("Father", 1, fFrac, fPct, totalEstate * (fPct / 100.0), fNote))
            }

        } else {
            // No children. Only parents exist in Class 1.
            if (hasMother) {
                if (hasHajbSiblings) {
                    mPct = 100.0 / 6.0
                    mFrac = "1/6"
                    mNote = "Mother's portion is capped to 1/6 due to blocking (Hajb) by sibling presence."
                } else {
                    mPct = 100.0 / 3.0
                    mFrac = "1/3"
                    mNote = "Mother receives her full default Quranic 1/3 share because there are no child/sibling blocks."
                }
            }

            if (hasFather) {
                // Father takes all remaining
                val fRemainingPct = (100.0 - spousePercentage - mPct).coerceAtLeast(0.0)
                results.add(HeirShare("Father", 1, "Residuary", fRemainingPct, totalEstate * (fRemainingPct / 100.0), "Father inherits all remaining residue in Class 1."))
            } else {
                // Only mother exists in Class 1 (besides spouse)
                // She gets her share + the rest is returned (Radd) to her
                val finalMotherPct = 100.0 - spousePercentage
                mPct = finalMotherPct
                mFrac = "1/3 + Radd"
                mNote = "Mother receives entire remaining available estate since she is the sole Class 1 heir."
            }

            if (hasMother) {
                results.add(HeirShare("Mother", 1, mFrac, mPct, totalEstate * (mPct / 100.0), mNote))
            }
        }
    } else {
        // Class 2 Calculation (Only if NO Class 1 Heirs present)
        val hasMaternalClass2 = hasMaternalGrandfather || hasMaternalGrandmother || numMaternalSiblings > 0
        val hasPaternalClass2 = hasPaternalGrandfather || hasPaternalGrandmother || numFullBrothers > 0 || numFullSisters > 0

        var maternalSideTotalPct = 0.0
        var paternalSideTotalPct = 0.0

        if (hasMaternalClass2 && hasPaternalClass2) {
            // Maternal portion gets 1/3 of the available balance, Paternal gets the remaining 2/3
            maternalSideTotalPct = remainingPctAfterSpouse / 3.0
            paternalSideTotalPct = (remainingPctAfterSpouse * 2.0) / 3.0
        } else if (hasMaternalClass2) {
            maternalSideTotalPct = remainingPctAfterSpouse
        } else if (hasPaternalClass2) {
            paternalSideTotalPct = remainingPctAfterSpouse
        }

        // Divide Maternal Side (equally between all maternal claimants)
        if (hasMaternalClass2) {
            val totalMaternalHeirCount = (if (hasMaternalGrandfather) 1 else 0) + (if (hasMaternalGrandmother) 1 else 0) + numMaternalSiblings
            if (totalMaternalHeirCount > 0) {
                val shareSlice = maternalSideTotalPct / totalMaternalHeirCount
                if (hasMaternalGrandfather) {
                    results.add(HeirShare("Maternal Grandfather", 1, "Equally Split Maternal (1/3)", shareSlice, totalEstate * (shareSlice / 100.0), "Maternal grandfather inherits equal slice of maternal portion."))
                }
                if (hasMaternalGrandmother) {
                    results.add(HeirShare("Maternal Grandmother", 1, "Equally Split Maternal (1/3)", shareSlice, totalEstate * (shareSlice / 100.0), "Maternal grandmother inherits equal slice of maternal portion."))
                }
                if (numMaternalSiblings > 0) {
                    results.add(HeirShare("Maternal (Uterine) Sibling", numMaternalSiblings, "Equally Split Maternal (1/3)", shareSlice * numMaternalSiblings, totalEstate * ((shareSlice * numMaternalSiblings) / 100.0), "Maternal/Uterine sibling(s) split their shared maternal allocation equally."))
                }
            }
        }

        // Divide Paternal Side (using male:female 2:1 ratio)
        if (hasPaternalClass2) {
            val malesCount = (if (hasPaternalGrandfather) 1 else 0) + numFullBrothers
            val femalesCount = (if (hasPaternalGrandmother) 1 else 0) + numFullSisters
            val totalPaternalShares = (malesCount * 2.0) + (femalesCount * 1.0)
            
            if (totalPaternalShares > 0) {
                val singleUnitPct = paternalSideTotalPct / totalPaternalShares

                if (hasPaternalGrandfather) {
                    val grandFPercent = singleUnitPct * 2.0
                    results.add(HeirShare("Paternal Grandfather", 1, "Paternal 2/3 (Double ratio)", grandFPercent, totalEstate * (grandFPercent / 100.0), "Paternal Grandfather inherits double sister ratio in paternal assets."))
                }
                if (hasPaternalGrandmother) {
                    results.add(HeirShare("Paternal Grandmother", 1, "Paternal 2/3 (Single ratio)", singleUnitPct, totalEstate * (singleUnitPct / 100.0), "Paternal Grandmother inherits single ratio in paternal assets."))
                }
                if (numFullBrothers > 0) {
                    val combinedBrotherPct = singleUnitPct * 2.0 * numFullBrothers
                    results.add(HeirShare("Full/Paternal Brother", numFullBrothers, "Paternal 2/3 (Double ratio)", combinedBrotherPct, totalEstate * (combinedBrotherPct / 100.0), "Paternal brothers inherit with double ratio split."))
                }
                if (numFullSisters > 0) {
                    val combinedSisterPct = singleUnitPct * numFullSisters
                    results.add(HeirShare("Full/Paternal Sister", numFullSisters, "Paternal 2/3 (Single ratio)", combinedSisterPct, totalEstate * (combinedSisterPct / 100.0), "Paternal sisters inherit with single ratio split."))
                }
            }
        }
    }

    return results
}

@Composable
fun UsharTab(languageCode: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var cropYieldInput by remember { mutableStateOf("") }
    var cropPriceInput by remember { mutableStateOf("") }
    
    val irrigationOptions = listOf(
        "natural" to (if (languageCode == "ur") "قدرتی آبپاشی (باران، دریا، چشمہ - %10)" else "Natural Irrigation (Rain, River - 10%)"),
        "artificial" to (if (languageCode == "ur") "مصنوعی آبپاشی (ٹوب ویل، کنواں - %5)" else "Artificial Irrigation (Well, Pump - 5%)"),
        "mixed" to (if (languageCode == "ur") "مخلوط آبپاشی (دونوں ذرائع - %7.5)" else "Mixed Irrigation (Both sources - 7.5%)")
    )
    
    var selectedOption by remember { mutableStateOf("natural") }
    
    val cropYield = cropYieldInput.toDoubleOrNull() ?: 0.0
    val cropPrice = cropPriceInput.toDoubleOrNull() ?: 0.0
    
    val usharNisabKg = 847.2
    val reachedNisab = cropYield >= usharNisabKg
    
    val rate = when (selectedOption) {
        "natural" -> 0.10
        "artificial" -> 0.05
        "mixed" -> 0.075
        else -> 0.10
    }
    
    val dueWeight = if (reachedNisab) cropYield * rate else 0.0
    val dueValue = dueWeight * cropPrice
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (languageCode == "ur") 
                    "دستبرداری: یہ کیلکولیٹر فقہ جعفریہ کے مطابق عمومی رہنمائی فراہم کرتا ہے۔ دیگر مراجع کی آراء مختلف ہو سکتی ہیں۔ حتمی شرعی حکم کے لیے اپنے مرجعِ تقلید سے رجوع کریں۔" 
                    else "Disclaimer: This tool is for educational purposes under Shia Ja'fari Fiqh. Rulings can vary by Marja' (e.g. Ayatollah Sistani, Khamenei). For binding verdicts, please consult your religious authority.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = "Crop Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (languageCode == "ur") "عشر کیا ہے؟ (زرعی زکوٰۃ)" else "What is Ushar? (Agricultural Zakat)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (languageCode == "ur") 
                            "فقہِ جعفریہ میں عشر صرف چار اجناس پر معین شرائط کے ساتھ واجب ہے: گندم، جو، کھجور اور کشمش۔ نصاب 5 وسق (تقریباً 847.2 کلوگرام) ہے۔ اس سے کم مقدار پر عشر واجب نہیں ہے۔ پیداواری اخراجات منہا کرنے کی اجازت ہے۔" 
                            else "In Shia Ja'fari Fiqh, crop Zakat is obligatory (Wajib) only on 4 items: Wheat, Barley, Dates, and Raisins. The Nisab threshold is 5 Wasaq (approx. 847.2 kg). Deductions for farming costs are allowed before calculating the final tax weight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedTextField(
            value = cropYieldInput,
            onValueChange = { cropYieldInput = it },
            label = { Text(if (languageCode == "ur") "کل پیداوار کا وزن (کلوگرام میں)" else "Total Crop Yield (in Kilograms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("kg") }
        )

        OutlinedTextField(
            value = cropPriceInput,
            onValueChange = { cropPriceInput = it },
            label = { Text(if (languageCode == "ur") "مارکیٹ فی کلو قیمت (اختیاری)" else "Market Price per kg (Optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("$currencySymbol ") }
        )

        Text(
            text = if (languageCode == "ur") "آبپاشی کا طریقہ کار منتخب کریں:" else "Select Irrigation Method:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            irrigationOptions.forEach { (option, label) ->
                val isSelected = selectedOption == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                        .clickable { selectedOption = option }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedOption = option }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (languageCode == "ur") "عشر کا حساب کتاب:" else "Ushar Assessment:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "حدِ نصاب (847.2 کلوگرام):" else "Nisab (847.2 kg):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (reachedNisab) 
                            (if (languageCode == "ur") "پورا ہو گیا (واجب)" else "Nisab Reached (Wajib)") 
                            else (if (languageCode == "ur") "کم ہے (مستحب)" else "Below Nisab (Mustahab)"),
                        fontWeight = FontWeight.Bold,
                        color = if (reachedNisab) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "قابلِ ٹیکس ریٹ:" else "Applied Rate:", style = MaterialTheme.typography.bodyMedium)
                    Text("${rate * 100}%", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "واجب عشر کا وزن:" else "Due Ushar Weight:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2f kg", dueWeight), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                if (cropPrice > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (languageCode == "ur") "واجب عشر کی نقدی مالیت:" else "Monetary Due Value:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$currencySymbol${String.format("%.2f", dueValue)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MehrTab(languageCode: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var priceInput by remember { mutableStateOf("0.90") }
    var priceUnit by remember { mutableStateOf("gram") }
    
    val enteredPrice = priceInput.toDoubleOrNull() ?: 0.90
    
    val pricePerGram = when (priceUnit) {
        "gram" -> enteredPrice
        "tola" -> enteredPrice / 11.6638
        "10g" -> enteredPrice / 10.0
        else -> enteredPrice
    }
    
    val silverWeightGrams = 1250.0
    val totalValue = silverWeightGrams * pricePerGram
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (languageCode == "ur") 
                    "دستبرداری: یہ کیلکولیٹر فقہ جعفریہ کے مطابق عمومی معلومات فراہم کرتا ہے۔ مہر کا حتمی فیصلہ فریقین کی باہمی رضامندی اور عاقد کے مشورے پر منحصر ہے۔" 
                    else "Disclaimer: This tool calculates Mehr al-Sunnah weight based on classic Shia Jafari sources. Actual Mehr amount depends completely on mutual family consent. Consult a qualified scholar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = Icons.Default.BrightnessHigh,
                    contentDescription = "Mehr Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (languageCode == "ur") "مہرِ فاطمیؑ (مہرِ سنت) کیا ہے؟" else "What is Mehr al-Sunnah (Fatimid Mehr)?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (languageCode == "ur") 
                            "رسول اللہ ص نے اپنی چہیتی صاحبزادی حضرت فاطمہ زہراؑ کا مہرِ مبارک 500 درہمِ چاندی مقرر فرمایا تھا، جس کا وزن 1250 گرام (سوا کلو) خالص چاندی بنتا ہے۔ یہ شادی میں سادگی اور معنوی برکت کی عظیم مثال ہے۔" 
                            else "The Prophet (S) established the Mehr (dowry) of his daughter Sayyidah Fatima al-Zahra (sa) to Imam Ali (as) at 500 Dirhams of silver, which computes precisely to 1250 Grams (1.25 Kilograms) of pure silver.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = if (languageCode == "ur") "چاندی کا مقامی مارکیٹ ریٹ درج کریں:" else "Enter Silver Market Price:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = priceInput,
                onValueChange = { priceInput = it },
                label = { Text(if (languageCode == "ur") "قیمت" else "Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.2f),
                prefix = { Text("$currencySymbol ") }
            )
            
            var unitExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(0.8f)) {
                OutlinedButton(
                    onClick = { unitExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (priceUnit) {
                            "gram" -> if (languageCode == "ur") "فی گرام" else "per Gram"
                            "tola" -> if (languageCode == "ur") "فی تولہ" else "per Tola"
                            "10g" -> if (languageCode == "ur") "فی 10 گرام" else "per 10g"
                            else -> "Unit"
                        },
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (languageCode == "ur") "فی گرام" else "per Gram") },
                        onClick = { priceUnit = "gram"; unitExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(if (languageCode == "ur") "فی تولہ (11.66g)" else "per Tola (11.66g)") },
                        onClick = { priceUnit = "tola"; unitExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(if (languageCode == "ur") "فی 10 گرام" else "per 10g") },
                        onClick = { priceUnit = "10g"; unitExpanded = false }
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (languageCode == "ur") "مہرِ فاطمیؑ کی کل شرعی مالیت:" else "Mehr al-Sunnah Value:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "مجموعی چاندی کا وزن:" else "Total Silver Weight:", style = MaterialTheme.typography.bodyMedium)
                    Text("1250 Grams (~107.2 Tolas)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "حسابی قیمت فی گرام:" else "Computed Price per Gram:", style = MaterialTheme.typography.bodyMedium)
                    Text("$currencySymbol${String.format("%.2f", pricePerGram)}", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (languageCode == "ur") "مہرِ سنت متبادل نقدی قیمت:" else "Total Mehr al-Sunnah Value:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$currencySymbol${String.format("%,.2f", totalValue)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (languageCode == "ur") "سیدہ کائناتؑ کا مبارک جہیز و مہر" else "The Blessed Household & Dowry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (languageCode == "ur")
                        "امام علیؑ نے اپنا ایک زرہ بیچ کر 500 درہم جمع کیے تھے۔ آپ ص نے اسے تین حصوں میں بانٹا: ایک عطر و خوشبو، ایک سادگی سے بنے ہوئے بستر اور مٹی کے لوندے، اور ایک حصہ دیگر اخراجات کے لیے۔ مہر میاں بیوی کے درمیان الفت اور تقدس پیدا کرنے کے لیے ہے، نہ کہ نمود و نمائش کا ذریعہ۔"
                        else "To establish the marriage, Imam Ali (as) sold his shield for 500 Dirhams. The Prophet ( السالم ) divided this modest sum into parts: one part for cosmetics/spices, another for essential household items (earthware water jugs, hand-mills, and goatskin pillows), and another reserved for basic expenses, demonstrating a life rich in spirituality and simple in material needs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}





@Composable
fun SadaqahTrackerTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    val financeTransactions by viewModel.allFinanceTransactions.collectAsState()
    val totalSadaqah = com.example.data.calculateTransactionTotal(
        transactions = financeTransactions,
        type = "Sadaqah",
        currency = currencySymbol
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (currentLang) { "ur" -> "صدقہ بلاؤں کو ٹالتا ہے۔ اپنے روزمرہ کے صدقے کا حساب یہاں رکھیں۔"; "ar" -> "الصدقة تدفع البلاء. سجل صدقاتك اليومية والعرضية هنا."; else -> "Sadaqah repels calamities. Track your daily or occasional Sadaqah here." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text(when (currentLang) { "ur" -> "رقم ($currencySymbol)"; "ar" -> "المبلغ ($currencySymbol)"; else -> "Amount ($currencySymbol)" }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text(when (currentLang) { "ur" -> "تفصیل (اختیاری)"; "ar" -> "ملاحظة (اختياري)"; else -> "Note (Optional)" }) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        

        Button(
            onClick = {
                val amount = amountInput.toDoubleOrNull() ?: 0.0
                if (amount.isFinite() && amount > 0) {
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val today = format.format(java.util.Date())
                    viewModel.insertFinanceTransaction(
                        com.example.data.FinanceTransaction(
                            dateString = today,
                            type = "Sadaqah",
                            amount = amount,
                            currency = currencySymbol,
                            notes = noteInput.ifBlank {
                                when (currentLang) {
                                    "ur" -> "روزانہ صدقہ"
                                    "ar" -> "صدقة يومية"
                                    else -> "Daily Sadaqah"
                                }
                            }
                        )
                    )
                    amountInput = ""
                    noteInput = ""
                }
            },

            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(when (currentLang) { "ur" -> "صدقہ شامل کریں"; "ar" -> "إضافة صدقة"; else -> "Add Sadaqah" })
        }
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (currentLang) { "ur" -> "کل صدقہ"; "ar" -> "إجمالي الصدقة"; else -> "Total Sadaqah" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$currencySymbol ${"%.2f".format(totalSadaqah)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DonationHistoryTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
    val transactions by viewModel.allFinanceTransactions.collectAsState()
    val donations = transactions.filter {
        it.type.equals("Donation", ignoreCase = true) || it.type.equals("Sadaqah", ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = when (currentLang) {
                    "ur" -> "آپ کے عطیات"
                    "ar" -> "تبرعاتك"
                    else -> "Your Donations"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (donations.isEmpty()) {
            item {
                Text(
                    text = when (currentLang) {
                        "ur" -> "ابھی کوئی عطیہ درج نہیں ہے۔"
                        "ar" -> "لا توجد تبرعات مسجلة بعد."
                        else -> "No donations have been recorded yet."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(donations, key = { it.id }) { donation ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = financeTypeLabel(donation.type, currentLang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = donation.dateString,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (donation.notes.isNotBlank()) {
                                Text(
                                    text = donation.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "${donation.currency.ifBlank { currencySymbol }} ${"%.2f".format(donation.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QurbaniCalculatorTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var sharePriceInput by remember { mutableStateOf("") }
    var sharesCountInput by remember { mutableStateOf("1") }
    
    val sharePrice = sharePriceInput.toDoubleOrNull() ?: 0.0
    val sharesCount = sharesCountInput.toIntOrNull() ?: 0
    val totalCost = (sharePrice * sharesCount).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    val canSave = sharePrice.isFinite() && sharePrice > 0.0 && sharesCount > 0 && totalCost > 0.0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (currentLang) { "ur" -> "بڑے جانور (گائے، اونٹ) میں 7 حصے ہوتے ہیں، جبکہ چھوٹے جانور (بکرا، بھیڑ) کا 1 حصہ ہوتا ہے۔"; "ar" -> "للحيوان الكبير (البقرة والجمل) 7 حصص، وللحيوان الصغير (الماعز والخروف) حصة واحدة."; else -> "A large animal (cow, camel) has 7 shares, while a small animal (goat, sheep) equals 1 share." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        
        OutlinedTextField(
            value = sharePriceInput,
            onValueChange = { sharePriceInput = it },
            label = { Text(when (currentLang) { "ur" -> "فی حصہ قیمت ($currencySymbol)"; "ar" -> "سعر الحصة ($currencySymbol)"; else -> "Price per share ($currencySymbol)" }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        OutlinedTextField(
            value = sharesCountInput,
            onValueChange = { sharesCountInput = it },
            label = { Text(when (currentLang) { "ur" -> "حصوں کی تعداد"; "ar" -> "عدد الحصص"; else -> "Number of shares" }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (currentLang) { "ur" -> "کل تخمینہ"; "ar" -> "التقدير الإجمالي"; else -> "Total Estimate" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$currencySymbol ${"%.2f".format(totalCost)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(
            onClick = {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = format.format(java.util.Date())
                viewModel.insertFinanceTransaction(
                    com.example.data.FinanceTransaction(
                        dateString = today,
                        type = "Qurbani",
                        amount = totalCost,
                        currency = currencySymbol,
                        notes = when (currentLang) { "ur" -> "حصے: $sharesCount"; "ar" -> "الحصص: $sharesCount"; else -> "Shares: $sharesCount" }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(when (currentLang) { "ur" -> "ہسٹری میں محفوظ کریں"; "ar" -> "حفظ في السجل"; else -> "Save to History" })
        }
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FinanceSettingsTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
    val currencyCode by viewModel.currencyCode.collectAsState()
    val marja by viewModel.marja.collectAsState()
    val autoRefresh by viewModel.autoRefreshPrices.collectAsState()
    val goldPrice by viewModel.goldPriceGram.collectAsState()
    val silverPrice by viewModel.silverPriceGram.collectAsState()
    val lastUpdated by viewModel.financePricesLastUpdated.collectAsState()
    val loading by viewModel.financePricesLoading.collectAsState()
    val errorMsg by viewModel.financePricesError.collectAsState()
    
    var expandedCurrency by remember { mutableStateOf(false) }
    var expandedMarja by remember { mutableStateOf(false) }
    
    val currencies = listOf("usd" to "$", "pkr" to "Rs", "eur" to "€", "gbp" to "£", "aed" to "د.إ", "sar" to "ر.س", "iqd" to "ع.د", "irr" to "﷼", "try" to "₺")
    val marjas = listOf("Ayatollah Sistani", "Ayatollah Khamenei", "Ayatollah Makarem Shirazi", "Ayatollah Wahid Khorasani", "Ayatollah Fadlallah")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (currentLang) { "ur" -> "فنانس کی ترتیبات"; "ar" -> "الإعدادات المالية"; else -> "Finance Settings" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Currency Selection
        ExposedDropdownMenuBox(
            expanded = expandedCurrency,
            onExpandedChange = { expandedCurrency = !expandedCurrency }
        ) {
            OutlinedTextField(
                value = currencyCode.uppercase(),
                onValueChange = {},
                readOnly = true,
                label = { Text(if (currentLang == "ur") "کرنسی" else "Default Currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expandedCurrency,
                onDismissRequest = { expandedCurrency = false }
            ) {
                currencies.forEach { (code, symbol) ->
                    DropdownMenuItem(
                        text = { Text("${code.uppercase()} ($symbol)") },
                        onClick = {
                            viewModel.setCurrencyCode(code)
                            viewModel.setCurrencySymbol(symbol)
                            expandedCurrency = false
                        }
                    )
                }
            }
        }
        
        // Marja Selection
        ExposedDropdownMenuBox(
            expanded = expandedMarja,
            onExpandedChange = { expandedMarja = !expandedMarja }
        ) {
            OutlinedTextField(
                value = marja,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (currentLang == "ur") "مرجع تقلید" else "Marja-e-Taqlid") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMarja) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expandedMarja,
                onDismissRequest = { expandedMarja = false }
            ) {
                marjas.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m) },
                        onClick = {
                            viewModel.setMarja(m)
                            expandedMarja = false
                        }
                    )
                }
            }
        }
        
        // Auto Refresh
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (currentLang == "ur") "قیمتوں کا خودکار ریفریش" else "Auto Refresh Prices", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = autoRefresh,
                onCheckedChange = { viewModel.setAutoRefreshPrices(it) }
            )
        }
        
        HorizontalDivider()
        
        // Live Prices Info
        Text(
            text = if (currentLang == "ur") "موجودہ قیمتیں" else "Current Market Prices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gold / gram:", fontWeight = FontWeight.Bold)
                        Text(if (goldPrice != null) "$currencySymbol ${"%.2f".format(goldPrice)}" else "N/A")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Silver / gram:", fontWeight = FontWeight.Bold)
                        Text(if (silverPrice != null) "$currencySymbol ${"%.2f".format(silverPrice)}" else "N/A")
                    }
                    if (lastUpdated > 0) {
                        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                        Text("Last Updated: ${dateFormat.format(java.util.Date(lastUpdated))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    errorMsg?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.refreshFinancePrices() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (currentLang == "ur") "ریفریش" else "Refresh Now")
            }
            OutlinedButton(
                onClick = { viewModel.clearFinanceCache() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (currentLang == "ur") "کیش صاف کریں" else "Clear Cache")
            }
        }
    }
}

@Composable
fun FinanceHistoryTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
    val history by viewModel.allFinanceTransactions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filteredHistory = remember(history, searchQuery, selectedFilter) {
        history.filter { 
            (selectedFilter == "All" || it.type == selectedFilter) &&
            (it.notes.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true) || it.dateString.contains(searchQuery))
        }
    }
    
    val totalsByCurrency = remember(filteredHistory, currencySymbol) {
        com.example.data.calculateCurrencyTotals(filteredHistory, currencySymbol)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (currentLang) { "ur" -> "فنانس کی تاریخ"; "ar" -> "السجل المالي"; else -> "Finance History" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(when (currentLang) { "ur" -> "تلاش کریں..."; "ar" -> "بحث..."; else -> "Search..." }) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Khums", "Zakat", "Fitrana", "Sadaqah", "Qurbani", "Donation")
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(financeTypeLabel(filter, currentLang)) }
                )
            }
        }
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "${when (currentLang) { "ur" -> "کل"; "ar" -> "المجموع"; else -> "Total" }} (${financeTypeLabel(selectedFilter, currentLang)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(horizontalAlignment = Alignment.End) {
                    if (totalsByCurrency.isEmpty()) {
                        Text(
                            "$currencySymbol 0.00",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        totalsByCurrency.forEach { (currency, total) ->
                            Text(
                                "$currency ${"%.2f".format(total)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        if (filteredHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(when (currentLang) { "ur" -> "کوئی مالی معاملہ نہیں ملا۔"; "ar" -> "لا توجد معاملات مالية."; else -> "No transactions found." }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filteredHistory, key = { it.id }) { tx ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = financeTypeLabel(tx.type, currentLang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tx.dateString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (tx.notes.isNotEmpty()) {
                                    Text(
                                        text = tx.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${tx.currency.uppercase()} ${"%.2f".format(tx.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.deleteFinanceTransaction(tx) }) {
                                    Icon(Icons.Default.Delete, contentDescription = when (currentLang) { "ur" -> "حذف کریں"; "ar" -> "حذف"; else -> "Delete" }, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun financeTypeLabel(type: String, languageCode: String): String = when (type) {
    "All" -> when (languageCode) { "ur" -> "تمام"; "ar" -> "الكل"; else -> "All" }
    "Khums" -> when (languageCode) { "ur" -> "خمس"; "ar" -> "الخمس"; else -> "Khums" }
    "Zakat" -> when (languageCode) { "ur" -> "زکوٰۃ"; "ar" -> "الزكاة"; else -> "Zakat" }
    "Fitrana" -> when (languageCode) { "ur" -> "فطرانہ"; "ar" -> "الفطرة"; else -> "Fitrana" }
    "Sadaqah" -> when (languageCode) { "ur" -> "صدقہ"; "ar" -> "الصدقة"; else -> "Sadaqah" }
    "Qurbani" -> when (languageCode) { "ur" -> "قربانی"; "ar" -> "الأضحية"; else -> "Qurbani" }
    "Donation" -> when (languageCode) { "ur" -> "عطیہ"; "ar" -> "التبرع"; else -> "Donation" }
    else -> type
}
