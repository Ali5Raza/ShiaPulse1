import re

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "r") as f:
    content = f.read()

# Find items(filteredZiyarats, key = { it.id }) { ziyarat ->
# and append items(filteredSupplications) after the ZiyaratItem list block.

old_block = """                items(filteredZiyarats, key = { it.id }) { ziyarat ->"""
# Let's see the context to safely add items(filteredSupplications)
