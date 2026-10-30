import re

with open("app/src/main/java/com/example/ui/DuasScreen.kt", "r") as f:
    content = f.read()

old_folders = """                            // 1. Sacred Ziyarats Folder Card
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "Sacred Ziyarats",
                                    titleUr = "زیاراتِ مقدسہ معصومینؑ",
                                    icon = Icons.Default.Mosque,
                                    color = Color(0xFF8B6B22),
                                    count = if (allZiyarats.isNotEmpty()) allZiyarats.size else 14,
                                    onClick = { selectedCategoryTab = "Sacred Ziyarats" }
                                )
                            }
                            
                            // 2. Ziyarats Collection Folder Card (Supplications Ziyarats)
                            val supplicationZiyaratCount = loadedDuas.count { it.category.contains("Ziyarat", ignoreCase = true) }
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "Ziyarats Collection",
                                    titleUr = "زیاراتِ مبارکہ (عاشورہ و دیگر)",
                                    icon = Icons.Default.Book,
                                    color = Color(0xFF2E7D32),
                                    count = if (supplicationZiyaratCount > 0) supplicationZiyaratCount else 8,
                                    onClick = { selectedCategoryTab = "Ziyarat" }
                                )
                            }"""

new_folders = """                            // Merged Ziyarats Folder Card
                            val supplicationZiyaratCount = loadedDuas.count { it.category.contains("Ziyarat", ignoreCase = true) }
                            val totalZiyaratsCount = (if (allZiyarats.isNotEmpty()) allZiyarats.size else 14) + supplicationZiyaratCount
                            
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "Ziyarats",
                                    titleUr = "زیارات (مقدسہ و مجموعہ)",
                                    icon = Icons.Default.Mosque,
                                    color = Color(0xFF8B6B22),
                                    count = totalZiyaratsCount,
                                    onClick = { selectedCategoryTab = "Sacred Ziyarats" }
                                )
                            }"""

content = content.replace(old_folders, new_folders)

with open("app/src/main/java/com/example/ui/DuasScreen.kt", "w") as f:
    f.write(content)
