import re

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "r") as f:
    content = f.read()

old_ui = """                            recommendedList.forEach { recDua ->
                                Card(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .clickable {
                                            activeOverlayParam = recDua.id
                                            activeOverlay = "duas"
                                        }
                                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {"""

new_ui = """                            recommendedList.forEach { recItem ->
                                Card(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .clickable {
                                            activeOverlayParam = recItem.id
                                            activeOverlay = recItem.type
                                        }
                                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {"""

content = content.replace(old_ui, new_ui)

content = content.replace("""Text(
                                                        text = if (currentLang == "ur" && recDua.titleUr != null) recDua.titleUr else if (currentLang == "ar" && recDua.titleAr != null) recDua.titleAr else recDua.title,""", """Text(
                                                        text = if (currentLang == "ur") recItem.titleUr else if (currentLang == "ar") recItem.titleAr else recItem.title,""")

content = content.replace("recDua.category", "recItem.category")
content = content.replace("recDua.description", "recItem.descEng")

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "w") as f:
    f.write(content)
