const fs = require('fs');
const filepath = './app/src/main/java/com/example/ui/ZiyaratsScreen.kt';

let content = fs.readFileSync(filepath, 'utf8');

// Find the index of "musayyib_tiflan"
const targetIdIndex = content.indexOf('id = "musayyib_tiflan"');
if (targetIdIndex === -1) {
    console.error('Could not find musayyib_tiflan in the file!');
    process.exit(1);
}

// Find the first closing parenthesis "    )" after targetIdIndex
const closingParenthesisIndex = content.indexOf('    )', targetIdIndex);
if (closingParenthesisIndex === -1) {
    console.error('Could not find the closing parenthesis after musayyib_tiflan!');
    process.exit(1);
}

// The end of the ZiyaratLocation block for musayyib_tiflan
const endOfLocationIndex = closingParenthesisIndex + '    )'.length;

// Find the start of val leafletHtml = """
const leafletHtmlIndex = content.indexOf('val leafletHtml = """');
if (leafletHtmlIndex === -1) {
    console.error('Could not find val leafletHtml = """');
    process.exit(1);
}

// We want to replace everything from endOfLocationIndex to leafletHtmlIndex
// with "\n)\n\n" (closing the list and adding space)
const before = content.substring(0, endOfLocationIndex);
const after = content.substring(leafletHtmlIndex);

const newContent = before + '\n)\n\n' + after;

fs.writeFileSync(filepath, newContent, 'utf8');
console.log('Successfully modified ZiyaratsScreen.kt!');
