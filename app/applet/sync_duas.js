const fs = require('fs');
const json = JSON.parse(fs.readFileSync('app/src/main/assets/duas.json', 'utf8'));

let out = `val shiaDuasList: List<ShiaDua> = listOf(
`;

json.forEach((dua, idx) => {
    out += `    ShiaDua(
        id = "${dua.id}",
        title = "${dua.title}",
        category = "${dua.category}",
        description = "${dua.description.replace(/"/g, '\\"')}",
        relevance = "${dua.relevance.replace(/"/g, '\\"')}",
        audioUrl = "${dua.audioUrl}",
        lines = listOf(
`;
    dua.lines.forEach((line, lidx) => {
        out += `            DuaLine(
                arabic = """${line.arabic}""",
                translation = """${line.translation}""",
                transliteration = """${line.transliteration}""",
                urduTranslation = ${line.urduTranslation ? `"""${line.urduTranslation}"""` : 'null'},
                farsiTranslation = ${line.farsiTranslation ? `"""${line.farsiTranslation}"""` : 'null'}
            )${lidx < dua.lines.length - 1 ? ',' : ''}
`;
    });
    out += `        )
    )${idx < json.length - 1 ? ',' : ''}
`;
});
out += `)\n`;

fs.writeFileSync('generated_duasList.kt', out);
console.log("Done");
