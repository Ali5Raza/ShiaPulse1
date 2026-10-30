import re

with open('app/src/main/java/com/example/ui/AamalScreen.kt', 'r') as f:
    content = f.read()

imports = [
    'import androidx.compose.foundation.background',
    'import androidx.compose.foundation.shape.CircleShape',
    'import androidx.compose.ui.text.style.TextAlign',
    'import androidx.compose.ui.text.font.FontStyle'
]

for imp in imports:
    if imp not in content:
        content = content.replace('import androidx.compose.ui.unit.dp', f'import androidx.compose.ui.unit.dp\n{imp}')

with open('app/src/main/java/com/example/ui/AamalScreen.kt', 'w') as f:
    f.write(content)
