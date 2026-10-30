import re

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "r") as f:
    content = f.read()

print("items(filteredZiyarats)" in content)
