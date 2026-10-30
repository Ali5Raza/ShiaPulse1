import re

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "r") as f:
    content = f.read()

# Update ZiyaratsScreen signature
old_sig = """fun ZiyaratsScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    languageCode: String = "en",
    modifier: Modifier = Modifier,
    initialSelectedZiyaratId: String? = null,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onOpenZiyaratsMap: (() -> Unit)? = null
) {"""
new_sig = """fun ZiyaratsScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    languageCode: String = "en",
    modifier: Modifier = Modifier,
    initialSelectedZiyaratId: String? = null,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onOpenZiyaratsMap: (() -> Unit)? = null,
    supplicationZiyarats: List<com.example.ui.ShiaDua> = emptyList(),
    onOpenDua: ((com.example.ui.ShiaDua) -> Unit)? = null
) {"""
content = content.replace(old_sig, new_sig)

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "w") as f:
    f.write(content)
