fun normalizeCategory(cat: String?): String {
    if (cat.isNullOrBlank()) return "Duas"
    val lower = cat.lowercase()
    if (lower.contains("ziyarat")) return "Ziyarats"
    if (lower.contains("munajat")) return "Munajats"
    if (lower == "general supplications" || lower == "daily supplications" || lower == "supplication" || lower == "duas" || lower == "dua") return "Duas"
    if (lower.contains("amaal")) return "Amaal"
    if (lower.contains("prayer")) return "Prayers"
    return cat // Leave others as they are
}

fun main() {
    println(normalizeCategory("General Supplications"))
    println(normalizeCategory("Ziyarat"))
    println(normalizeCategory("Amaal"))
    println(normalizeCategory("Prayers"))
    println(normalizeCategory("Ramadan Specials"))
}
