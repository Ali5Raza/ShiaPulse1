package com.example.ui

import com.example.data.KnowledgeReference
import com.example.data.KnowledgeReferenceType
import com.example.data.quranReference

/** Curated references for the two hardcoded name catalogues. IDs are stable UI IDs. */
object KnowledgeNameReferences {
    val asmaHusnaHadith = KnowledgeReference(
        type = KnowledgeReferenceType.HADITH,
        citation = "Oddatol Daee, chapter 8 — the 99 Names tradition and explanations",
        url = "https://www.duas.org/od/chap8.htm"
    )

    private val asmaCollectionReference = KnowledgeReference(
        type = KnowledgeReferenceType.HADITH,
        citation = "Oddatol Daee, chapter 8 — discussion of this Beautiful Name",
        url = "https://www.duas.org/od/chap8.htm"
    )

    private val allahVerses: Map<Int, KnowledgeReference> = mapOf(
        1 to quranReference(1, 1),
        2 to quranReference(1, 1),
        3 to quranReference(59, 23),
        4 to quranReference(59, 23),
        5 to quranReference(59, 23),
        6 to quranReference(59, 23),
        7 to quranReference(59, 23),
        8 to quranReference(59, 23),
        9 to quranReference(59, 23),
        10 to quranReference(59, 23),
        11 to quranReference(59, 24),
        12 to quranReference(59, 24),
        13 to quranReference(59, 24),
        14 to quranReference(38, 66),
        15 to quranReference(39, 4),
        16 to quranReference(3, 8),
        17 to quranReference(51, 58),
        18 to quranReference(34, 26),
        19 to quranReference(2, 158),
        20 to quranReference(2, 245, direct = false),
        21 to quranReference(2, 245, direct = false),
        22 to quranReference(56, 3, direct = false),
        23 to quranReference(56, 3, direct = false),
        24 to quranReference(3, 26, direct = false),
        25 to quranReference(3, 26, direct = false),
        26 to quranReference(2, 127),
        27 to quranReference(4, 58),
        28 to quranReference(95, 8, direct = false),
        29 to quranReference(4, 58, direct = false),
        30 to quranReference(6, 103),
        31 to quranReference(6, 103),
        32 to quranReference(2, 235),
        33 to quranReference(2, 255),
        34 to quranReference(2, 235),
        35 to quranReference(35, 30),
        36 to quranReference(2, 255),
        37 to quranReference(22, 62),
        38 to quranReference(34, 21),
        39 to quranReference(4, 85),
        40 to quranReference(4, 6),
        41 to quranReference(55, 27, direct = false),
        42 to quranReference(82, 6),
        43 to quranReference(4, 1),
        44 to quranReference(11, 61),
        45 to quranReference(2, 268),
        46 to quranReference(2, 129),
        47 to quranReference(85, 14),
        48 to quranReference(85, 15),
        49 to quranReference(22, 7, direct = false),
        50 to quranReference(4, 33),
        51 to quranReference(22, 6),
        52 to quranReference(3, 173),
        53 to quranReference(22, 40),
        54 to quranReference(51, 58),
        55 to quranReference(42, 9),
        56 to quranReference(14, 1),
        57 to quranReference(72, 28, direct = false),
        58 to quranReference(85, 13, direct = false),
        59 to quranReference(85, 13, direct = false),
        60 to quranReference(41, 39, direct = false),
        61 to quranReference(2, 28, direct = false),
        62 to quranReference(2, 255),
        63 to quranReference(2, 255),
        64 to asmaCollectionReference,
        65 to quranReference(85, 15, direct = false),
        66 to quranReference(39, 4),
        67 to quranReference(112, 1),
        68 to quranReference(112, 2),
        69 to quranReference(6, 65),
        70 to quranReference(54, 55),
        71 to asmaCollectionReference,
        72 to quranReference(16, 61, direct = false),
        73 to quranReference(57, 3),
        74 to quranReference(57, 3),
        75 to quranReference(57, 3),
        76 to quranReference(57, 3),
        77 to quranReference(42, 9, direct = false),
        78 to quranReference(13, 9),
        79 to quranReference(52, 28),
        80 to quranReference(2, 37),
        81 to quranReference(32, 22, direct = false),
        82 to quranReference(4, 43),
        83 to quranReference(2, 143),
        84 to quranReference(3, 26),
        85 to quranReference(55, 27),
        86 to quranReference(3, 18, direct = false),
        87 to quranReference(3, 9),
        88 to quranReference(2, 263),
        89 to quranReference(9, 28, direct = false),
        90 to quranReference(2, 245, direct = false),
        91 to quranReference(6, 17, direct = false),
        92 to quranReference(10, 107, direct = false),
        93 to quranReference(24, 35),
        94 to quranReference(25, 31),
        95 to quranReference(2, 117),
        96 to quranReference(55, 27, direct = false),
        97 to quranReference(15, 23),
        98 to quranReference(2, 256, direct = false),
        99 to asmaCollectionReference
    )

    private const val BEACONS =
        "https://al-islam.org/beacons-light-abu-ali-al-fadl-ibn-al-hasan-ibn-al-fadl-tabarsi/holy-prophet-islam"
    private const val KHISAL =
        "https://al-islam.org/al-khisal-numeric-classification-traditions-characteristics-shaykh-saduq/part-10-ten-numbered"
    private const val FIRST_INFALLIBLE =
        "https://al-islam.org/brief-history-fourteen-infallibles/first-infallible-holy-prophet-islam-muhammad-ibn-abdullah-peace"
    private const val SADIQ_AMIN =
        "https://al-islam.org/prophet-muhammad-s-brief-biography/truthful-and-honest"
    private const val SHUKR =
        "https://al-islam.org/forty-hadith-exposition-second-revised-edition-sayyid-ruhullah-musawi-khomeini/twenty-first-hadith"
    private const val INTERCESSION =
        "https://al-islam.org/inquiries-about-shia-islam-sayyid-moustafa-al-qazwini/intercession-shafaah"

    val prophetNamesOverview = KnowledgeReference(
        type = KnowledgeReferenceType.HADITH,
        citation = "Beacons of Light — His Names, Epithets and Honorable Lineage",
        url = BEACONS
    )

    private fun transmitted(citation: String, url: String = BEACONS) = KnowledgeReference(
        type = KnowledgeReferenceType.HADITH,
        citation = citation,
        url = url
    )

    private fun historical(citation: String, url: String) = KnowledgeReference(
        type = KnowledgeReferenceType.HISTORY,
        citation = citation,
        url = url
    )

    private val prophetReferences: Map<Int, List<KnowledgeReference>> = mapOf(
        1 to listOf(quranReference(48, 29)),
        2 to listOf(quranReference(61, 6)),
        4 to listOf(
            quranReference(17, 79, direct = false).copy(
                citation = "Qur’an 17:79 — Maqam Mahmud (the Praised Station)"
            )
        ),
        5 to listOf(
            historical(
                "Brief History of Fourteen Infallibles — the Prophet’s kunya, Abu’l-Qasim",
                FIRST_INFALLIBLE
            )
        ),
        6 to listOf(transmitted("Beacons of Light — al-‘Aqib in the Prophet’s reported names")),
        7 to listOf(transmitted("Al-Khisal, ten-named characteristics — al-Fatih", KHISAL)),
        8 to listOf(quranReference(33, 40)),
        9 to listOf(transmitted("Beacons of Light — al-Hashir in the reported five names")),
        10 to listOf(transmitted("Beacons of Light — al-Mahi in the reported five names")),
        11 to listOf(quranReference(33, 46)),
        12 to listOf(quranReference(33, 46)),
        13 to listOf(quranReference(33, 45)),
        14 to listOf(quranReference(33, 45)),
        15 to listOf(quranReference(26, 194, direct = false)),
        16 to listOf(quranReference(7, 157)),
        17 to listOf(historical("Prophet Muhammad: A Brief Biography — as-Sadiq", SADIQ_AMIN)),
        18 to listOf(historical("Prophet Muhammad: A Brief Biography — al-Amin", SADIQ_AMIN)),
        19 to listOf(transmitted("Al-Kafi narration in Forty Hadith — ‘a grateful servant’", SHUKR)),
        20 to listOf(
            quranReference(33, 33, direct = false).copy(
                noteEn = "The purification verse is the Qur’anic context for this honorific; the exact title Mutahhar is not quoted here.",
                noteUr = "آیتِ تطہیر اس لقب کا قرآنی پس منظر ہے؛ اس میں لفظِ مطہر بطور لقب موجود نہیں۔",
                noteAr = "آية التطهير هي السياق القرآني لهذا اللقب، ولا يرد فيها لفظ مطهر كعنوان."
            )
        ),
        23 to listOf(historical("Brief History of Fourteen Infallibles — title al-Mustafa", FIRST_INFALLIBLE)),
        25 to listOf(quranReference(9, 128)),
        26 to listOf(quranReference(9, 128)),
        27 to listOf(quranReference(21, 107)),
        28 to listOf(
            quranReference(20, 1, direct = false),
            transmitted("Beacons of Light — Taha counted among the Prophet’s titles in tradition")
        ),
        29 to listOf(
            quranReference(36, 1, direct = false),
            transmitted("Beacons of Light — Yasin counted among the Prophet’s titles in tradition")
        ),
        30 to listOf(quranReference(73, 1)),
        31 to listOf(quranReference(74, 1)),
        32 to listOf(transmitted("Prophetic hadith on God-permitted intercession", INTERCESSION)),
        33 to listOf(transmitted("Prophetic hadith on God-permitted intercession", INTERCESSION)),
        34 to listOf(transmitted("Prophetic hadith on accepted, God-permitted intercession", INTERCESSION))
    )

    fun forAllahName(id: Int): List<KnowledgeReference> =
        allahVerses[id]?.let { reference -> listOf(reference) }.orEmpty()

    fun forProphetName(id: Int): List<KnowledgeReference> =
        prophetReferences[id].orEmpty()
}
