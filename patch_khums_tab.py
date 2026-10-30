import re

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "r") as f:
    content = f.read()

khums_tab_start = 'fun KhumsTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {'
new_khums_tab = """fun KhumsTab(currentLang: String, currencySymbol: String, viewModel: PrayerViewModel) {
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
"""

content = content.replace(khums_tab_start, new_khums_tab)

target_info = 'Text(if (currentLang == "ur") "خمس ایک شرعی ٹیکس ہے..." else if (currentLang == "ar") "الخمس هو ضريبة..." else "Khums is an Islamic tax...", style = MaterialTheme.typography.bodyMedium)'

reminder_ui = """
        Spacer(modifier = Modifier.height(16.dp))
        
        // Khums Year Reminder Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (currentLang == "ur") "خمس سال کی یاد دہانی" else "Khums Year Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startDateInput,
                    onValueChange = { 
                        startDateInput = it 
                        viewModel.setKhumsYearStartDate(it) 
                    },
                    label = { Text(if (currentLang == "ur") "خمس سال شروع ہونے کی تاریخ (YYYY-MM-DD)" else "Khums Year Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (remainingDays != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentLang == "ur") "اگلے خمس کے واجب ہونے میں باقی دن: $remainingDays" else "Days remaining until next Khums: $remainingDays",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
"""

content = content.replace(target_info, target_info + reminder_ui)

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "w") as f:
    f.write(content)
