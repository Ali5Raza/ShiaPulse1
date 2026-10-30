import re

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "r") as f:
    content = f.read()

target = 'Text(if (languageCode == "ur") "زکوٰۃ صرف 9 چیزوں پر واجب ہے: سونے اور چاندی کے سکے، اونٹ، گائے، بھیڑ، گندم، جو، کھجور، اور کشمش۔ عمومی بچت، کرنسی، اور تجارتی مال پر 2.5 فیصد زکوٰۃ دینا مستحب ہے۔" else if (languageCode == "ar") "تجب الزكاة في تسعة أشياء فقط: الذهب والفضة (المسكوكان)، الإبل، البقر، الغنم، الحنطة، الشعير، التمر، والزبيب. وتستحب الزكاة بنسبة 2.5٪ على المدخرات العامة والعملات." else "Zakat is obligatorily due (Wajib) only on 9 items: Gold coins, Silver coins, Camels, Cows, Sheep, Wheat, Barley, Dates, and Raisins. It is highly recommended (Mustahab) on general saves, currencies, and trade goods at a rate of 2.5%.",'

nisab_ui = """
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
"""

content = content.replace(target, target + nisab_ui)

# Make sure we didn't add it twice
with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "w") as f:
    f.write(content)

