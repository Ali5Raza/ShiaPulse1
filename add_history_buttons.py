import re

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "r") as f:
    content = f.read()

# 1. Khums
khums_save_btn = """
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = format.format(java.util.Date())
                viewModel.insertFinanceTransaction(
                    com.example.data.FinanceTransaction(
                        dateString = today,
                        type = "Khums",
                        amount = khums,
                        currency = currencySymbol,
                        notes = "Saved from Khums Calculator"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (currentLang == "ur") "ہسٹری میں محفوظ کریں" else "Save to History")
        }
"""
content = content.replace('Text(if (currentLang == "ur") "نوٹ: یہ ایک بنیادی کیلکولیٹر ہے..." else if (currentLang == "ar") "ملاحظة: هذه حاسبة أساسية..." else "Note: This is a basic calculator...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))', 
                          khums_save_btn + '\n        Text(if (currentLang == "ur") "نوٹ: یہ ایک بنیادی کیلکولیٹر ہے..." else if (currentLang == "ar") "ملاحظة: هذه حاسبة أساسية..." else "Note: This is a basic calculator...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))')


# 2. Zakat
zakat_save_btn = """
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = format.format(java.util.Date())
                viewModel.insertFinanceTransaction(
                    com.example.data.FinanceTransaction(
                        dateString = today,
                        type = "Zakat",
                        amount = totalZakat,
                        currency = currencySymbol,
                        notes = "Saved from Zakat Calculator"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (languageCode == "ur") "ہسٹری میں محفوظ کریں" else "Save to History")
        }
"""
content = content.replace('Text(if (languageCode == "ur") "نوٹ: زکوٰۃ کے پیچیدہ مسائل کے لیے..." else if (languageCode == "ar") "ملاحظة: يرجى الرجوع..." else "Note: Please refer to your Marja for complex Zakat scenarios...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))', 
                          zakat_save_btn + '\n        Text(if (languageCode == "ur") "نوٹ: زکوٰۃ کے پیچیدہ مسائل کے لیے..." else if (languageCode == "ar") "ملاحظة: يرجى الرجوع..." else "Note: Please refer to your Marja for complex Zakat scenarios...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))')


# 3. Fitrana
fitrana_save_btn = """
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = format.format(java.util.Date())
                viewModel.insertFinanceTransaction(
                    com.example.data.FinanceTransaction(
                        dateString = today,
                        type = "Fitrana",
                        amount = totalFitrana,
                        currency = currencySymbol,
                        notes = "Saved from Fitrana Calculator"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (languageCode == "ur") "ہسٹری میں محفوظ کریں" else "Save to History")
        }
"""
content = content.replace('Text(if (languageCode == "ur") "نوٹ: فطرانہ عید الفطر کی نماز سے پہلے..." else if (languageCode == "ar") "ملاحظة: يجب دفع زكاة الفطرة..." else "Note: Fitrana must be paid before Eid al-Fitr prayers...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))', 
                          fitrana_save_btn + '\n        Text(if (languageCode == "ur") "نوٹ: فطرانہ عید الفطر کی نماز سے پہلے..." else if (languageCode == "ar") "ملاحظة: يجب دفع زكاة الفطرة..." else "Note: Fitrana must be paid before Eid al-Fitr prayers...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))')

# 4. SadaqahTracker
sadaqah_save_btn = """
        Button(
            onClick = {
                val amount = amountInput.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val today = format.format(java.util.Date())
                    viewModel.insertFinanceTransaction(
                        com.example.data.FinanceTransaction(
                            dateString = today,
                            type = "Sadaqah",
                            amount = amount,
                            currency = currencySymbol,
                            notes = noteInput.ifEmpty { "Daily Sadaqah" }
                        )
                    )
                    totalSadaqah += amount
                    amountInput = ""
                    noteInput = ""
                }
            },
"""
content = content.replace("""        Button(
            onClick = {
                val amount = amountInput.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    totalSadaqah += amount
                    amountInput = ""
                    noteInput = ""
                }
            },""", sadaqah_save_btn)


# 5. Qurbani
qurbani_save_btn = """
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
                        notes = "Shares: $sharesCount"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (currentLang == "ur") "ہسٹری میں محفوظ کریں" else "Save to History")
        }
    }
}
"""
content = content.replace("""        Card(
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
                        text = if (currentLang == "ur") "کل تخمینہ" else "Total Estimate",
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
    }
}""", """        Card(
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
                        text = if (currentLang == "ur") "کل تخمینہ" else "Total Estimate",
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
""" + qurbani_save_btn)

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "w") as f:
    f.write(content)

