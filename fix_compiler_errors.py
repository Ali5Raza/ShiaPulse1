import re

with open("app/src/main/java/com/example/ui/DuasScreen.kt", "r") as f:
    content = f.read()

content = content.replace("selectedDuaForReading = dua", "activeReadingDua = dua")

with open("app/src/main/java/com/example/ui/DuasScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("(dua.titleUr ?: dua.title)", "dua.title")
content = content.replace("Icons.AutoMirrored.Filled.ArrowForward", "Icons.Filled.ArrowForward")
content = content.replace("Icons.AutoMirrored.Filled", "Icons.Filled") # Just in case

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "w") as f:
    f.write(content)
