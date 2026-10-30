import re

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "r") as f:
    content = f.read()

# Replace the ZakatTab signature again just in case
# Find the start of ZakatTab
zakat_start = content.find("fun ZakatTab(")

# Let's just modify the ZakatTab implementation using python regex
old_zakat = """@Composable
fun ZakatTab(languageCode: String, currencySymbol: String, viewModel: PrayerViewModel) {
    var goldPriceGram by remember { mutableStateOf("0") }
    var silverPriceGram by remember { mutableStateOf("0") }
    var goldGrams by remember { mutableStateOf("") }
    var silverGrams by remember { mutableStateOf("") }"""

new_zakat = """@Composable
fun ZakatTab(languageCode: String, currencySymbol: String, viewModel: PrayerViewModel) {
    val liveGoldPrice by viewModel.goldPriceGram.collectAsState()
    val liveSilverPrice by viewModel.silverPriceGram.collectAsState()

    var goldPriceGram by remember(liveGoldPrice) { mutableStateOf(liveGoldPrice?.toString() ?: "0") }
    var silverPriceGram by remember(liveSilverPrice) { mutableStateOf(liveSilverPrice?.toString() ?: "0") }
    var goldGrams by remember { mutableStateOf("") }
    var silverGrams by remember { mutableStateOf("") }"""

content = content.replace(old_zakat, new_zakat)

# Add Nisab display card
nisab_ui = """
        // Live Nisab Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (languageCode == "ur") "آج کا نصاب (لائیو قیمتوں کے مطابق)" else "Live Nisab (Based on Market Prices)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's Gold Nisab (96g):", style = MaterialTheme.typography.bodyMedium)
                    val gPrice = liveGoldPrice ?: 0.0
                    Text("$currencySymbol ${"%.2f".format(96.0 * gPrice)}", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's Silver Nisab (700g):", style = MaterialTheme.typography.bodyMedium)
                    val sPrice = liveSilverPrice ?: 0.0
                    Text("$currencySymbol ${"%.2f".format(700.0 * sPrice)}", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
"""

content = content.replace('Text(if (languageCode == "ur") "زکوٰۃ سونا اور چاندی کی مخصوص مقدار (نصاب) سے تجاوز کرنے پر واجب ہے۔" else if (languageCode == "ar") "تجب الزكاة في الذهب والفضة إذا بلغا النصاب." else "Zakat is obligatory on Gold and Silver if they exceed the Nisab threshold.", style = MaterialTheme.typography.bodyMedium)', 
                          'Text(if (languageCode == "ur") "زکوٰۃ سونا اور چاندی کی مخصوص مقدار (نصاب) سے تجاوز کرنے پر واجب ہے۔" else if (languageCode == "ar") "تجب الزكاة في الذهب والفضة إذا بلغا النصاب." else "Zakat is obligatory on Gold and Silver if they exceed the Nisab threshold.", style = MaterialTheme.typography.bodyMedium)\n\n' + nisab_ui)

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "w") as f:
    f.write(content)
