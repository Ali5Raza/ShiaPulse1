const fs = require('fs');

const path = 'app/src/main/java/com/example/ui/DashboardScreen.kt';
if (!fs.existsSync(path)) {
   console.log("File not found at: " + path);
   process.exit(1);
}
let contents = fs.readFileSync(path, 'utf8');

const replacements = [
  { pattern: /Color\(0xFF1E293B\)/g, replace: 'MaterialTheme.colorScheme.onSurface' },
  { pattern: /Color\(0xFF64748B\)/g, replace: 'MaterialTheme.colorScheme.onSurfaceVariant' },
  { pattern: /Color\(0xFF475569\)/g, replace: 'MaterialTheme.colorScheme.onSurfaceVariant' },
  { pattern: /Color\(0xFF334155\)/g, replace: 'MaterialTheme.colorScheme.onSurface' },
  { pattern: /Color\(0xFFF8FAFC\)/g, replace: 'MaterialTheme.colorScheme.surfaceVariant' },
  { pattern: /Color\(0xFFEEF2F6\)/g, replace: 'MaterialTheme.colorScheme.surfaceVariant' },
  { pattern: /Color\(0xFFF1F5F9\)/g, replace: 'MaterialTheme.colorScheme.surfaceVariant' },
  { pattern: /Color\(0xFFE2E8F0\)/g, replace: 'MaterialTheme.colorScheme.outlineVariant' },
  { pattern: /Color\(0xFF4F46E5\)/g, replace: 'MaterialTheme.colorScheme.primary' },
  { pattern: /Color.White/g, replace: 'MaterialTheme.colorScheme.onPrimary' },
  { pattern: /Color\(0xFFFFEBEE\)/g, replace: 'MaterialTheme.colorScheme.errorContainer' },
  { pattern: /Color\(0xFFFFCDD2\)/g, replace: 'MaterialTheme.colorScheme.error' },
  { pattern: /Color\(0xFFC62828\)/g, replace: 'MaterialTheme.colorScheme.onErrorContainer' },
  { pattern: /Color\(0xFF5D4037\)/g, replace: 'MaterialTheme.colorScheme.onErrorContainer' }
];

let lines = contents.split('\n');

let inPopup = false;

for (let i = 0; i < lines.length; i++) {
  if (lines[i].includes('if (showProfileSettingsDialog) {')) {
    inPopup = true;
  }
  
  if (inPopup && lines[i].includes('Color(') || inPopup && lines[i].includes('Color.')) {
     for(let r of replacements) {
       lines[i] = lines[i].replace(r.pattern, r.replace);
     }
  }
}

fs.writeFileSync(path, lines.join('\n'));
console.log('Colors replaced successfully!');
