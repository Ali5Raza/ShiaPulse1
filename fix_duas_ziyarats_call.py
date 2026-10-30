import re

with open("app/src/main/java/com/example/ui/DuasScreen.kt", "r") as f:
    content = f.read()

old_call = """        ZiyaratsScreen(
            viewModel = viewModel,
            onBack = { selectedCategoryTab = "Categories" },
            languageCode = currentLang,
            modifier = modifier,
            onEditCard = onEditCard,
            onOpenZiyaratsMap = onOpenZiyaratsMap
        )"""

new_call = """        ZiyaratsScreen(
            viewModel = viewModel,
            onBack = { selectedCategoryTab = "Categories" },
            languageCode = currentLang,
            modifier = modifier,
            onEditCard = onEditCard,
            onOpenZiyaratsMap = onOpenZiyaratsMap,
            supplicationZiyarats = loadedDuas.filter { it.category.contains("Ziyarat", ignoreCase = true) },
            onOpenDua = { dua -> 
                selectedDuaForReading = dua
            }
        )"""

content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/ui/DuasScreen.kt", "w") as f:
    f.write(content)
